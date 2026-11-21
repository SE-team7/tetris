package com.team.tetris.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleEndReason;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings.KeyAction;

/**
 * 대전 진행 관리: 엔진 실행 스레드, 입력 큐, 공격 연결, 제한시간, 승패 판정.
 *
 * 스레드 구조
 *  - 플레이어 스레드(플레이어마다 1개): 입력 처리 → 자동 낙하(tick) → 스냅샷 갱신.
 *    엔진(GameEngineService)은 자기 플레이어 스레드에서만 변경한다.
 *  - 판정 스레드(1개): 게임오버·제한시간을 확인하고, 양쪽을 멈춘 뒤 같은 시점에서 승패를 정한다.
 *  - 화면(3번)은 getPlayerSnapshot()으로 마지막 스냅샷만 읽는다. 엔진을 직접 건드리지 않는다.
 *
 * 리스너는 화면 스레드가 아닌 곳에서 호출되므로, 화면을 바꿀 때는 Platform.runLater를 사용해야 한다.
 */
public class BattleService {

    // 플레이어 스레드 실행 간격
    private static final long STEP_MILLIS = 5;
    // 판정 스레드 실행 간격
    private static final long JUDGE_MILLIS = 10;
    // 좌우 키를 꾹 눌렀을 때 반복 이동이 시작되기까지의 시간
    static final long MOVE_REPEAT_DELAY_MILLIS = 170;
    // 좌우 반복 이동 간격
    static final long MOVE_REPEAT_INTERVAL_MILLIS = 50;
    // 소프트 드랍 반복 간격
    static final long SOFT_DROP_INTERVAL_MILLIS = 50;
    // P2P: 처음 종료 조건을 본 뒤 상대 게임오버 소식을 기다리는 시간 (동시 게임오버 판정용)
    static final long NETWORK_JUDGE_GRACE_MILLIS = 300;
    // 스레드 정지 대기 시간
    private static final long STOP_WAIT_MILLIS = 500;

    // P2P 판정 담당 (서버 = PLAYER1)
    static final PlayerId NETWORK_JUDGE = PlayerId.PLAYER1;

    // 엔진 생성 방식 (테스트에서 블럭 순서를 고정할 때 교체)
    interface EngineFactory {
        GameEngineService create(Board board, AttackService attackService);
    }

    private final EngineFactory engineFactory;
    private final Object lock = new Object();

    private volatile BattleState state = BattleState.WAITING;
    private volatile BattleResult result;
    private volatile BattleConfig config;
    private volatile boolean networkMode;
    private volatile PlayerId localPlayer;

    // 시작할 때마다 새 맵으로 교체 (읽는 쪽은 잠금 없이 참조만 읽음)
    private volatile Map<PlayerId, PlayerRunner> runners = Map.of();
    private ScheduledExecutorService judgeExecutor;

    // P2P 상대 정보
    private volatile AttackQueue localIncomingQueue;
    private volatile GameSnapshot remoteSnapshot;
    private volatile boolean remoteGameOver;

    // 제한시간 (일시정지 시간 제외)
    private long startNanos;
    private long pausedAtNanos;
    private long pausedTotalNanos;
    private long finishedAtNanos;

    // P2P 판정 진행 상태
    private boolean endTriggered;
    private long judgeDeadlineNanos;

    // 리스너 (기본값: 아무 일도 하지 않음)
    private volatile Consumer<AttackRows> outgoingAttackListener = attack -> { };
    private volatile Runnable localGameOverListener = () -> { };
    private volatile Consumer<GameSnapshot> localSnapshotListener = snapshot -> { };
    private volatile Consumer<BattleResult> resultListener = battleResult -> { };

    public BattleService() {
        this((board, attackService) -> new GameEngineService(
                board, new BlockFactory(), new ScoreService(), new LevelService(), attackService));
    }

    BattleService(EngineFactory engineFactory) {
        this.engineFactory = engineFactory;
    }

    // ===================== 시작 =====================

