package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleState;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.service.BattleService;

/** BattleServiceGateway: NetworkController의 호출이 실제 2번 BattleService에 올바르게 전달되는지 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class BattleServiceGatewayTest {

    private static final MessagePayload.Config NORMAL = new MessagePayload.Config("NORMAL", Difficulty.NORMAL, 0);

    private final BattleService battle = new BattleService();
    private final BattleServiceGateway gateway = new BattleServiceGateway(battle);

    @AfterEach
    void stopBattle() {
        battle.stop();
    }

    @Test
    void startNetworkBattle_hostIsPlayer1() {
        gateway.startNetworkBattle(NORMAL, true);

        assertEquals(PlayerId.PLAYER1, gateway.getLocalPlayer());
        assertEquals(PlayerId.PLAYER1, battle.getLocalPlayer());
        assertTrue(battle.isNetworkBattle());
        assertEquals(BattleState.RUNNING, battle.getState());
        assertEquals(PayloadMapper.toBattleConfig(NORMAL), battle.getConfig());
    }

    @Test
    void startNetworkBattle_clientIsPlayer2() {
        gateway.startNetworkBattle(new MessagePayload.Config("TIME_LIMIT", Difficulty.HARD, 300), false);

        assertEquals(PlayerId.PLAYER2, battle.getLocalPlayer());
        assertTrue(battle.getConfig().isTimeLimited());
        assertEquals(300, battle.getConfig().getTimeLimitSeconds());
    }

    @Test
    void startNetworkBattle_rejectsInvalidConfig() {
        assertThrows(IllegalArgumentException.class,
                () -> gateway.startNetworkBattle(new MessagePayload.Config("UNKNOWN", Difficulty.EASY, 0), true));
        assertEquals(BattleState.WAITING, battle.getState());
    }

    @Test
    void getLocalSnapshot_returnsLocalPlayerState() throws InterruptedException {
        gateway.startNetworkBattle(NORMAL, false);

        waitUntil(() -> gateway.getLocalSnapshot() != null && gateway.getLocalSnapshot().currentBlock() != null);
        MessagePayload.Snapshot snapshot = gateway.getLocalSnapshot();
        assertFalse(snapshot.gameOver());
        assertEquals(battle.getPlayerSnapshot(PlayerId.PLAYER2).getScore(), snapshot.score());
    }

    @Test
    void getLocalSnapshot_beforeStartIsNull() {
        assertNull(gateway.getLocalSnapshot());
    }

    @Test
    void receiveRemoteAttack_entersLocalPendingAttack() throws InterruptedException {
        gateway.startNetworkBattle(NORMAL, true);
        // 첫 블럭 생성 전에 받은 공격은 생성 직전에 바로 보드에 삽입되므로 블럭이 생긴 뒤 보냄
        waitUntil(() -> battle.getPlayerSnapshot(PlayerId.PLAYER1).getCurrentBlock() != null);

        gateway.receiveRemoteAttack(new MessagePayload.Attack(NetworkTestData.attackRows(3, 0)));

        waitUntil(() -> battle.getPlayerSnapshot(PlayerId.PLAYER1).getPendingAttack().getRowCount() == 3);
    }

    @Test
    void updateRemoteSnapshot_isShownAsOpponent() {
        gateway.startNetworkBattle(NORMAL, true);

        gateway.updateRemoteSnapshot(NetworkTestData.snapshot(4321));

        GameSnapshot opponent = battle.getPlayerSnapshot(PlayerId.PLAYER2);
        assertNotNull(opponent);
        assertEquals(4321, opponent.getScore());
        assertEquals(2, opponent.getPendingAttack().getRowCount());
    }

    @Test
    void notifyRemoteLeft_localPlayerWins() {
        gateway.startNetworkBattle(NORMAL, false);

        gateway.notifyRemoteLeft();

        assertEquals(new MessagePayload.Result("PLAYER2", false, "PLAYER_LEFT"), gateway.getResult());
        assertEquals(BattleState.FINISHED, battle.getState());
    }

    @Test
    void notifyConnectionLost_endsWithoutWinner() {
        gateway.startNetworkBattle(NORMAL, true);

        gateway.notifyConnectionLost();

        assertEquals(new MessagePayload.Result(null, false, "CONNECTION_LOST"), gateway.getResult());
    }

    @Test
    void applyRemoteResult_appliesJudgeResult() {
        gateway.startNetworkBattle(NORMAL, false);
        MessagePayload.Result judged = new MessagePayload.Result("PLAYER1", false, "GAME_OVER");

        gateway.applyRemoteResult(judged);

        assertEquals(judged, gateway.getResult());
        assertEquals(BattleState.FINISHED, battle.getState());
    }

    @Test
    void notifyRemoteGameOver_hostJudgesLocalWin() throws InterruptedException {
        gateway.startNetworkBattle(NORMAL, true);

        gateway.notifyRemoteGameOver();

        waitUntil(() -> gateway.getResult() != null);
        assertEquals(new MessagePayload.Result("PLAYER1", false, "GAME_OVER"), gateway.getResult());
    }

    @Test
    void getResult_beforeEndIsNull() {
        gateway.startNetworkBattle(NORMAL, true);

        assertNull(gateway.getResult());
    }

    @Test
    void pauseResumeAndStop_areForwarded() {
        gateway.startNetworkBattle(NORMAL, true);

        gateway.pause();
        assertEquals(BattleState.PAUSED, battle.getState());
        gateway.resume();
        assertEquals(BattleState.RUNNING, battle.getState());
        gateway.stop();
        assertEquals(BattleState.WAITING, battle.getState());
    }

    @Test
    void outgoingAttackListener_receivesConvertedAttack() {
        List<MessagePayload.Attack> sent = new CopyOnWriteArrayList<>();
        gateway.setOutgoingAttackListener(sent::add);
        gateway.startNetworkBattle(NORMAL, true);

        // 내 엔진이 만든 공격이 BattleService → 어댑터 → 리스너로 전달되는 경로
        java.util.function.Consumer<AttackRows> engineSide = captureOutgoing();
        engineSide.accept(new AttackRows(NetworkTestData.attackRows(2, 4)));
        engineSide.accept(AttackRows.empty());

        assertEquals(1, sent.size(), "빈 공격은 보내지 않음");
        assertEquals(2, sent.get(0).rowCount());
    }

    @Test
    void outgoingAttackListener_nullDisconnects() {
        List<MessagePayload.Attack> sent = new CopyOnWriteArrayList<>();
        gateway.setOutgoingAttackListener(sent::add);
        gateway.setOutgoingAttackListener(null);

        captureOutgoing().accept(new AttackRows(NetworkTestData.attackRows(2, 4)));

        assertTrue(sent.isEmpty());
    }

    @Test
    void localGameOverListener_isRegisteredOnBattleService() throws Exception {
        boolean[] called = new boolean[1];
        gateway.setLocalGameOverListener(() -> called[0] = true);

        Runnable registered = (Runnable) field("localGameOverListener");
        registered.run();

        assertTrue(called[0]);
    }

    @Test
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new BattleServiceGateway(null));
        assertSame(battle, gateway.getBattleService());
    }

    // BattleService에 등록된 공격 송신 콜백 (엔진이 공격을 만들면 이 콜백이 호출됨)
    @SuppressWarnings("unchecked")
    private java.util.function.Consumer<AttackRows> captureOutgoing() {
        try {
            return (java.util.function.Consumer<AttackRows>) field("outgoingAttackListener");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private Object field(String name) throws ReflectiveOperationException {
        var field = BattleService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(battle);
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition not met in time");
            }
            Thread.sleep(5);
        }
    }
}
