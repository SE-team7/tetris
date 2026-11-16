package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.team.tetris.FxTestSupport;
import com.team.tetris.controller.NetworkController.State;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.BattleConfig;
import com.team.tetris.domain.BattleEndReason;
import com.team.tetris.domain.BattleResult;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.network.MessageCodec;
import com.team.tetris.network.MessagePayload;
import com.team.tetris.network.MessageType;
import com.team.tetris.network.NetworkMessage;
import com.team.tetris.network.NetworkService;
import com.team.tetris.network.NetworkEvents;
import com.team.tetris.network.RecentHostStore;
import com.team.tetris.service.BattleService;
import com.team.tetris.util.FileManager;

import javafx.application.Platform;

/**
 * [요구사항 테스트] P2P 대전 진행 테스트.
 * 실제 소켓으로 연결한 서버·클라이언트 NetworkController 두 개와 기록용 가짜 대전(FakeBattleGateway)으로
 * 연결 → 서버 모드 선택 → 양쪽 '게임 시작' → 대전(공격·상태·게임오버) → 승패 표시 → 대기 상태 복귀 → 새 대전,
 * 일시정지·재대전·나가기·연결 끊김 처리를 확인한다.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class NetworkControllerTest {

    private static final NetworkService.Timing NET = new NetworkService.Timing(1000, 50, 150, 800);
    private static final NetworkController.Timing FAST = new NetworkController.Timing(100, 20);
    private static final MessagePayload.Config NORMAL = new MessagePayload.Config("NORMAL", Difficulty.NORMAL, 0);
    private static final MessagePayload.Config TIME = new MessagePayload.Config("TIME_LIMIT", Difficulty.HARD, 180);
    private static final long WAIT_SECONDS = 5;

    @TempDir
    Path tempDir;

    private final List<NetworkController> controllers = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        controllers.forEach(NetworkController::shutdown);
    }

    /** 한 PC의 플레이어 */
    private final class Side {
        final FakeBattleGateway battle = new FakeBattleGateway();
        final RecordingNetworkEvents events = new RecordingNetworkEvents();
        final NetworkService network = new NetworkService(NET);
        final RecentHostStore recent;
        final NetworkController controller;

        Side(String folder) {
            recent = new RecentHostStore(new FileManager(tempDir.resolve(folder)));
            controller = new NetworkController(network, battle, recent, Runnable::run, FAST);
            controller.setEvents(events);
            controllers.add(controller);
        }
    }

    private Side server;
    private Side client;
    private int serverPort;

    /** 서버 대기 → 클라이언트 접속까지 */
    private void connect() throws InterruptedException {
        server = new Side("server");
        client = new Side("client");
        server.controller.host(0);
        serverPort = server.network.getLocalPort();
        client.controller.join("127.0.0.1", serverPort);
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
    }

    /** 연결 → 서버 설정 → 양쪽 준비 → 대전 시작까지 */
    private void startBattle(MessagePayload.Config config) throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(config);
        waitUntil(() -> client.controller.getConfig() != null);
        readyBothAndWaitForBattle();
    }

    private void readyBothAndWaitForBattle() throws InterruptedException {
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        server.controller.setReady(true);
        waitUntil(() -> server.controller.getState() == State.IN_BATTLE
                && client.controller.getState() == State.IN_BATTLE);
    }

    // ================================================================ 연결

    @Test
    void host_waitsAndShowsAddressAndPort() {
        Side side = new Side("host");

        side.controller.host(0);

        assertEquals(State.HOSTING, side.controller.getState());
        assertTrue(side.controller.isHost());
        String status = side.events.statuses.get(side.events.statuses.size() - 1);
        assertTrue(status.contains("접속 대기"), status);
        assertTrue(status.contains(String.valueOf(side.network.getLocalPort())), status);
        assertTrue(status.contains(side.controller.getHostAddresses().get(0)), status);
    }

    @Test
    void join_connectsBothSidesAndShowsMessages() throws InterruptedException {
        connect();

        assertTrue(server.controller.isHost());
        assertFalse(client.controller.isHost());
        assertTrue(server.events.states.contains(State.CONNECTED));
        assertTrue(client.events.states.contains(State.CONNECTING));
        assertTrue(server.events.statuses.stream().anyMatch(s -> s.contains("상대가 접속")));
        assertTrue(client.events.statuses.stream().anyMatch(s -> s.contains("서버에 연결")));
    }

    @Test
    void join_savesServerAddressAsRecentHost() throws InterruptedException {
        connect();

        RecentHostStore.HostEntry latest = client.controller.getRecentHosts().get(0);
        assertEquals("127.0.0.1", latest.host());
        assertEquals(serverPort, latest.port());
        assertTrue(server.controller.getRecentHosts().isEmpty(), "서버는 접속 주소를 저장하지 않음");
    }

    @Test
    void join_failureShowsErrorAndReturnsToStart() throws Exception {
        Side side = new Side("client");
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        side.controller.join("127.0.0.1", closedPort);

        waitUntil(() -> side.controller.getState() == State.DISCONNECTED);
        assertFalse(side.events.errors.isEmpty());
        assertTrue(side.controller.getRecentHosts().isEmpty(), "실패한 주소는 저장하지 않음");
    }

    @Test
    void join_invalidArgumentsLeaveStateUnchanged() {
        Side side = new Side("client");

        assertThrows(NullPointerException.class, () -> side.controller.join(null, 7777));
        assertThrows(IllegalArgumentException.class, () -> side.controller.join(" ", 7777));
        assertEquals(State.DISCONNECTED, side.controller.getState());
    }

    @Test
    void host_portInUseShowsErrorAndStaysDisconnected() throws IOException {
        try (ServerSocket occupied = new ServerSocket(0)) {
            Side side = new Side("host");

            side.controller.host(occupied.getLocalPort());

            assertEquals(State.DISCONNECTED, side.controller.getState());
            assertEquals(1, side.events.errors.size());
        }
    }

    @Test
    void host_andJoin_requireDisconnectedState() throws InterruptedException {
        connect();

        assertThrows(IllegalStateException.class, () -> server.controller.host(0));
        assertThrows(IllegalStateException.class, () -> client.controller.join("127.0.0.1", 7777));
    }

    @Test
    void leave_whileHostingCancelsWaiting() {
        Side side = new Side("host");
        side.controller.host(0);

        side.controller.leave();

        assertEquals(State.DISCONNECTED, side.controller.getState());
        assertFalse(side.network.isOpening());
        assertTrue(side.events.errors.isEmpty());
        assertTrue(side.battle.calls.isEmpty());
    }

    @Test
    void lagStateIsForwardedToScreen() throws InterruptedException {
        connect();

        waitUntil(() -> !client.events.lagStates.isEmpty());
        assertFalse(client.events.lagStates.get(0), "로컬 연결은 랙 아님");
    }

    // ================================================================ 설정·준비

    @Test
    void sendBattleConfig_onlyHostSelectsModeAndClientReceivesIt() throws InterruptedException {
        connect();

        assertThrows(IllegalStateException.class, () -> client.controller.sendBattleConfig(NORMAL));
        server.controller.sendBattleConfig(TIME);

        waitUntil(() -> TIME.equals(client.controller.getConfig()));
        assertEquals(server.controller.getBattleId(), client.controller.getBattleId());
        assertTrue(client.events.configs.contains(TIME));
        assertTrue(server.events.configs.contains(TIME));
        assertTrue(client.events.statuses.stream().anyMatch(s -> s.contains("TIME_LIMIT 180초")));
    }

    @Test
    void sendBattleConfig_requiresConnection() {
        Side side = new Side("host");
        side.controller.host(0);

        assertThrows(IllegalStateException.class, () -> side.controller.sendBattleConfig(NORMAL));
        assertThrows(NullPointerException.class, () -> side.controller.sendBattleConfig((MessagePayload.Config) null));
    }

    @Test
    void setReady_requiresSelectedConfig() throws InterruptedException {
        connect();

        assertThrows(IllegalStateException.class, () -> client.controller.setReady(true));
        assertThrows(IllegalStateException.class, () -> server.controller.setReady(true));
    }

    @Test
    void setReady_isShownToPeer() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);

        client.controller.setReady(true);

        waitUntil(server.controller::isPeerReady);
        assertTrue(client.controller.isLocalReady());
        assertTrue(server.events.peerReady.contains(true));
        assertTrue(server.events.statuses.contains("상대 준비 완료"));
    }

    @Test
    void onlyOneSideReady_doesNotStart() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);

        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        Thread.sleep(FAST.startDelayMillis() * 2);

        assertEquals(State.CONNECTED, server.controller.getState());
        assertEquals(0, server.battle.count("start"));
    }

    @Test
    void cancelledReady_doesNotStart() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);

        client.controller.setReady(false);
        waitUntil(() -> !server.controller.isPeerReady());
        server.controller.setReady(true);
        Thread.sleep(FAST.startDelayMillis() * 2);

        assertEquals(State.CONNECTED, server.controller.getState());
        assertEquals(0, client.battle.count("start"));
    }

    @Test
    void configChange_resetsBothReadyStates() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);

        server.controller.sendBattleConfig(TIME);

        assertFalse(server.controller.isPeerReady());
        waitUntil(() -> TIME.equals(client.controller.getConfig()));
        assertFalse(client.controller.isLocalReady(), "설정이 바뀌면 다시 준비해야 함");
    }

    @Test
    void readyForOldConfig_isIgnored() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        long oldId = server.controller.getBattleId();
        server.controller.sendBattleConfig(TIME);

        server.controller.handleMessage(
                NetworkMessage.of(MessageType.READY, oldId, new MessagePayload.Ready(true)));

        assertFalse(server.controller.isPeerReady());
    }

    // ================================================================ 시작

    @Test
    void bothReady_startsBattleOnBothSidesWithSameConfig() throws InterruptedException {
        startBattle(TIME);

        assertEquals(TIME, server.battle.startedConfig);
        assertEquals(TIME, client.battle.startedConfig);
        assertTrue(server.battle.startedAsHost, "서버 = PLAYER1");
        assertFalse(client.battle.startedAsHost, "클라이언트 = PLAYER2");
        assertEquals(1, server.events.battleStarted);
        assertEquals(1, client.events.battleStarted);
        assertTrue(server.events.states.contains(State.STARTING));
        assertTrue(client.events.states.contains(State.STARTING));
        assertEquals(FAST.startDelayMillis(), server.events.startingDelays.get(0));
        assertTrue(client.events.startingDelays.get(0) <= FAST.startDelayMillis());
    }

    @Test
    void startMessageWithOtherBattleId_isIgnored() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);

        client.controller.handleMessage(NetworkMessage.of(MessageType.START,
                client.controller.getBattleId() + 1, new MessagePayload.Start(0)));

        assertEquals(State.CONNECTED, client.controller.getState());
    }

    @Test
    void leaveDuringCountdown_cancelsStart() throws InterruptedException {
        server = new Side("server");
        client = new Side("client");
        NetworkController slowServer = new NetworkController(new NetworkService(NET), server.battle, null,
                Runnable::run, new NetworkController.Timing(1000, 20));
        controllers.add(slowServer);
        NetworkService slowNetwork = getNetwork(slowServer);
        slowServer.host(0);
        client.controller.join("127.0.0.1", slowNetwork.getLocalPort());
        waitUntil(() -> slowServer.getState() == State.CONNECTED && client.controller.getState() == State.CONNECTED);
        slowServer.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        client.controller.setReady(true);
        waitUntil(slowServer::isPeerReady);
        slowServer.setReady(true);
        assertEquals(State.STARTING, slowServer.getState());

        slowServer.leave();
        Thread.sleep(1300);

        assertEquals(0, server.battle.count("start"), "카운트다운 중 나가면 시작하지 않음");
        assertEquals(0, client.battle.count("start"));
        assertEquals(State.DISCONNECTED, client.controller.getState());
    }

    // ================================================================ 대전 중

    @Test
    void attackFromLocalEngine_reachesOpponentEngine() throws InterruptedException {
        startBattle(NORMAL);
        MessagePayload.Attack attack = new MessagePayload.Attack(attackRows(3));

        client.battle.engineCreatesAttack(attack);

        waitUntil(() -> server.battle.receivedAttacks.size() == 1);
        assertEquals(attack, server.battle.receivedAttacks.get(0));
    }

    @Test
    void attacksInBothDirections_keepOrder() throws InterruptedException {
        startBattle(NORMAL);

        for (int rows = 1; rows <= 5; rows++) {
            server.battle.engineCreatesAttack(new MessagePayload.Attack(attackRows(rows)));
            client.battle.engineCreatesAttack(new MessagePayload.Attack(attackRows(rows)));
        }

        waitUntil(() -> server.battle.receivedAttacks.size() == 5 && client.battle.receivedAttacks.size() == 5);
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, server.battle.receivedAttacks.get(i).rowCount());
            assertEquals(i + 1, client.battle.receivedAttacks.get(i).rowCount());
        }
    }

    @Test
    void localSnapshot_isShownOnOpponentScreen() throws InterruptedException {
        startBattle(NORMAL);

        client.battle.localSnapshot = snapshot(120);

        waitUntil(() -> server.battle.remoteSnapshots.stream().anyMatch(s -> s.score() == 120));
        client.battle.localSnapshot = snapshot(250);
        waitUntil(() -> server.battle.remoteSnapshots.stream().anyMatch(s -> s.score() == 250));
        List<MessagePayload.Snapshot> received = server.battle.remoteSnapshots;
        assertEquals(250, received.get(received.size() - 1).score(), "최신 상태가 마지막");
    }

    @Test
    void snapshot_updatesWithinLatencyRequirement() throws InterruptedException {
        startBattle(NORMAL);
        waitUntil(() -> true);

        long sentAt = System.nanoTime();
        client.battle.localSnapshot = snapshot(999);
        waitUntil(() -> server.battle.remoteSnapshots.stream().anyMatch(s -> s.score() == 999));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sentAt);

        assertTrue(elapsedMillis < 200, "조작 후 상대 화면 반영까지 200ms 이하: " + elapsedMillis);
    }

    @Test
    void olderSnapshotSequence_isIgnored() throws InterruptedException {
        startBattle(NORMAL);
        client.battle.localSnapshot = null;   // 자동 전송 중지
        long id = server.controller.getBattleId();
        Thread.sleep(50);
        server.battle.remoteSnapshots.clear();

        server.controller.handleMessage(numbered(NetworkMessage.of(MessageType.SNAPSHOT, id, snapshot(2)), 1_000_000));
        server.controller.handleMessage(numbered(NetworkMessage.of(MessageType.SNAPSHOT, id, snapshot(1)), 999_999));

        assertEquals(1, server.battle.remoteSnapshots.size());
        assertEquals(2, server.battle.remoteSnapshots.get(0).score());
    }

    @Test
    void localGameOver_isSentToOpponent() throws InterruptedException {
        startBattle(NORMAL);

        client.battle.engineGameOver();

        waitUntil(() -> server.battle.count("notifyRemoteGameOver") == 1);
        assertEquals(0, client.battle.count("notifyRemoteGameOver"));
    }

    @Test
    void hostJudgesAndBothSidesShowSameResult() throws InterruptedException {
        startBattle(NORMAL);
        MessagePayload.Result result = new MessagePayload.Result("PLAYER1", false, "GAME_OVER");

        server.battle.result = result;   // 서버 BattleService가 결과 확정

        waitUntil(() -> client.events.results.size() == 1 && server.events.results.size() == 1);
        assertEquals(result, server.events.results.get(0));
        assertEquals(result, client.events.results.get(0));
        assertEquals(List.of(result), client.battle.appliedResults, "클라이언트는 서버 판정을 그대로 적용");
        assertTrue(server.battle.appliedResults.isEmpty());
        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
        assertEquals(1, server.battle.count("stop"));
        assertEquals(1, client.battle.count("stop"));
    }

    @Test
    void clientDoesNotJudgeByItself() throws InterruptedException {
        startBattle(NORMAL);

        client.battle.result = new MessagePayload.Result("PLAYER2", false, "GAME_OVER");
        Thread.sleep(200);

        assertEquals(State.IN_BATTLE, client.controller.getState());
        assertTrue(client.events.results.isEmpty());
        assertTrue(server.events.results.isEmpty());
    }

    @Test
    void afterResult_returnToWaitingAndStartNewBattleWithNewId() throws InterruptedException {
        startBattle(NORMAL);
        long firstId = server.controller.getBattleId();
        server.battle.result = new MessagePayload.Result(null, true, "TIME_UP");
        waitUntil(() -> client.controller.getState() == State.CONNECTED && !client.events.results.isEmpty());
        waitUntil(() -> client.controller.getBattleId() == server.controller.getBattleId());
        assertTrue(server.controller.getBattleId() > firstId);
        assertFalse(client.controller.isLocalReady());
        server.battle.result = null;

        readyBothAndWaitForBattle();

        assertEquals(2, server.battle.count("start"));
        assertEquals(2, client.battle.count("start"));
    }

    @Test
    void messagesFromPreviousBattle_areIgnored() throws InterruptedException {
        startBattle(NORMAL);
        long firstId = server.controller.getBattleId();
        server.battle.result = new MessagePayload.Result("PLAYER2", false, "GAME_OVER");
        waitUntil(() -> client.controller.getBattleId() > firstId);
        server.battle.result = null;
        readyBothAndWaitForBattle();

        server.controller.handleMessage(
                NetworkMessage.of(MessageType.ATTACK, firstId, new MessagePayload.Attack(attackRows(2))));
        server.controller.handleMessage(NetworkMessage.of(MessageType.GAME_OVER, firstId,
                new MessagePayload.GameOver(0, 0)));
        client.controller.handleMessage(NetworkMessage.of(MessageType.RESULT, firstId,
                new MessagePayload.Result("PLAYER1", false, "GAME_OVER")));

        assertTrue(server.battle.receivedAttacks.isEmpty());
        assertEquals(0, server.battle.count("notifyRemoteGameOver"));
        assertEquals(State.IN_BATTLE, client.controller.getState());
    }

    @Test
    void battleMessagesOutsideBattle_areIgnored() throws InterruptedException {
        connect();
        long id = server.controller.getBattleId();

        server.controller.handleMessage(
                NetworkMessage.of(MessageType.ATTACK, id, new MessagePayload.Attack(attackRows(1))));
        server.controller.handleMessage(NetworkMessage.of(MessageType.PAUSE, id));
        server.controller.handleMessage(NetworkMessage.of(MessageType.SNAPSHOT, id, snapshot(1)));
        server.controller.sendAttack(new MessagePayload.Attack(attackRows(1)));
        server.controller.sendGameOver();
        server.controller.sendSnapshot(snapshot(1));

        assertTrue(server.battle.calls.isEmpty());
        Thread.sleep(100);
        assertTrue(client.battle.calls.isEmpty());
    }

    @Test
    void pauseAndResume_areSynchronized() throws InterruptedException {
        startBattle(NORMAL);

        client.controller.requestPause();

        assertTrue(client.controller.isPaused());
        waitUntil(server.controller::isPaused);
        assertEquals(1, server.battle.count("pause"));
        assertEquals(1, client.battle.count("pause"));
        assertEquals(List.of(true), server.events.pauses);

        server.controller.requestResume();

        assertFalse(server.controller.isPaused());
        waitUntil(() -> !client.controller.isPaused());
        assertEquals(1, server.battle.count("resume"));
        assertEquals(1, client.battle.count("resume"));
    }

    @Test
    void pauseRequests_areIgnoredWhenNotApplicable() throws InterruptedException {
        connect();

        server.controller.requestPause();
        server.controller.requestResume();

        assertTrue(server.battle.calls.isEmpty());
        assertFalse(server.controller.isPaused());
    }

    @Test
    void doublePause_isSentOnce() throws InterruptedException {
        startBattle(NORMAL);

        client.controller.requestPause();
        client.controller.requestPause();
        waitUntil(server.controller::isPaused);
        Thread.sleep(100);

        assertEquals(1, server.battle.count("pause"));
    }

    @Test
    void sendBattleResult_onlyByHost() throws InterruptedException {
        connect();

        assertThrows(IllegalStateException.class, () -> client.controller.sendBattleResult(
                new MessagePayload.Result(null, true, "TIME_UP")));
    }

    // ================================================================ 재대전

    @Test
    void rematchAccepted_startsNewBattle() throws InterruptedException {
        startBattle(NORMAL);
        server.battle.result = new MessagePayload.Result("PLAYER1", false, "GAME_OVER");
        waitUntil(() -> client.controller.getState() == State.CONNECTED
                && client.controller.getBattleId() == server.controller.getBattleId());
        server.battle.result = null;

        client.controller.requestRematch();
        waitUntil(() -> server.events.rematchRequests == 1);
        server.controller.respondRematch(true);

        waitUntil(() -> client.events.rematchAnswers.contains(true));
        waitUntil(() -> server.controller.getState() == State.IN_BATTLE
                && client.controller.getState() == State.IN_BATTLE);
        assertEquals(2, client.battle.count("start"));
    }

    @Test
    void rematchDeclined_staysWaiting() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);

        server.controller.requestRematch();
        waitUntil(() -> client.events.rematchRequests == 1);
        client.controller.respondRematch(false);

        waitUntil(() -> server.events.rematchAnswers.contains(false));
        Thread.sleep(FAST.startDelayMillis() * 2);
        assertEquals(State.CONNECTED, server.controller.getState());
        assertFalse(server.controller.isLocalReady());
        assertTrue(server.events.statuses.stream().anyMatch(s -> s.contains("거절")));
    }

    @Test
    void unrequestedRematchResponse_isIgnored() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);

        server.controller.handleMessage(NetworkMessage.of(MessageType.REMATCH_RESPONSE,
                server.controller.getBattleId(), new MessagePayload.RematchResponse(true)));

        assertTrue(server.events.rematchAnswers.isEmpty());
        assertFalse(server.controller.isLocalReady());
    }

    @Test
    void rematch_requiresWaitingState() throws InterruptedException {
        startBattle(NORMAL);

        assertThrows(IllegalStateException.class, () -> client.controller.requestRematch());
        assertThrows(IllegalStateException.class, () -> client.controller.respondRematch(true));
    }

    // ================================================================ 나가기·끊김

    @Test
    void leaveDuringBattle_peerWinsByPlayerLeftWithoutDisconnectError() throws InterruptedException {
        startBattle(NORMAL);

        client.controller.leave();

        assertEquals(State.DISCONNECTED, client.controller.getState());
        assertEquals(1, client.battle.count("stop"));
        waitUntil(() -> !server.events.results.isEmpty());
        assertEquals(1, server.battle.count("notifyRemoteLeft"));
        assertEquals(0, server.battle.count("notifyConnectionLost"), "나가기를 끊김으로 중복 처리하지 않음");
        assertEquals(new MessagePayload.Result("PLAYER1", false, "PLAYER_LEFT"), server.events.results.get(0));
        waitUntil(() -> !server.events.closedByPeerLeft.isEmpty());
        assertEquals(List.of(true), server.events.closedByPeerLeft);
        Thread.sleep(200);
        assertTrue(server.events.errors.isEmpty(), "정상 나가기는 오류 아님: " + server.events.errors);
        assertTrue(client.events.closedByPeerLeft.isEmpty(), "나간 쪽은 종료 통지 없음");
    }

    @Test
    void hostLeaves_clientWinsAsPlayer2() throws InterruptedException {
        startBattle(NORMAL);

        server.controller.leave();

        waitUntil(() -> !client.events.results.isEmpty());
        assertEquals(new MessagePayload.Result("PLAYER2", false, "PLAYER_LEFT"), client.events.results.get(0));
    }

    @Test
    void peerLeftUsesBattleResultWhenAvailable() throws InterruptedException {
        startBattle(NORMAL);
        // notifyRemoteLeft 후 BattleService가 확정한 결과를 그대로 사용 (점수 등 정보가 다를 수 있음)
        MessagePayload.Result fromBattle = new MessagePayload.Result("PLAYER1", false, "PLAYER_LEFT");
        server.battle.resultAfterRemoteLeft = fromBattle;

        client.controller.leave();

        waitUntil(() -> !server.events.results.isEmpty());
        assertSame(fromBattle, server.events.results.get(0));
    }

    @Test
    void leaveInLobby_closesPeerWithoutBattleCalls() throws InterruptedException {
        connect();

        server.controller.leave();

        waitUntil(() -> !client.events.closedByPeerLeft.isEmpty());
        assertEquals(List.of(true), client.events.closedByPeerLeft);
        assertTrue(client.events.results.isEmpty());
        assertTrue(client.battle.calls.isEmpty());
        assertTrue(server.battle.calls.isEmpty());
    }

    @Test
    void connectionLostDuringBattle_endsWithoutWinnerAndShowsError() throws InterruptedException {
        startBattle(NORMAL);

        server.network.disconnect();   // LEAVE 없이 연결만 끊김

        waitUntil(() -> !client.events.closedByPeerLeft.isEmpty());
        assertEquals(State.DISCONNECTED, client.controller.getState());
        assertEquals(1, client.battle.count("notifyConnectionLost"));
        assertEquals(1, client.battle.count("stop"));
        assertEquals(0, client.battle.count("notifyRemoteLeft"));
        assertEquals(List.of(false), client.events.closedByPeerLeft);
        assertFalse(client.events.errors.isEmpty(), "연결 끊김은 오류 메시지 표시");
    }

    @Test
    void connectionLostInLobby_returnsToStartScreen() throws InterruptedException {
        connect();

        client.network.disconnect();

        waitUntil(() -> !server.events.closedByPeerLeft.isEmpty());
        assertEquals(State.DISCONNECTED, server.controller.getState());
        assertEquals(List.of(false), server.events.closedByPeerLeft);
        assertTrue(server.battle.calls.isEmpty());
    }

    @Test
    void canHostAgainAfterDisconnect() throws InterruptedException {
        connect();
        client.controller.leave();
        waitUntil(() -> server.controller.getState() == State.DISCONNECTED);

        server.controller.host(0);
        client.controller.join("127.0.0.1", server.network.getLocalPort());

        waitUntil(() -> server.controller.getState() == State.CONNECTED
                && client.controller.getState() == State.CONNECTED);
        assertNull(server.controller.getConfig(), "새 연결은 설정부터 다시");
    }

    @Test
    void leave_whenDisconnectedDoesNothing() {
        Side side = new Side("idle");

        side.controller.leave();

        assertEquals(State.DISCONNECTED, side.controller.getState());
        assertTrue(side.events.statuses.isEmpty());
    }

    // ================================================================ 기타

    @Test
    void constructor_validatesArguments() {
        NetworkService network = new NetworkService(NET);
        FakeBattleGateway battle = new FakeBattleGateway();

        assertThrows(NullPointerException.class,
                () -> new NetworkController(null, battle, null, Runnable::run, FAST));
        assertThrows(NullPointerException.class,
                () -> new NetworkController(network, null, null, Runnable::run, FAST));
        assertThrows(NullPointerException.class,
                () -> new NetworkController(network, battle, null, null, FAST));
        assertThrows(NullPointerException.class,
                () -> new NetworkController(network, battle, null, Runnable::run, null));
        network.shutdown();
    }

    @Test
    void timing_validatesValues() {
        assertThrows(IllegalArgumentException.class, () -> new NetworkController.Timing(-1, 50));
        assertThrows(IllegalArgumentException.class, () -> new NetworkController.Timing(0, 0));
        assertTrue(NetworkController.Timing.DEFAULT.snapshotIntervalMillis() < 200, "지연 200ms 이하 목표");
    }

    @Test
    void handleMessage_rejectsNull() {
        Side side = new Side("idle");

        assertThrows(NullPointerException.class, () -> side.controller.handleMessage(null));
    }

    @Test
    void setEvents_nullIsAllowed() {
        Side side = new Side("idle");

        side.controller.setEvents(null);

        assertDoesNotThrow(() -> side.controller.host(0));
    }

    @Test
    void getRecentHosts_withoutStoreIsEmpty() {
        NetworkController controller = new NetworkController(new NetworkService(NET), new FakeBattleGateway(),
                null, Runnable::run, FAST);
        controllers.add(controller);

        assertTrue(controller.getRecentHosts().isEmpty());
    }

    // ================================================================ 명세 시그니처 (2번 도메인 타입)

    @Test
    void sendBattleConfig_withBattleConfigDeliversSameSettings() throws InterruptedException {
        connect();

        server.controller.sendBattleConfig(BattleConfig.timeLimit(Difficulty.EASY, 600));

        waitUntil(() -> client.controller.getConfig() != null);
        assertEquals(new MessagePayload.Config("TIME_LIMIT", Difficulty.EASY, 600), client.controller.getConfig());
        assertThrows(NullPointerException.class, () -> server.controller.sendBattleConfig((BattleConfig) null));
    }

    @Test
    void sendAttack_withAttackRowsSkipsEmptyAttack() throws InterruptedException {
        startBattle(NORMAL);

        client.controller.sendAttack(AttackRows.empty());
        client.controller.sendAttack(new AttackRows(attackRows(2)));

        waitUntil(() -> server.battle.receivedAttacks.size() == 1);
        Thread.sleep(100);
        assertEquals(1, server.battle.receivedAttacks.size(), "빈 공격은 보내지 않음");
        assertEquals(2, server.battle.receivedAttacks.get(0).rowCount());
    }

    @Test
    void sendSnapshot_withGameSnapshotIsShownToOpponent() throws InterruptedException {
        startBattle(NORMAL);
        client.battle.localSnapshot = null;   // 자동 전송 대신 직접 전송
        GameSnapshot snapshot = new GameSnapshot(new BlockType[Board.ROWS][Board.COLS],
                new boolean[Board.ROWS][Board.COLS], null, null, 777, 2, 5, null, false);

        client.controller.sendSnapshot(snapshot);

        waitUntil(() -> server.battle.remoteSnapshots.stream().anyMatch(s -> s.score() == 777));
        assertThrows(NullPointerException.class, () -> client.controller.sendSnapshot((GameSnapshot) null));
    }

    @Test
    void sendBattleResult_withBattleResultIsAppliedByClient() throws InterruptedException {
        startBattle(NORMAL);

        server.controller.sendBattleResult(BattleResult.draw(BattleEndReason.TIME_UP));

        waitUntil(() -> client.battle.appliedResults.size() == 1);
        assertEquals(new MessagePayload.Result(null, true, "TIME_UP"), client.battle.appliedResults.get(0));
        assertThrows(NullPointerException.class, () -> server.controller.sendBattleResult((BattleResult) null));
        assertThrows(IllegalStateException.class, () -> client.controller.sendBattleResult(
                BattleResult.win(PlayerId.PLAYER2, BattleEndReason.GAME_OVER)));
    }

    // ================================================================ 오류 상황

    @Test
    void snapshotErrorIsShownAndSyncKeepsRunning() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        server.battle.failSnapshot = true;   // 엔진 상태 조회 중 예외
        readyBothAndWaitForBattle();

        waitUntil(() -> server.events.errors.stream().anyMatch(e -> e.contains("동기화")));
        server.battle.failSnapshot = false;
        server.battle.localSnapshot = snapshot(31);
        waitUntil(() -> client.battle.remoteSnapshots.stream().anyMatch(s -> s.score() == 31));
        assertEquals(State.IN_BATTLE, server.controller.getState(), "예외 후에도 대전 유지");
    }

    @Test
    void battleStartFailure_returnsToWaitingWithError() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        client.battle.failStart = true;

        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);
        server.controller.setReady(true);

        waitUntil(() -> client.events.errors.stream().anyMatch(e -> e.contains("시작할 수 없습니다")));
        waitUntil(() -> client.controller.getState() == State.CONNECTED);
        assertFalse(client.controller.isLocalReady(), "다시 준비해야 함");
        assertNull(client.battle.attackListener, "시작 실패 시 콜백 해제");
    }

    // ================================================================ 접속 과정 메시지

    @Test
    void connectionProgressMessagesAreShownInOrder() throws InterruptedException {
        connect();
        server.controller.sendBattleConfig(NORMAL);
        waitUntil(() -> client.controller.getConfig() != null);
        client.controller.setReady(true);
        waitUntil(server.controller::isPeerReady);

        assertOrder(server.events.statuses, "접속 대기", "상대가 접속", "대전 설정", "상대 준비 완료");
        assertOrder(client.events.statuses, "연결 중", "서버에 연결", "대전 설정", "상대 준비를 기다리는 중");
        assertOrder(client.events.states, State.CONNECTING, State.CONNECTED);
        assertOrder(server.events.states, State.HOSTING, State.CONNECTED);
    }

    // ================================================================ 앱 연결 (JavaFX)

    @Test
    void appConstructor_usesBattleServiceAndNotifiesOnJavaFxThread() throws Exception {
        FxTestSupport.startJavaFx();
        NetworkController app = new NetworkController(new NetworkService(NET), new BattleService(), null);
        controllers.add(app);
        List<Boolean> onFxThread = new CopyOnWriteArrayList<>();
        app.setEvents(new NetworkEvents() {
            @Override
            public void showConnectionStatus(String status) {
                onFxThread.add(Platform.isFxApplicationThread());
            }
        });

        app.host(0);

        waitUntil(() -> !onFxThread.isEmpty());
        assertTrue(onFxThread.stream().allMatch(Boolean::booleanValue), "화면 통지는 JavaFX Application Thread에서");
        assertEquals(State.HOSTING, app.getState());
    }

    // ================================================================ 도우미

    private NetworkService getNetwork(NetworkController controller) {
        try {
            var field = NetworkController.class.getDeclaredField("network");
            field.setAccessible(true);
            return (NetworkService) field.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** 순서 번호를 붙인 메시지 (실제 수신과 같은 형태). 번호는 인코딩 바이트의 2~9번째 */
    private static NetworkMessage numbered(NetworkMessage message, long sequence) {
        MessageCodec codec = new MessageCodec();
        byte[] data = codec.encode(message);
        ByteBuffer.wrap(data).putLong(2, sequence);
        return codec.decode(data);
    }

    private static BlockType[][] attackRows(int count) {
        BlockType[][] rows = new BlockType[count][Board.COLS];
        for (int row = 0; row < count; row++) {
            for (int col = 1; col < Board.COLS; col++) {
                rows[row][col] = BlockType.Z;
            }
        }
        return rows;
    }

    private static MessagePayload.Snapshot snapshot(int score) {
        return new MessagePayload.Snapshot(new BlockType[Board.ROWS][Board.COLS],
                new boolean[Board.ROWS][Board.COLS], null, null, score, 1, 7,
                new BlockType[0][Board.COLS], false);
    }

    /** expected 항목들이 이 순서대로 나타나는지 (문자열은 포함 여부로 비교) */
    private static void assertOrder(List<?> actual, Object... expected) {
        int index = 0;
        for (Object item : actual) {
            if (index < expected.length && matches(item, expected[index])) {
                index++;
            }
        }
        assertEquals(expected.length, index, "순서대로 나타나야 함: " + List.of(expected) + " / 실제 " + actual);
    }

    private static boolean matches(Object item, Object expected) {
        return expected instanceof String text ? String.valueOf(item).contains(text) : expected.equals(item);
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
