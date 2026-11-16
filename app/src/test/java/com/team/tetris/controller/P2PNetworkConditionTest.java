package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
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
import com.team.tetris.network.LatencyProxy;
import com.team.tetris.network.NetworkService;
import com.team.tetris.service.BattleService;

/**
 * [요구사항 테스트] 네트워크 문제 상황에서의 P2P 대전.
 *  - "실제 평가시 네트워크 속도가 많이 저하될 수 있으니 이를 고려하여 테스트를 준비"
 *  - "랙 걸린 상태를 플레이어에게 표시"
 *  - "연결 자체가 일정 시간 이상 끊어지면 에러 메시지를 표시하고 P2P 첫 화면으로"
 * 클라이언트와 서버 사이에 LatencyProxy를 두어 느린 네트워크·일시 멈춤·선 끊김을 재현한다.
 * 실제 2번 BattleService를 사용한다. (화면 3번 제외)
 */
@Timeout(value = 40, unit = TimeUnit.SECONDS)
class P2PNetworkConditionTest {

    // 랙 기준 200ms(실제 기본값과 같음), 끊김 판정은 테스트 시간을 줄이려고 1.5초
    private static final NetworkService.Timing NET = new NetworkService.Timing(1000, 50, 200, 1500);
    private static final NetworkController.Timing START = new NetworkController.Timing(500, 20);
    private static final long WAIT_SECONDS = 15;

    private final List<NetworkController> controllers = new ArrayList<>();
    private final List<BattleService> battles = new ArrayList<>();
    private LatencyProxy proxy;

    @AfterEach
    void cleanUp() throws IOException {
        controllers.forEach(NetworkController::shutdown);
        battles.forEach(BattleService::stop);
        if (proxy != null) {
            proxy.close();
        }
    }

    private final class Pc {
        final BattleService battle = new BattleService();
        final NetworkService network = new NetworkService(NET);
        final RecordingNetworkEvents events = new RecordingNetworkEvents();
        final NetworkController controller =
                new NetworkController(network, new BattleServiceGateway(battle), null, Runnable::run, START);

        Pc() {
            controller.setEvents(events);
            controllers.add(controller);
            battles.add(battle);
        }
    }

    private Pc server;
    private Pc client;

    /** 클라이언트 ─(oneWayDelayMillis 지연)─ 프록시 ─ 서버 로 연결 */
    private void connectThroughProxy(long oneWayDelayMillis) throws Exception {
        server = new Pc();
        client = new Pc();
        server.controller.host(0);
        proxy = new LatencyProxy(server.network.getLocalPort(), oneWayDelayMillis);
        client.controller.join("127.0.0.1", proxy.getPort());
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
    }

    private void startBattle() throws InterruptedException {
        server.controller.sendBattleConfig(BattleConfig.normal(Difficulty.NORMAL));
        waitUntil(() -> client.controller.getConfig() != null);
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        server.controller.setReady(true);
        waitUntil(() -> server.battle.getState() == BattleState.RUNNING
                && client.battle.getState() == BattleState.RUNNING);
    }

    // ================================================================ 느린 네트워크

    @Test
    void slowNetwork_lagIsShownButConnectionIsKept() throws Exception {
        connectThroughProxy(150);   // 왕복 300ms > 랙 기준 200ms

        waitUntil(() -> client.events.lagStates.contains(true) && server.events.lagStates.contains(true));
        assertTrue(client.network.isLagging());
        Thread.sleep(NET.disconnectTimeoutMillis() + 500);
        assertEquals(State.CONNECTED, client.controller.getState(), "느리기만 하면 끊지 않음");
        assertEquals(State.CONNECTED, server.controller.getState());
        assertTrue(client.events.errors.isEmpty(), client.events.errors.toString());
    }

    @Test
    void slowNetwork_bothSidesStartAtAboutTheSameTime() throws Exception {
        connectThroughProxy(150);
        waitUntil(() -> client.network.getRoundTripMillis() > 0);

        startBattle();

        long gapMillis = TimeUnit.NANOSECONDS.toMillis(
                Math.abs(server.events.battleStartedAtNanos - client.events.battleStartedAtNanos));
        // 보정이 없으면 한 방향 지연(150ms)만큼 어긋남. 왕복 지연의 절반을 빼서 맞춘다
        assertTrue(gapMillis < 100, "양쪽 시작 시각 차이: " + gapMillis + "ms");
    }

