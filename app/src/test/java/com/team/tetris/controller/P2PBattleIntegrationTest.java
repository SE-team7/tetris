package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.team.tetris.controller.NetworkController.State;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleEndReason;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.network.BattleServiceGateway;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.network.NetworkService;
import com.team.tetris.service.BattleService;

/**
 * [요구사항 테스트] P2P 대전 통합 테스트.
 * 실제 2번 BattleService 두 개를 각각 NetworkController(4번)에 연결하고 실제 소켓으로 붙여
 * "두 PC" 대전을 한 프로세스에서 재현한다.
 *  - 양쪽 준비 후 같은 설정으로 시작, 서버 PLAYER1 / 클라이언트 PLAYER2
 *  - 상대 화면 상태가 실시간으로 반영됨
 *  - 공격이 상대 대기열에 들어감
 *  - 실제 게임오버 → 서버 판정 → 양쪽 같은 결과 → 대기 상태 → 새 대전
 *  - 일시정지 동기화, 나가기(PLAYER_LEFT), 연결 끊김(CONNECTION_LOST)
 * 화면(3번)은 포함하지 않는다.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class P2PBattleIntegrationTest {

    private static final NetworkService.Timing NET = new NetworkService.Timing(1000, 50, 150, 800);
    private static final NetworkController.Timing FAST = new NetworkController.Timing(100, 20);
    private static final long WAIT_SECONDS = 10;

    private final List<NetworkController> controllers = new ArrayList<>();
    private final List<BattleService> battles = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        controllers.forEach(NetworkController::shutdown);
        battles.forEach(BattleService::stop);
    }

    /** 한 PC */
    private final class Pc {
        final BattleService battle = new BattleService();
        final NetworkService network = new NetworkService(NET);
        final RecordingNetworkEvents events = new RecordingNetworkEvents();
        final NetworkController controller =
                new NetworkController(network, new BattleServiceGateway(battle), null, Runnable::run, FAST);

        Pc() {
            controller.setEvents(events);
            controllers.add(controller);
            battles.add(battle);
        }
    }

    private Pc server;
    private Pc client;

    private void startBattle(BattleConfig config) throws InterruptedException {
        server = new Pc();
        client = new Pc();
        server.controller.host(0);
        client.controller.join("127.0.0.1", server.network.getLocalPort());
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
        server.controller.sendBattleConfig(config);   // 명세 시그니처 (BattleConfig)
        waitUntil(() -> client.controller.getConfig() != null);
        readyBoth();
    }

    private void readyBoth() throws InterruptedException {
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        server.controller.setReady(true);
        waitUntil(() -> server.battle.getState() == BattleState.RUNNING
                && client.battle.getState() == BattleState.RUNNING);
    }

    // ---------------------------------------------------------------- 시작

    @Test
    void bothSidesStartSameBattleAsPlayer1AndPlayer2() throws InterruptedException {
        BattleConfig config = BattleConfig.timeLimit(Difficulty.HARD, 300);

        startBattle(config);

        assertEquals(config, server.battle.getConfig());
        assertEquals(config, client.battle.getConfig());
        assertTrue(server.battle.isNetworkBattle());
        assertTrue(client.battle.isNetworkBattle());
        assertEquals(PlayerId.PLAYER1, server.battle.getLocalPlayer());
        assertEquals(PlayerId.PLAYER2, client.battle.getLocalPlayer());
        assertEquals(State.IN_BATTLE, server.controller.getState());
        assertEquals(State.IN_BATTLE, client.controller.getState());
    }

    @Test
    void itemModeConfigIsDeliveredAsIs() throws InterruptedException {
        startBattle(BattleConfig.item());

        assertEquals(BattleConfig.item(), client.battle.getConfig());
    }

    // ---------------------------------------------------------------- 화면 상태

    @Test
    void opponentScreenIsUpdatedOnBothSides() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));

        // 상대의 현재 블럭까지 양쪽 화면에 표시
        waitUntil(() -> hasCurrentBlock(server.battle.getPlayerSnapshot(PlayerId.PLAYER2))
                && hasCurrentBlock(client.battle.getPlayerSnapshot(PlayerId.PLAYER1)));
        assertEquals(server.battle.getPlayerSnapshot(PlayerId.PLAYER1).getCurrentBlock().getType(),
                client.battle.getPlayerSnapshot(PlayerId.PLAYER1).getCurrentBlock().getType());
    }

    @Test
    void opponentMoveIsShownWithin200ms() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.EASY));
        waitUntil(() -> server.battle.getPlayerSnapshot(PlayerId.PLAYER2) != null
                && server.battle.getPlayerSnapshot(PlayerId.PLAYER2).getCurrentBlock() != null);
        int before = server.battle.getPlayerSnapshot(PlayerId.PLAYER2).getCurrentBlock().getPosition().getCol();

        long pressedAt = System.nanoTime();
        client.battle.submitAction(PlayerId.PLAYER2, KeyAction.MOVE_LEFT);   // 클라이언트 키 입력

        waitUntil(() -> {
            GameSnapshot seen = server.battle.getPlayerSnapshot(PlayerId.PLAYER2);
            return seen.getCurrentBlock() != null && seen.getCurrentBlock().getPosition().getCol() == before - 1;
        });
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pressedAt);
        assertTrue(elapsedMillis <= 200, "키 입력 후 상대 화면 표시까지 200ms 이하: " + elapsedMillis);
    }

    // ---------------------------------------------------------------- 공격

    @Test
    void attackFromClientEntersServerQueue() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));
        waitForFirstBlock(server, PlayerId.PLAYER1);

        client.controller.sendAttack(new AttackRows(attackRows(3)));   // 명세 시그니처 (AttackRows)

        waitUntil(() -> server.battle.getPlayerSnapshot(PlayerId.PLAYER1).getPendingAttack().getRowCount() == 3);
        // 클라이언트 화면에서도 서버의 대기 공격이 보임 (서버 상태 전송)
        waitUntil(() -> {
            GameSnapshot seen = client.battle.getPlayerSnapshot(PlayerId.PLAYER1);
            return seen != null && seen.getPendingAttack().getRowCount() == 3;
        });
    }

    @Test
    void attacksAreCappedAtTenRows() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));
        waitForFirstBlock(client, PlayerId.PLAYER2);

        for (int i = 0; i < 4; i++) {
            server.controller.sendAttack(new AttackRows(attackRows(4)));
        }

        waitUntil(() -> client.battle.getPlayerSnapshot(PlayerId.PLAYER2).getPendingAttack().getRowCount() == 10);
        Thread.sleep(200);
        assertEquals(10, client.battle.getPlayerSnapshot(PlayerId.PLAYER2).getPendingAttack().getRowCount());
    }

    // ---------------------------------------------------------------- 게임오버·판정

    @Test
    void realGameOverIsJudgedByServerAndBothShowSameResult() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));

        stackUntilGameOver(client, PlayerId.PLAYER2, server);

        BattleResult expected = BattleResult.win(PlayerId.PLAYER1, BattleEndReason.GAME_OVER);
        waitUntil(() -> expected.equals(server.battle.getResult()) && expected.equals(client.battle.getResult()));
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
        waitUntil(() -> !server.events.results.isEmpty() && !client.events.results.isEmpty());
        assertEquals(server.events.results.get(0), client.events.results.get(0), "양쪽 같은 승패 표시");
        assertEquals("PLAYER1", client.events.results.get(0).winner());
    }

    @Test
    void afterResultBothCanStartNewBattle() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));
        stackUntilGameOver(server, PlayerId.PLAYER1, client);
        waitUntil(() -> client.controller.getState() == State.CONNECTED
                && client.controller.getBattleId() == server.controller.getBattleId());
        assertEquals(PlayerId.PLAYER2, client.battle.getResult().getWinner());

        readyBoth();   // 대기 상태에서 다시 '게임 시작'

        assertNull(server.battle.getResult(), "새 대전은 결과 없이 시작");
        assertNull(client.battle.getResult());
        assertEquals(State.IN_BATTLE, client.controller.getState());
    }

    // ---------------------------------------------------------------- 일시정지·나가기·끊김

    @Test
    void pauseAndResumeAreSynchronized() throws InterruptedException {
        startBattle(BattleConfig.timeLimit(Difficulty.NORMAL, 180));

        client.controller.requestPause();

        waitUntil(() -> server.battle.getState() == BattleState.PAUSED
                && client.battle.getState() == BattleState.PAUSED);
        long remaining = server.battle.getRemainingMillis();
        Thread.sleep(200);
        assertEquals(remaining, server.battle.getRemainingMillis(), "일시정지 중 제한시간 멈춤");

        server.controller.requestResume();

        waitUntil(() -> server.battle.getState() == BattleState.RUNNING
                && client.battle.getState() == BattleState.RUNNING);
    }

    @Test
    void clientLeaves_serverWinsByPlayerLeft() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));

        client.controller.leave();

        waitUntil(() -> server.battle.getResult() != null);
        assertEquals(BattleResult.win(PlayerId.PLAYER1, BattleEndReason.PLAYER_LEFT), server.battle.getResult());
        waitUntil(() -> server.controller.getState() == State.DISCONNECTED);
        assertNotEquals(BattleState.RUNNING, client.battle.getState(), "나간 쪽 엔진도 멈춤");
    }

    @Test
    void serverLeaves_clientWinsByPlayerLeft() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));

        server.controller.leave();

        waitUntil(() -> client.battle.getResult() != null);
        assertEquals(BattleResult.win(PlayerId.PLAYER2, BattleEndReason.PLAYER_LEFT), client.battle.getResult());
    }

    @Test
    void connectionLost_endsWithoutWinnerAndNotDraw() throws InterruptedException {
        startBattle(BattleConfig.normal(Difficulty.NORMAL));

        server.network.disconnect();   // LEAVE 없이 연결만 끊김

        waitUntil(() -> client.battle.getResult() != null);
        BattleResult result = client.battle.getResult();
        assertNull(result.getWinner());
        assertFalse(result.isDraw(), "연결 끊김은 무승부와 구분");
        assertEquals(BattleEndReason.CONNECTION_LOST, result.getReason());
        waitUntil(() -> !client.events.errors.isEmpty());
        assertEquals(State.DISCONNECTED, client.controller.getState());
    }

    @Test
    void invalidConfigFromPeer_doesNotStartAndShowsError() throws InterruptedException {
        server = new Pc();
        client = new Pc();
        server.controller.host(0);
        client.controller.join("127.0.0.1", server.network.getLocalPort());
        waitUntil(() -> client.controller.getState() == State.CONNECTED);
        // 다른 버전의 프로그램이 보낸, 이쪽에서 모르는 모드
        server.controller.sendBattleConfig(new MessagePayload.Config("SURVIVAL", Difficulty.NORMAL, 0));
        waitUntil(() -> client.controller.getConfig() != null);

        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        server.controller.setReady(true);

        waitUntil(() -> !client.events.errors.isEmpty() && !server.events.errors.isEmpty());
        waitUntil(() -> client.controller.getState() == State.CONNECTED
                && server.controller.getState() == State.CONNECTED);
        assertEquals(BattleState.WAITING, client.battle.getState());
    }

    // ---------------------------------------------------------------- 도우미

    /**
     * 실제 대전 흐름으로 게임오버를 만든다: 상대가 10줄 공격을 보내고, 피해자는 하드드랍.
     * (다음 블럭 생성 전에 공격이 삽입되어 보드가 위로 밀려 결국 게임오버)
     */
    private void stackUntilGameOver(Pc victim, PlayerId player, Pc attacker) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS * 2);
        while (!victim.battle.getPlayerSnapshot(player).isGameOver()) {
            if (System.nanoTime() > deadline) {
                fail("game over not reached");
            }
            if (victim.battle.getPlayerSnapshot(player).getPendingAttack().isEmpty()) {
                attacker.controller.sendAttack(new AttackRows(attackRows(10)));
            }
            victim.battle.submitAction(player, KeyAction.HARD_DROP);
            Thread.sleep(50);
        }
    }

    /** 첫 블럭이 생긴 뒤에 공격을 보내야 대기열에 남는다 (생성 직전 공격은 바로 보드에 삽입됨) */
    private static void waitForFirstBlock(Pc pc, PlayerId player) throws InterruptedException {
        waitUntil(() -> hasCurrentBlock(pc.battle.getPlayerSnapshot(player)));
    }

    private static boolean hasCurrentBlock(GameSnapshot snapshot) {
        return snapshot != null && snapshot.getCurrentBlock() != null;
    }

    private static BlockType[][] attackRows(int count) {
        BlockType[][] rows = new BlockType[count][Board.COLS];
        for (int row = 0; row < count; row++) {
            for (int col = 1; col < Board.COLS; col++) {
                rows[row][col] = BlockType.S;
            }
        }
        return rows;
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition not met in time");
            }
            Thread.sleep(5);
        }
    }
}