    // 로컬 대전: 엔진 2개·입력 큐 2개·엔진 실행 스레드 2개 생성, 서로 공격 연결
    public void startLocalBattle(BattleConfig config) {
        synchronized (lock) {
            stopInternal();
            reset(config, false, null);

            AttackQueue player1Queue = new AttackQueue();
            AttackQueue player2Queue = new AttackQueue();

            Map<PlayerId, PlayerRunner> created = new EnumMap<>(PlayerId.class);
            created.put(PlayerId.PLAYER1, createRunner(PlayerId.PLAYER1, player1Queue,
                    attack -> deliverLocalAttack(PlayerId.PLAYER2, player2Queue, attack)));
            created.put(PlayerId.PLAYER2, createRunner(PlayerId.PLAYER2, player2Queue,
                    attack -> deliverLocalAttack(PlayerId.PLAYER1, player1Queue, attack)));
            runners = created;

            begin();
        }
    }

    // P2P 대전: 내 엔진 1개만 실행. 양쪽 준비 완료 후(4번) 호출한다
    public void startNetworkBattle(BattleConfig config, PlayerId localPlayer) {
        if (localPlayer == null) {
            throw new IllegalArgumentException("localPlayer must not be null");
        }
        synchronized (lock) {
            stopInternal();
            reset(config, true, localPlayer);

            AttackQueue incoming = new AttackQueue();
            localIncomingQueue = incoming;

            Map<PlayerId, PlayerRunner> created = new EnumMap<>(PlayerId.class);
            created.put(localPlayer, createRunner(localPlayer, incoming,
                    attack -> outgoingAttackListener.accept(attack)));
            runners = created;

            begin();
        }
    }