    @Test
    void slowNetwork_fullBattleStillWorks() throws Exception {
        connectThroughProxy(120);
        startBattle();

        // 상대 화면 표시
        waitUntil(() -> hasCurrentBlock(server.battle.getPlayerSnapshot(PlayerId.PLAYER2))
                && hasCurrentBlock(client.battle.getPlayerSnapshot(PlayerId.PLAYER1)));
        // 공격 전달
        server.controller.sendAttack(new AttackRows(attackRows(2)));
        waitUntil(() -> client.battle.getPlayerSnapshot(PlayerId.PLAYER2).getPendingAttack().getRowCount() >= 2);
        // 실제 게임오버 → 서버 판정 → 양쪽 같은 결과
        stackUntilGameOver(client, PlayerId.PLAYER2, server);
        BattleResult expected = BattleResult.win(PlayerId.PLAYER1, BattleEndReason.GAME_OVER);
        waitUntil(() -> expected.equals(server.battle.getResult()) && expected.equals(client.battle.getResult()));
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
    }

    // ================================================================ 일시 멈춤 (회복)

    @Test
    void shortStall_showsLagThenRecoversWithoutEndingBattle() throws Exception {
        connectThroughProxy(0);
        startBattle();
        waitUntil(() -> client.events.lagStates.contains(false));
        client.events.lagStates.clear();

        proxy.setStalled(true);   // 끊김 기준(1.5초)보다 짧게 멈춤
        waitUntil(() -> client.events.lagStates.contains(true));
        Thread.sleep(300);
        proxy.setStalled(false);

        waitUntil(() -> {
            List<Boolean> states = client.events.lagStates;
            return !states.isEmpty() && !states.get(states.size() - 1);
        });
        assertFalse(client.network.isLagging(), "회복되면 랙 표시 해제");
        assertEquals(State.IN_BATTLE, client.controller.getState());
        assertEquals(State.IN_BATTLE, server.controller.getState());
        assertTrue(client.events.errors.isEmpty());
        assertNull(client.battle.getResult());
    }

    // ================================================================ 선 끊김

    @Test
    void cableCutDuringBattle_bothSidesShowErrorAndReturnToStart() throws Exception {
        connectThroughProxy(0);
        startBattle();

        proxy.setBlackhole(true);   // 소켓은 열린 채 데이터만 사라짐

        waitUntil(() -> client.controller.getState() == State.DISCONNECTED
                && server.controller.getState() == State.DISCONNECTED);
        for (Pc pc : List.of(server, client)) {
            waitUntil(() -> !pc.events.closedByPeerLeft.isEmpty());
            assertEquals(List.of(false), pc.events.closedByPeerLeft, "끊김은 나가기와 구분");
            assertFalse(pc.events.errors.isEmpty(), "에러 메시지 표시");
            BattleResult result = pc.battle.getResult();
            assertEquals(BattleEndReason.CONNECTION_LOST, result.getReason());
            assertNull(result.getWinner());
            assertFalse(result.isDraw());
        }
    }

    @Test
    void cableCutInWaitingRoom_returnsToStartWithError() throws Exception {
        connectThroughProxy(0);
        server.controller.sendBattleConfig(BattleConfig.item());
        waitUntil(() -> client.controller.getConfig() != null);

        proxy.setBlackhole(true);

        waitUntil(() -> client.controller.getState() == State.DISCONNECTED
                && server.controller.getState() == State.DISCONNECTED);
        assertFalse(client.events.errors.isEmpty());
        assertFalse(server.events.errors.isEmpty());
        assertEquals(BattleState.WAITING, client.battle.getState(), "대전 전이므로 엔진은 시작하지 않음");
    }

    @Test
    void afterCableCut_canConnectAgain() throws Exception {
        connectThroughProxy(0);
        proxy.setBlackhole(true);
        waitUntil(() -> client.controller.getState() == State.DISCONNECTED
                && server.controller.getState() == State.DISCONNECTED);
        proxy.close();

        server.controller.host(0);
        client.controller.join("127.0.0.1", server.network.getLocalPort());

        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
    }

    // ================================================================ 도우미

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

    private static boolean hasCurrentBlock(GameSnapshot snapshot) {
        return snapshot != null && snapshot.getCurrentBlock() != null;
    }

    private static BlockType[][] attackRows(int count) {
        BlockType[][] rows = new BlockType[count][Board.COLS];
        for (int row = 0; row < count; row++) {
            for (int col = 1; col < Board.COLS; col++) {
                rows[row][col] = BlockType.J;
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
