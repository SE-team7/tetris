package com.team.tetris.controller;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.network.BattleGateway;
import com.team.tetris.network.BattleServiceGateway;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.network.MessageType;
import com.team.tetris.network.NetworkEvents;
import com.team.tetris.network.NetworkListener;
import com.team.tetris.network.NetworkMessage;
import com.team.tetris.network.NetworkService;
import com.team.tetris.network.PayloadMapper;
import com.team.tetris.network.RecentHostStore;
import com.team.tetris.service.BattleService;

import javafx.application.Platform;

/**
 * P2P 대전 진행 담당 (4번).
 * 연결 → 설정(서버가 모드 선택) → 양쪽 준비 → 공통 시작 → 대전 중 송수신 → 결과 → 다시 대기.
 *
 * 규칙
 *  - 서버 = PLAYER1 = 판정 담당. 서버가 확정한 결과(RESULT)를 클라이언트가 그대로 적용해 양쪽 승패가 같다.
 *  - battleId: 서버가 설정 변경·대전 종료 때마다 올린다. 번호가 다른 메시지(이전 대전의 늦은 메시지)는 버린다.
 *  - 상대 나가기(LEAVE)와 예상하지 못한 연결 끊김을 구분해 각각 한 번만 처리한다.
 *  - 엔진 상태는 BattleGateway(2번 API)로만 바꾸고, 화면 통지는 JavaFX Application Thread로 넘긴다.
 */
public class NetworkController {

    /** 연결 단계 */
    public enum State {
        /** 연결 없음 (P2P 첫 화면) */
        DISCONNECTED,
        /** 서버: 접속 대기 중 */
        HOSTING,
        /** 클라이언트: 접속 시도 중 */
        CONNECTING,
        /** 연결 완료, 설정·준비 대기 (대전 종료 후에도 이 상태로 돌아옴) */
        CONNECTED,
        /** 양쪽 준비 완료, 시작 카운트다운 중 */
        STARTING,
        /** 대전 중 */
        IN_BATTLE
    }

    /**
     * 진행 시간 기준.
     * @param startDelayMillis       양쪽 준비 후 시작까지 시간 (클라이언트는 지연 절반만큼 빼서 맞춤)
     * @param snapshotIntervalMillis 상대에게 내 화면 상태를 보내는 주기 (지연 200ms 이하 목표)
     */
    public record Timing(long startDelayMillis, long snapshotIntervalMillis) {
        public static final Timing DEFAULT = new Timing(3000, 50);

        public Timing {
            if (startDelayMillis < 0 || snapshotIntervalMillis <= 0) {
                throw new IllegalArgumentException("invalid timing");
            }
        }
    }

    public static final String HOST_PLAYER = "PLAYER1";
    public static final String CLIENT_PLAYER = "PLAYER2";
    public static final String REASON_PLAYER_LEFT = "PLAYER_LEFT";
    public static final String REASON_CONNECTION_LOST = "CONNECTION_LOST";

    private static final NetworkEvents NO_EVENTS = new NetworkEvents() {
    };