    private void reset(BattleConfig config, boolean networkMode, PlayerId localPlayer) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.networkMode = networkMode;
        this.localPlayer = localPlayer;
        this.result = null;
        this.remoteSnapshot = null;
        this.remoteGameOver = false;
        this.localIncomingQueue = null;
        this.endTriggered = false;
        this.pausedTotalNanos = 0;
        this.state = BattleState.WAITING;
    }

    private PlayerRunner createRunner(PlayerId player, AttackQueue incoming, Consumer<AttackRows> sender) {
        Board board = new Board();
        AttackService attackService = new AttackService(board, incoming, sender);
        GameEngineService engine = engineFactory.create(board, attackService);
        engine.startGame(config.getDifficulty(), config.isItemMode());
        return new PlayerRunner(player, engine);
    }

    // 공통 시작 기준으로 양쪽 진행·제한시간 시작
    private void begin() {
        startNanos = System.nanoTime();
        state = BattleState.RUNNING;
        for (PlayerRunner runner : runners.values()) {
            runner.start();
        }
        judgeExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreads("battle-judge"));
        judgeExecutor.scheduleAtFixedRate(this::judgeStep, JUDGE_MILLIS, JUDGE_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void deliverLocalAttack(PlayerId target, AttackQueue targetQueue, AttackRows attack) {
        if (attack == null || attack.isEmpty()) {
            return;
        }
        targetQueue.add(attack);
        PlayerRunner runner = runners.get(target);
        if (runner != null) {
            runner.markDirty(); // 대기 공격 표시 갱신
        }
    }

    // ===================== 입력 =====================

    // 회전·하드드랍·스킵 같은 단발 조작
    public void submitAction(PlayerId player, KeyAction action) {
        PlayerRunner runner = runners.get(player);
        if (runner != null && action != null) {
            runner.inputs.add(InputEvent.action(action));
        }
    }

    // 좌우 이동·소프트 드랍의 누름/해제 상태. 반복 이동은 엔진 스레드에서 처리
    public void submitKeyState(PlayerId player, KeyAction action, boolean pressed) {
        PlayerRunner runner = runners.get(player);
        if (runner != null && action != null) {
            runner.inputs.add(InputEvent.keyState(action, pressed));
        }
    }

    // 양쪽 누른 키 상태와 대기 입력 초기화 (포커스를 잃었을 때 등)
    public void clearInputStates() {
        for (PlayerRunner runner : runners.values()) {
            runner.requestClearInputs();
        }
    }

    // ===================== 일시정지·재개·정지 =====================

    // 양쪽 진행·제한시간 정지, 입력 상태 초기화
    public void pause() {
        synchronized (lock) {
            if (state != BattleState.RUNNING) {
                return;
            }
            pausedAtNanos = System.nanoTime();
            state = BattleState.PAUSED;
            clearInputStates();
        }
    }

    // 양쪽 진행·제한시간 재개, 낙하 시간 기준 재설정
    public void resume() {
        synchronized (lock) {
            if (state != BattleState.PAUSED) {
                return;
            }
            pausedTotalNanos += System.nanoTime() - pausedAtNanos;
            for (PlayerRunner runner : runners.values()) {
                runner.requestDropTimerReset();
            }
            state = BattleState.RUNNING;
        }
    }

    // 엔진 스레드·판정 작업 중단 및 자원 정리 (메뉴로 나갈 때 등)
    public void stop() {
        synchronized (lock) {
            stopInternal();
            if (state != BattleState.FINISHED) {
                state = BattleState.WAITING; // 결과 없이 중단
            }
        }
    }

    private void stopInternal() {
        for (PlayerRunner runner : runners.values()) {
            runner.stop();
        }
        if (judgeExecutor != null) {
            judgeExecutor.shutdownNow();
            judgeExecutor = null;
        }
    }

    // ===================== P2P 수신 (4번이 호출) =====================

    // 상대가 보낸 공격을 내 대기열에 추가
    public void receiveRemoteAttack(AttackRows attackRows) {
        AttackQueue queue = localIncomingQueue;
        if (!networkMode || queue == null || attackRows == null || attackRows.isEmpty()) {
            return;
        }
        queue.add(attackRows);
        PlayerRunner runner = runners.get(localPlayer);
        if (runner != null) {
            runner.markDirty();
        }
    }

    // 상대 화면 표시용 상태 갱신
    public void updateRemoteSnapshot(GameSnapshot snapshot) {
        if (networkMode) {
            remoteSnapshot = snapshot;
        }
    }

    // 상대 게임오버 반영. 판정은 판정 스레드에서 양쪽 상태를 확인한 뒤 한다
    public void notifyRemoteGameOver() {
        if (networkMode) {
            remoteGameOver = true;
        }
    }

    // 판정 담당이 확정한 최종 결과 적용. 이미 끝났으면 무시
    public void applyRemoteResult(BattleResult remoteResult) {
        if (remoteResult == null) {
            return;
        }
        synchronized (lock) {
            if (isActive()) {
                finish(remoteResult);
            }
        }
    }

    // 상대 나가기: 로컬 플레이어 승리
    public void notifyRemoteLeft() {
        synchronized (lock) {
            if (networkMode && isActive()) {
                finish(BattleResult.win(localPlayer, BattleEndReason.PLAYER_LEFT));
            }
        }
    }

    // 연결 끊김: 승자 없이 종료
    public void notifyConnectionLost() {
        synchronized (lock) {
            if (networkMode && isActive()) {
                finish(BattleResult.connectionLost());
            }
        }
    }

    // ===================== 리스너 등록 =====================

    // P2P: 내가 만든 공격 전송 (4번 sendAttack 연결)
    public void setOutgoingAttackListener(Consumer<AttackRows> listener) {
        outgoingAttackListener = listener == null ? attack -> { } : listener;
    }

    // P2P: 내 게임오버 통지 (4번 sendGameOver 연결)
    public void setLocalGameOverListener(Runnable listener) {
        localGameOverListener = listener == null ? () -> { } : listener;
    }

    // P2P: 내 상태가 바뀔 때마다 호출 (4번 sendSnapshot 연결)
    public void setLocalSnapshotListener(Consumer<GameSnapshot> listener) {
        localSnapshotListener = listener == null ? snapshot -> { } : listener;
    }

    // 결과가 확정되면 한 번 호출 (3번 결과 화면, P2P 판정 담당은 4번 sendBattleResult 연결)
    public void setResultListener(Consumer<BattleResult> listener) {
        resultListener = listener == null ? battleResult -> { } : listener;
    }

    // ===================== 조회 =====================

    // 해당 플레이어의 최신 상태. 아직 없으면 null (P2P 상대는 마지막으로 받은 상태)
    public GameSnapshot getPlayerSnapshot(PlayerId player) {
        PlayerRunner runner = runners.get(player);
        if (runner != null) {
            return runner.snapshot;
        }
        if (networkMode && localPlayer != null && player == localPlayer.opponent()) {
            return remoteSnapshot;
        }
        return null;
    }

    // 남은 제한시간(ms). 시간제한 모드가 아니면 -1
    public long getRemainingMillis() {
        synchronized (lock) {
            return remainingMillis(System.nanoTime());
        }
    }

    public BattleState getState() {
        return state;
    }

    // 종료 전 null, 종료 후 결과
    public BattleResult getResult() {
        return result;
    }

    public BattleConfig getConfig() {
        return config;
    }

    public boolean isNetworkBattle() {
        return networkMode;
    }

    // P2P 내 플레이어, 로컬 대전이면 null
    public PlayerId getLocalPlayer() {
        return localPlayer;
    }

    // ===================== 판정 =====================

    private boolean isActive() {
        return state == BattleState.RUNNING || state == BattleState.PAUSED;
    }

    private long remainingMillis(long nowNanos) {
        BattleConfig current = config;
        if (current == null || !current.isTimeLimited()) {
            return -1;
        }
        long limitMillis = current.getTimeLimitSeconds() * 1000L;
        if (state == BattleState.WAITING) {
            return limitMillis;
        }
        long end = switch (state) {
            case PAUSED -> pausedAtNanos;
            case FINISHED -> finishedAtNanos;
            default -> nowNanos;
        };
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(end - startNanos - pausedTotalNanos);
        return Math.max(0, limitMillis - elapsedMillis);
    }

    private boolean isTimeUp(long nowNanos) {
        return config.isTimeLimited() && remainingMillis(nowNanos) <= 0;
    }

    private void judgeStep() {
        try {
            synchronized (lock) {
                if (state != BattleState.RUNNING) {
                    return;
                }
                long now = System.nanoTime();
                if (networkMode) {
                    judgeNetwork(now);
                } else {
                    judgeLocal(now);
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    // 로컬: 종료 조건이 보이면 양쪽을 먼저 멈추고, 같은 시점의 엔진 상태로 판정
    private void judgeLocal(long now) {
        PlayerRunner player1 = runners.get(PlayerId.PLAYER1);
        PlayerRunner player2 = runners.get(PlayerId.PLAYER2);
        boolean timeUp = isTimeUp(now);
        if (!player1.gameOver && !player2.gameOver && !timeUp) {
            return;
        }

        freezeAll();
        boolean player1Over = player1.engine.isGameOver();
        boolean player2Over = player2.engine.isGameOver();

        if (player1Over || player2Over) {
            finish(decideByGameOver(PlayerId.PLAYER1, player1Over, player2Over));
        } else {
            finish(decideByScore(PlayerId.PLAYER1,
                    player1.engine.getScore(), player2.engine.getScore()));
        }
    }

    // P2P: 종료 조건이 보이면 내 엔진을 멈춘다.
    // 판정 담당은 상대 소식을 잠시 기다린 뒤 판정하고, 다른 쪽은 결과(applyRemoteResult)를 기다린다
    private void judgeNetwork(long now) {
        PlayerRunner local = runners.get(localPlayer);
        boolean timeUp = isTimeUp(now);
        if (!local.gameOver && !remoteGameOver && !timeUp) {
            return;
        }

        if (!endTriggered) {
            endTriggered = true;
            freezeAll();
            judgeDeadlineNanos = now + TimeUnit.MILLISECONDS.toNanos(NETWORK_JUDGE_GRACE_MILLIS);
        }
        if (localPlayer != NETWORK_JUDGE || now < judgeDeadlineNanos) {
            return;
        }

        boolean localOver = local.engine.isGameOver();
        boolean opponentOver = remoteGameOver;
        if (localOver || opponentOver) {
            finish(decideByGameOver(localPlayer, localOver, opponentOver));
        } else {
            GameSnapshot opponent = remoteSnapshot;
            int opponentScore = opponent == null ? 0 : opponent.getScore();
            finish(decideByScore(localPlayer, local.engine.getScore(), opponentScore));
        }
    }

    // 게임오버 판정: 둘 다면 무승부, 아니면 안 끝난 쪽 승리
    static BattleResult decideByGameOver(PlayerId self, boolean selfOver, boolean opponentOver) {
        if (selfOver && opponentOver) {
            return BattleResult.draw(BattleEndReason.GAME_OVER);
        }
        PlayerId winner = selfOver ? self.opponent() : self;
        return BattleResult.win(winner, BattleEndReason.GAME_OVER);
    }

    // 시간 종료 판정: 점수 높은 쪽 승리, 같으면 무승부
    static BattleResult decideByScore(PlayerId self, int selfScore, int opponentScore) {
        if (selfScore == opponentScore) {
            return BattleResult.draw(BattleEndReason.TIME_UP);
        }
        PlayerId winner = selfScore > opponentScore ? self : self.opponent();
        return BattleResult.win(winner, BattleEndReason.TIME_UP);
    }

    // 엔진 스레드를 멈추고 마지막 상태를 스냅샷에 반영
    private void freezeAll() {
        for (PlayerRunner runner : runners.values()) {
            runner.stop();
            runner.publishFinal();
        }
    }

    // 결과 확정 (lock 안에서 호출). 이미 끝났으면 무시
    private void finish(BattleResult battleResult) {
        if (state == BattleState.FINISHED) {
            return;
        }
        finishedAtNanos = state == BattleState.PAUSED ? pausedAtNanos : System.nanoTime();
        freezeAll();
        if (judgeExecutor != null) {
            judgeExecutor.shutdown();
            judgeExecutor = null;
        }
        result = battleResult;
        state = BattleState.FINISHED;
        try {
            resultListener.accept(battleResult);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static ThreadFactory daemonThreads(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    // ===================== 입력 이벤트 =====================

    private static final class InputEvent {
        final KeyAction action;
        final boolean keyState; // true: 누름/해제 상태, false: 단발 조작
        final boolean pressed;

        private InputEvent(KeyAction action, boolean keyState, boolean pressed) {
            this.action = action;
            this.keyState = keyState;
            this.pressed = pressed;
        }

        static InputEvent action(KeyAction action) {
            return new InputEvent(action, false, true);
        }

        static InputEvent keyState(KeyAction action, boolean pressed) {
            return new InputEvent(action, true, pressed);
        }
    }

    // ===================== 플레이어 스레드 =====================

    // 플레이어 1명의 엔진을 자기 스레드에서 실행
    private final class PlayerRunner {
        final PlayerId player;
        final GameEngineService engine;
        final ConcurrentLinkedQueue<InputEvent> inputs = new ConcurrentLinkedQueue<>();

        // 엔진 스레드에서만 사용: 눌린 키 → 다음 반복 시각(ns)
        private final Map<KeyAction, Long> heldKeys = new EnumMap<>(KeyAction.class);
        private final AtomicBoolean dirty = new AtomicBoolean(true);
        private final ScheduledExecutorService executor;
        private volatile Thread thread;

        private volatile boolean active;
        private volatile boolean clearInputsRequested;
        private volatile boolean dropTimerResetRequested;
        private long lastDropNanos;
        private boolean gameOverNotified;

        volatile GameSnapshot snapshot;
        volatile boolean gameOver;

        PlayerRunner(PlayerId player, GameEngineService engine) {
            this.player = player;
            this.engine = engine;
            this.snapshot = engine.getSnapshot();
            this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread created = daemonThreads("battle-" + player).newThread(runnable);
                thread = created;
                return created;
            });
        }

        void start() {
            active = true;
            // 시작하자마자 첫 블럭이 나오도록 첫 낙하 시점을 지난 상태로 둔다
            lastDropNanos = System.nanoTime()
                    - TimeUnit.MILLISECONDS.toNanos(engine.getDropIntervalMillis());
            executor.scheduleAtFixedRate(this::stepSafely, 0, STEP_MILLIS, TimeUnit.MILLISECONDS);
        }

        void stop() {
            if (!active && executor.isShutdown()) {
                return;
            }
            active = false;
            executor.shutdownNow();
            if (Thread.currentThread() != thread) {
                try {
                    executor.awaitTermination(STOP_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        void markDirty() {
            dirty.set(true);
        }

        void requestClearInputs() {
            clearInputsRequested = true;
        }

        void requestDropTimerReset() {
            dropTimerResetRequested = true;
        }

        // 스레드가 멈춘 뒤 마지막 상태 반영
        void publishFinal() {
            gameOver = engine.isGameOver();
            snapshot = engine.getSnapshot();
            if (networkMode) {
                notifySnapshot(snapshot);
            }
        }

        private void stepSafely() {
            try {
                step();
            } catch (RuntimeException e) {
                // 예외가 나도 스레드 반복이 끊기지 않도록 기록만 한다
                e.printStackTrace();
            }
        }

        private void step() {
            if (!active) {
                return;
            }
            long now = System.nanoTime();

            if (clearInputsRequested) {
                clearInputsRequested = false;
                inputs.clear();
                heldKeys.clear();
            }
            if (dropTimerResetRequested) {
                dropTimerResetRequested = false;
                lastDropNanos = now;
            }
            if (state != BattleState.RUNNING || engine.isGameOver()) {
                publishIfDirty();
                return;
            }

            processInputs(now);
            processHeldKeys(now);

            long dropIntervalNanos = TimeUnit.MILLISECONDS.toNanos(engine.getDropIntervalMillis());
            if (now - lastDropNanos >= dropIntervalNanos) {
                lastDropNanos = now;
                engine.tick();
                dirty.set(true);
            }

            settleLineClears();
            checkGameOver();
            publishIfDirty();
        }

        private void processInputs(long now) {
            InputEvent event;
            while ((event = inputs.poll()) != null) {
                dirty.set(true);
                if (!event.keyState) {
                    applyAction(event.action);
                } else if (!isRepeatable(event.action)) {
                    if (event.pressed) {
                        applyAction(event.action); // 반복 없는 키는 누를 때 한 번만
                    }
                } else if (event.pressed) {
                    if (!heldKeys.containsKey(event.action)) {
                        applyAction(event.action); // 누르는 즉시 한 번 이동
                        heldKeys.put(event.action, now + firstRepeatDelayNanos(event.action));
                    }
                } else {
                    heldKeys.remove(event.action);
                }
            }
        }

        private void processHeldKeys(long now) {
            for (Map.Entry<KeyAction, Long> entry : heldKeys.entrySet()) {
                if (now >= entry.getValue()) {
                    applyAction(entry.getKey());
                    entry.setValue(now + repeatIntervalNanos(entry.getKey()));
                    dirty.set(true);
                }
            }
        }

        private boolean isRepeatable(KeyAction action) {
            return action == KeyAction.MOVE_LEFT
                    || action == KeyAction.MOVE_RIGHT
                    || action == KeyAction.SOFT_DROP;
        }

        private long firstRepeatDelayNanos(KeyAction action) {
            long millis = action == KeyAction.SOFT_DROP
                    ? SOFT_DROP_INTERVAL_MILLIS
                    : MOVE_REPEAT_DELAY_MILLIS;
            return TimeUnit.MILLISECONDS.toNanos(millis);
        }

        private long repeatIntervalNanos(KeyAction action) {
            long millis = action == KeyAction.SOFT_DROP
                    ? SOFT_DROP_INTERVAL_MILLIS
                    : MOVE_REPEAT_INTERVAL_MILLIS;
            return TimeUnit.MILLISECONDS.toNanos(millis);
        }

        private void applyAction(KeyAction action) {
            switch (action) {
                case MOVE_LEFT -> engine.moveLeft();
                case MOVE_RIGHT -> engine.moveRight();
                case SOFT_DROP -> engine.moveDown();
                case ROTATE -> engine.rotate();
                case HARD_DROP -> engine.hardDrop();
                case SKIP -> engine.skipCurrentBlock();
                default -> { } // 일시정지·나가기는 화면(3번)에서 pause()/stop()으로 처리
            }
        }

        // 대전에는 삭제 애니메이션을 기다리지 않음: 미뤄둔 줄 삭제를 바로 반영
        private void settleLineClears() {
            while (engine.pollClearEvent() != null) {
                dirty.set(true);
            }
            if (engine.applyPendingLineClear()) {
                dirty.set(true);
            }
        }

        private void checkGameOver() {
            if (!engine.isGameOver() || gameOverNotified) {
                return;
            }
            gameOverNotified = true;
            gameOver = true;
            dirty.set(true);
            if (networkMode) {
                try {
                    localGameOverListener.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        private void publishIfDirty() {
            if (!dirty.getAndSet(false)) {
                return;
            }
            snapshot = engine.getSnapshot();
            if (networkMode) {
                notifySnapshot(snapshot);
            }
        }

        private void notifySnapshot(GameSnapshot current) {
            try {
                localSnapshotListener.accept(current);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
}