    private final NetworkService network;
    private final BattleGateway battle;
    private final RecentHostStore recentHosts;
    private final Executor ui;
    private final Timing timing;
    private final ScheduledExecutorService timer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "p2p-controller");
                thread.setDaemon(true);
                return thread;
            });

    private volatile NetworkEvents events = NO_EVENTS;

    // 아래 상태는 this로 보호. 엔진 스레드에서 오는 send*는 volatile 값만 읽는다.
    private volatile State state = State.DISCONNECTED;
    private volatile long battleId;
    private volatile long battleStartNanos;
    private volatile boolean host;
    private MessagePayload.Config config;
    private boolean localReady;
    private boolean peerReady;
    private boolean paused;
    private boolean rematchRequested;
    private long lastSnapshotSequence;
    private String joiningHost;
    private int joiningPort;
    private ScheduledFuture<?> startTask;
    private ScheduledFuture<?> snapshotTask;

    /** 앱에서 쓰는 생성자: 2번 BattleService에 연결하고, 화면 통지는 JavaFX Application Thread로 */
    public NetworkController(NetworkService network, BattleService battleService, RecentHostStore recentHosts) {
        this(network, new BattleServiceGateway(battleService), recentHosts);
    }

    /** 화면 통지를 JavaFX Application Thread로 보내는 기본 생성자 */
    public NetworkController(NetworkService network, BattleGateway battle, RecentHostStore recentHosts) {
        this(network, battle, recentHosts, Platform::runLater, Timing.DEFAULT);
    }

    public NetworkController(NetworkService network, BattleGateway battle, RecentHostStore recentHosts,
                             Executor uiExecutor, Timing timing) {
        this.network = Objects.requireNonNull(network, "network must not be null");
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        this.recentHosts = recentHosts;
        this.ui = Objects.requireNonNull(uiExecutor, "uiExecutor must not be null");
        this.timing = Objects.requireNonNull(timing, "timing must not be null");
        network.setListener(new Listener());
    }

    /** 화면 통지 대상 등록. null이면 해제 */
    public void setEvents(NetworkEvents events) {
        this.events = events == null ? NO_EVENTS : events;
    }

    // ================================================================ 연결

    /** 호스트(서버) 연결 대기 시작 */
    public synchronized void host(int port) {
        requireState(State.DISCONNECTED);
        resetSession(true);
        changeState(State.HOSTING);
        network.openHost(port);
        if (network.isOpening()) {
            int openPort = network.getLocalPort();
            status("접속 대기 중 - IP " + String.join(", ", getHostAddresses()) + " / 포트 " + openPort);
        } else if (state == State.HOSTING) {
            changeState(State.DISCONNECTED);   // 포트 열기 실패 (오류는 onError로 이미 표시)
        }
    }

    /** 상대 호스트 접속 */
    public synchronized void join(String hostAddress, int port) {
        requireState(State.DISCONNECTED);
        Objects.requireNonNull(hostAddress, "host must not be null");
        resetSession(false);
        joiningHost = hostAddress.trim();
        joiningPort = port;
        changeState(State.CONNECTING);
        try {
            network.connect(hostAddress, port);
        } catch (RuntimeException e) {
            changeState(State.DISCONNECTED);
            throw e;
        }
        status(joiningHost + ":" + port + " 에 연결 중...");
    }

    /** 서버 화면에 표시할 이 PC의 IP 목록 */
    public List<String> getHostAddresses() {
        return NetworkService.getLocalAddresses();
    }

    /** 클라이언트가 고를 수 있는 최근 접속 주소 (최근 순) */
    public List<RecentHostStore.HostEntry> getRecentHosts() {
        return recentHosts == null ? List.of() : recentHosts.load();
    }

    // ================================================================ 설정·준비·시작

    /** 모드·난이도·제한시간 설정을 상대에게 전달 (서버만). 바뀌면 양쪽 준비 상태 초기화 */
    public synchronized void sendBattleConfig(MessagePayload.Config newConfig) {
        Objects.requireNonNull(newConfig, "config must not be null");
        if (!host) {
            throw new IllegalStateException("only the host selects the battle mode");
        }
        requireState(State.CONNECTED);
        config = newConfig;
        battleId++;
        localReady = false;
        peerReady = false;
        network.send(NetworkMessage.of(MessageType.CONFIG, battleId, newConfig));
        notifyConfig(newConfig);
    }

    /** 명세 시그니처: BattleConfig로 설정 전달 */
    public void sendBattleConfig(BattleConfig newConfig) {
        Objects.requireNonNull(newConfig, "config must not be null");
        sendBattleConfig(PayloadMapper.toPayload(newConfig));
    }

    /** 자신의 준비 상태 전달. 양쪽 준비 완료 시 서버가 공통 시작 절차 진행 */
    public synchronized void setReady(boolean ready) {
        requireState(State.CONNECTED);
        if (config == null) {
            throw new IllegalStateException("battle config has not been selected");
        }
        localReady = ready;
        network.send(NetworkMessage.of(MessageType.READY, battleId, new MessagePayload.Ready(ready)));
        status(ready ? (peerReady ? "양쪽 준비 완료" : "상대 준비를 기다리는 중...") : "준비 취소");
        startIfBothReady();
    }

    // 서버만 시작 시점을 정해 START를 보낸다
    private void startIfBothReady() {
        if (!host || !localReady || !peerReady || state != State.CONNECTED) {
            return;
        }
        long delay = timing.startDelayMillis();
        network.send(NetworkMessage.of(MessageType.START, battleId, new MessagePayload.Start(delay)));
        scheduleStart(delay);
    }

    private void scheduleStart(long delayMillis) {
        changeState(State.STARTING);
        long id = battleId;
        startTask = timer.schedule(() -> beginBattle(id), delayMillis, TimeUnit.MILLISECONDS);
        ui(() -> events.onBattleStarting(delayMillis));
        status((delayMillis / 1000.0) + "초 후 대전 시작");
    }

    private synchronized void beginBattle(long id) {
        if (state != State.STARTING || battleId != id) {
            return;   // 그 사이에 나가기·끊김
        }
        lastSnapshotSequence = 0;
        paused = false;
        battle.setOutgoingAttackListener(this::sendAttack);
        battle.setLocalGameOverListener(this::sendGameOver);
        try {
            battle.startNetworkBattle(config, host);
        } catch (RuntimeException e) {
            // 받은 설정이 잘못된 경우 등: 대전을 시작하지 않고 대기 상태로 돌아간다
            stopTasks();
            localReady = false;
            peerReady = false;
            changeState(State.CONNECTED);
            ui(() -> events.showError("대전을 시작할 수 없습니다: " + e.getMessage()));
            return;
        }
        battleStartNanos = System.nanoTime();
        changeState(State.IN_BATTLE);
        long interval = timing.snapshotIntervalMillis();
        snapshotTask = timer.scheduleAtFixedRate(this::syncTick, 0, interval, TimeUnit.MILLISECONDS);
        ui(events::onBattleStarted);
    }

    // 주기 작업: 내 상태 전송, 서버는 결과가 확정되면 판정 전달
    private void syncTick() {
        try {
            if (state != State.IN_BATTLE) {
                return;
            }
            MessagePayload.Snapshot snapshot = battle.getLocalSnapshot();
            if (snapshot != null) {
                sendSnapshot(snapshot);
            }
            if (host) {
                MessagePayload.Result result = battle.getResult();
                if (result != null) {
                    finishAsJudge(result, battleId);
                }
            }
        } catch (RuntimeException e) {
            // 주기 작업이 예외로 멈추지 않게 함
            ui(() -> events.showError("상태 동기화 오류: " + e.getMessage()));
        }
    }

    private synchronized void finishAsJudge(MessagePayload.Result result, long id) {
        if (state != State.IN_BATTLE || battleId != id) {
            return;
        }
        sendBattleResult(result);
        finishBattle(result);
    }

    // ================================================================ 대전 중 송신 (엔진·타이머 스레드에서 호출 가능)

    /** 로컬 엔진이 생성한 공격 전달. 상대가 받으면 receiveRemoteAttack */
    public void sendAttack(MessagePayload.Attack attack) {
        Objects.requireNonNull(attack, "attack must not be null");
        if (state == State.IN_BATTLE) {
            network.send(NetworkMessage.of(MessageType.ATTACK, battleId, attack));
        }
    }

    /** 상대 화면 표시용 상태 전달. 오래된 상태는 송신 쪽에서 버리고 수신 쪽에서 번호로 거른다 */
    public void sendSnapshot(MessagePayload.Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (state == State.IN_BATTLE) {
            network.send(NetworkMessage.of(MessageType.SNAPSHOT, battleId, snapshot));
        }
    }

    /** 명세 시그니처: AttackRows로 공격 전달. 빈 공격은 보내지 않음 */
    public void sendAttack(AttackRows attackRows) {
        MessagePayload.Attack attack = PayloadMapper.toPayload(attackRows);
        if (attack != null) {
            sendAttack(attack);
        }
    }

    /** 명세 시그니처: GameSnapshot으로 상태 전달 */
    public void sendSnapshot(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        sendSnapshot(PayloadMapper.toPayload(snapshot));
    }

    /** 명세 시그니처: BattleResult로 결과 전달 (서버만) */
    public void sendBattleResult(BattleResult result) {
        Objects.requireNonNull(result, "result must not be null");
        sendBattleResult(PayloadMapper.toPayload(result));
    }

    /** 로컬 게임오버와 판정에 필요한 진행 시점(경과 시간·점수) 전달 */
    public void sendGameOver() {
        if (state != State.IN_BATTLE) {
            return;
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - battleStartNanos);
        MessagePayload.Snapshot snapshot = battle.getLocalSnapshot();
        int score = snapshot == null ? 0 : snapshot.score();
        network.send(NetworkMessage.of(MessageType.GAME_OVER, battleId,
                new MessagePayload.GameOver(Math.max(0, elapsed), score)));
    }

    /** 판정 담당(서버)이 확정한 결과 전달 */
    public void sendBattleResult(MessagePayload.Result result) {
        Objects.requireNonNull(result, "result must not be null");
        if (!host) {
            throw new IllegalStateException("only the host sends the battle result");
        }
        network.send(NetworkMessage.of(MessageType.RESULT, battleId, result));
    }

    // ================================================================ 일시정지·재대전·나가기

    /** 양쪽 일시정지 요청 */
    public synchronized void requestPause() {
        if (state != State.IN_BATTLE || paused) {
            return;
        }
        network.send(NetworkMessage.of(MessageType.PAUSE, battleId));
        applyPause(true);
    }

    /** 양쪽 재개 요청 */
    public synchronized void requestResume() {
        if (state != State.IN_BATTLE || !paused) {
            return;
        }
        network.send(NetworkMessage.of(MessageType.RESUME, battleId));
        applyPause(false);
    }

    private void applyPause(boolean pause) {
        paused = pause;
        if (pause) {
            battle.pause();
        } else {
            battle.resume();
        }
        ui(() -> events.onPauseChanged(pause));
    }

    /** 재대전 요청 전달 (대전 종료 후 대기 상태에서) */
    public synchronized void requestRematch() {
        requireState(State.CONNECTED);
        rematchRequested = true;
        network.send(NetworkMessage.of(MessageType.REMATCH_REQUEST, battleId));
        status("재대전 요청을 보냈습니다.");
    }

    /** 재대전 수락·거절 전달. 수락하면 바로 준비 상태가 되어 양쪽 동의 후 시작 절차 진행 */
    public synchronized void respondRematch(boolean accepted) {
        requireState(State.CONNECTED);
        network.send(NetworkMessage.of(MessageType.REMATCH_RESPONSE, battleId,
                new MessagePayload.RematchResponse(accepted)));
        if (accepted && config != null) {
            setReady(true);
        }
    }

    /**
     * 명시적 나가기. LEAVE를 보낸 뒤 연결을 정리한다.
     * 상대는 PLAYER_LEFT로 승리 처리하며, 이 종료를 연결 끊김으로 따로 처리하지 않는다.
     */
    public synchronized void leave() {
        if (state == State.DISCONNECTED) {
            return;
        }
        boolean wasPlaying = state == State.IN_BATTLE || state == State.STARTING;
        if (network.isConnected()) {
            network.send(NetworkMessage.of(MessageType.LEAVE, battleId));
        }
        stopTasks();
        if (wasPlaying) {
            battle.stop();
        }
        network.disconnect();
        changeState(State.DISCONNECTED);
        status("연결을 종료했습니다.");
    }

    /** 앱 종료 시 정리 */
    public void shutdown() {
        leave();
        timer.shutdownNow();
        network.shutdown();
    }

    // ================================================================ 수신

    /**
     * 받은 메시지 분기 (설정·준비·시작·공격·상태·승패·일시정지·재대전·나가기).
     * 대전 번호가 다른 대전 메시지는 버린다. 엔진 상태는 BattleGateway로만 변경.
     */
    public synchronized void handleMessage(NetworkMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        switch (message.getType()) {
            case CONFIG -> onConfig(message);
            case READY -> onReady(message);
            case START -> onStart(message);
            case ATTACK -> {
                if (inCurrentBattle(message)) {
                    battle.receiveRemoteAttack(message.getPayload(MessagePayload.Attack.class));
                }
            }
            case SNAPSHOT -> {
                if (inCurrentBattle(message) && message.getSequence() > lastSnapshotSequence) {
                    lastSnapshotSequence = message.getSequence();
                    battle.updateRemoteSnapshot(message.getPayload(MessagePayload.Snapshot.class));
                }
            }
            case GAME_OVER -> {
                if (inCurrentBattle(message)) {
                    battle.notifyRemoteGameOver();
                }
            }
            case RESULT -> onResult(message);
            case PAUSE -> {
                if (inCurrentBattle(message) && !paused) {
                    applyPause(true);
                }
            }
            case RESUME -> {
                if (inCurrentBattle(message) && paused) {
                    applyPause(false);
                }
            }
            case REMATCH_REQUEST -> {
                if (state == State.CONNECTED) {
                    ui(events::onRematchRequested);
                }
            }
            case REMATCH_RESPONSE -> onRematchResponse(message);
            case LEAVE -> onPeerLeft();
            case PING, PONG -> {
                // NetworkService에서 처리됨
            }
        }
    }

    private void onConfig(NetworkMessage message) {
        if (host || state != State.CONNECTED) {
            return;   // 설정은 서버만 정함. 대전 중에는 대전 종료 처리 후에만 바뀜
        }
        config = message.getPayload(MessagePayload.Config.class);
        battleId = message.getBattleId();
        localReady = false;
        peerReady = false;
        notifyConfig(config);
    }

    private void onReady(NetworkMessage message) {
        if (state != State.CONNECTED || message.getBattleId() != battleId) {
            return;   // 이전 설정에 대한 준비 응답은 무시
        }
        peerReady = message.getPayload(MessagePayload.Ready.class).ready();
        boolean ready = peerReady;
        ui(() -> events.onPeerReadyChanged(ready));
        status(ready ? "상대 준비 완료" : "상대가 준비를 취소했습니다.");
        startIfBothReady();
    }

    private void onStart(NetworkMessage message) {
        if (host || state != State.CONNECTED || message.getBattleId() != battleId || config == null) {
            return;
        }
        long delay = message.getPayload(MessagePayload.Start.class).startDelayMillis();
        long rtt = network.getRoundTripMillis();
        // START가 오는 데 걸린 시간(왕복의 절반)만큼 빼서 양쪽 시작 시점을 맞춘다
        scheduleStart(Math.max(0, delay - Math.max(0, rtt) / 2));
    }

    private void onResult(NetworkMessage message) {
        if (host || !inCurrentBattle(message)) {
            return;
        }
        MessagePayload.Result result = message.getPayload(MessagePayload.Result.class);
        battle.applyRemoteResult(result);
        finishBattle(result);
    }

    private void onRematchResponse(NetworkMessage message) {
        if (!rematchRequested || state != State.CONNECTED) {
            return;
        }
        rematchRequested = false;
        boolean accepted = message.getPayload(MessagePayload.RematchResponse.class).accepted();
        ui(() -> events.onRematchAnswered(accepted));
        if (accepted && config != null) {
            setReady(true);
        } else if (!accepted) {
            status("상대가 재대전을 거절했습니다.");
        }
    }

    private void onPeerLeft() {
        if (state == State.DISCONNECTED) {
            return;
        }
        boolean wasPlaying = state == State.IN_BATTLE || state == State.STARTING;
        stopTasks();
        if (wasPlaying) {
            MessagePayload.Result result = null;
            if (state == State.IN_BATTLE) {
                battle.notifyRemoteLeft();
                result = battle.getResult();
            }
            if (result == null) {
                result = new MessagePayload.Result(host ? HOST_PLAYER : CLIENT_PLAYER, false, REASON_PLAYER_LEFT);
            }
            battle.stop();
            MessagePayload.Result finalResult = result;
            ui(() -> events.onBattleFinished(finalResult));
        }
        // 상대가 이미 나갔으므로 직접 정리 (이후 소켓 종료를 끊김으로 처리하지 않음)
        network.disconnect();
        changeState(State.DISCONNECTED);
        ui(() -> events.onConnectionClosed("상대가 나갔습니다.", true));
        status("상대가 나갔습니다.");
    }

    private synchronized void onConnectionLost(String reason) {
        if (state == State.DISCONNECTED) {
            return;
        }
        boolean wasPlaying = state == State.IN_BATTLE || state == State.STARTING;
        stopTasks();
        if (state == State.IN_BATTLE) {
            battle.notifyConnectionLost();
        }
        if (wasPlaying) {
            battle.stop();
        }
        changeState(State.DISCONNECTED);
        ui(() -> events.showError(reason));
        ui(() -> events.onConnectionClosed(reason, false));
    }

    private void finishBattle(MessagePayload.Result result) {
        stopTasks();
        battle.stop();
        localReady = false;
        peerReady = false;
        paused = false;
        rematchRequested = false;
        changeState(State.CONNECTED);
        ui(() -> events.onBattleFinished(result));
        status("대전 종료 - 게임 시작을 누르면 새 대전을 시작합니다.");
        if (host && config != null) {
            // 다음 대전 번호를 알려 이전 대전의 늦은 메시지를 구분
            battleId++;
            network.send(NetworkMessage.of(MessageType.CONFIG, battleId, config));
        }
    }

    // ================================================================ 조회

    public State getState() {
        return state;
    }

    /** 서버(판정 담당)인지 */
    public synchronized boolean isHost() {
        return host;
    }

    /** 현재 대전 설정. 아직 선택 전이면 null */
    public synchronized MessagePayload.Config getConfig() {
        return config;
    }

    public long getBattleId() {
        return battleId;
    }

    public synchronized boolean isLocalReady() {
        return localReady;
    }

    public synchronized boolean isPeerReady() {
        return peerReady;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    // ================================================================ 내부

    private boolean inCurrentBattle(NetworkMessage message) {
        return state == State.IN_BATTLE && message.getBattleId() == battleId;
    }

    private void resetSession(boolean asHost) {
        host = asHost;
        config = null;
        battleId = 0;
        localReady = false;
        peerReady = false;
        paused = false;
        rematchRequested = false;
        lastSnapshotSequence = 0;
        joiningHost = null;
    }

    private void stopTasks() {
        if (startTask != null) {
            startTask.cancel(false);
            startTask = null;
        }
        if (snapshotTask != null) {
            snapshotTask.cancel(false);
            snapshotTask = null;
        }
        battle.setOutgoingAttackListener(null);
        battle.setLocalGameOverListener(null);
    }

    private void requireState(State expected) {
        if (state != expected) {
            throw new IllegalStateException("expected " + expected + " but was " + state);
        }
    }

    private void changeState(State newState) {
        if (state == newState) {
            return;
        }
        state = newState;
        ui(() -> events.onStateChanged(newState));
    }

    private void notifyConfig(MessagePayload.Config newConfig) {
        ui(() -> events.onConfigChanged(newConfig));
        ui(() -> events.onPeerReadyChanged(false));
        status("대전 설정: " + newConfig.mode()
                + (newConfig.timeLimitSeconds() > 0 ? " " + newConfig.timeLimitSeconds() + "초" : ""));
    }

    private void status(String text) {
        ui(() -> events.showConnectionStatus(text));
    }

    private void ui(Runnable task) {
        ui.execute(task);
    }

    // NetworkService 콜백 (통신 스레드)
    private final class Listener implements NetworkListener {
        @Override
        public void onConnected() {
            synchronized (NetworkController.this) {
                if (state != State.HOSTING && state != State.CONNECTING) {
                    return;
                }
                if (!host && recentHosts != null && joiningHost != null) {
                    recentHosts.add(joiningHost, joiningPort);
                }
                changeState(State.CONNECTED);
                status(host
                        ? "상대가 접속했습니다 (" + network.getRemoteAddress() + "). 대전 모드를 선택하세요."
                        : "서버에 연결되었습니다. 서버가 대전 모드를 선택하기를 기다리는 중...");
            }
        }

        @Override
        public void onMessage(NetworkMessage message) {
            handleMessage(message);
        }

        @Override
        public void onDisconnected() {
            onConnectionLost("상대와의 연결이 끊어졌습니다.");
        }

        @Override
        public void onError(String message) {
            synchronized (NetworkController.this) {
                ui(() -> events.showError(message));
                if ((state == State.HOSTING || state == State.CONNECTING)
                        && !network.isOpening() && !network.isConnected()) {
                    changeState(State.DISCONNECTED);
                }
            }
        }

        @Override
        public void onLatencyChanged(long roundTripMillis, boolean lagging) {
            ui(() -> events.onLagChanged(roundTripMillis, lagging));
        }
    }
}
