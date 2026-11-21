package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * [요구사항 테스트] P2P 네트워크 접속 과정 자동화 테스트.
 * "네트워크 접속 과정이 잘 진행되는지 확인할 수 있는 자동화된 테스트를 작성합니다." 에 해당한다.
 *
 * 실제 TCP 소켓(127.0.0.1)으로 서버·클라이언트를 연결해 다음을 확인한다.
 *  - 접속 대기 → 접속 → 양쪽 연결 완료 통지
 *  - 양방향 송수신, 이벤트 순서 보장, 스냅샷 최신 상태 우선
 *  - 정상 종료(나가기)와 예상하지 못한 끊김 구분
 *  - 접속 실패·포트 사용 중·잘못된 데이터 오류 처리
 *  - 하트비트로 지연(랙) 측정, 응답 없음 → 끊김 처리
 *  - 끊긴 뒤 다시 접속
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class NetworkServiceTest {

    // 테스트가 빨리 끝나도록 짧게 잡은 기준 (PING 50ms, 랙 150ms, 끊김 800ms)
    private static final NetworkService.Timing FAST = new NetworkService.Timing(1000, 50, 150, 800);
    private static final String LOCALHOST = "127.0.0.1";

    private final List<NetworkService> services = new ArrayList<>();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final MessageCodec codec = new MessageCodec();

    @AfterEach
    void cleanUp() throws Exception {
        for (NetworkService service : services) {
            service.shutdown();
        }
        for (AutoCloseable resource : resources) {
            resource.close();
        }
    }

    private NetworkService newService(RecordingListener listener) {
        NetworkService service = new NetworkService(FAST);
        service.setListener(listener);
        services.add(service);
        return service;
    }

    /** 서버·클라이언트 한 쌍을 연결된 상태로 준비 */
    private final class Pair {
        final RecordingListener hostEvents = new RecordingListener();
        final RecordingListener clientEvents = new RecordingListener();
        final NetworkService host = newService(hostEvents);
        final NetworkService client = newService(clientEvents);

        Pair() throws InterruptedException {
            host.openHost(0);
            client.connect(LOCALHOST, host.getLocalPort());
            hostEvents.awaitConnected();
            clientEvents.awaitConnected();
        }
    }

    // ------------------------------------------------------------ 접속 과정

    @Test
    void openHost_waitsForClientOnLocalPort() {
        RecordingListener events = new RecordingListener();
        NetworkService host = newService(events);

        host.openHost(0);

        assertTrue(host.isOpening());
        assertFalse(host.isConnected());
        assertTrue(host.getLocalPort() > 0);
        assertEquals(1, events.connected.getCount(), "아직 접속한 상대 없음");
    }

    @Test
    void connect_notifiesBothSidesAndMarksConnected() throws InterruptedException {
        Pair pair = new Pair();

        assertTrue(pair.host.isConnected());
        assertTrue(pair.client.isConnected());
        assertFalse(pair.host.isOpening());
        assertFalse(pair.client.isOpening());
        assertEquals(1, pair.hostEvents.connectedCount.get());
        assertEquals(1, pair.clientEvents.connectedCount.get());
        assertEquals(LOCALHOST, pair.host.getRemoteAddress());
        assertEquals(LOCALHOST, pair.client.getRemoteAddress());
        assertEquals(-1, pair.host.getLocalPort(), "접속을 받으면 대기 포트를 닫음");
    }

    @Test
    void connect_acceptsHostNameWithSpaces() throws InterruptedException {
        RecordingListener hostEvents = new RecordingListener();
        RecordingListener clientEvents = new RecordingListener();
        NetworkService host = newService(hostEvents);
        NetworkService client = newService(clientEvents);
        host.openHost(0);

        client.connect("  localhost ", host.getLocalPort());

        clientEvents.awaitConnected();
        hostEvents.awaitConnected();
    }

    @Test
    void connect_refusedReportsErrorAndAllowsRetry() throws Exception {
        int closedPort = findFreePort();
        RecordingListener events = new RecordingListener();
        NetworkService client = newService(events);

        client.connect(LOCALHOST, closedPort);

        assertTrue(events.nextError().contains(LOCALHOST + ":" + closedPort));
        waitUntil(() -> !client.isOpening());
        assertFalse(client.isConnected());
        assertEquals(1, events.connected.getCount());
        assertEquals(0, events.disconnectedCount.get(), "접속 실패는 끊김이 아니라 오류");

        // 실패 후 다시 시도 가능
        RecordingListener hostEvents = new RecordingListener();
        NetworkService host = newService(hostEvents);
        host.openHost(0);
        client.connect(LOCALHOST, host.getLocalPort());
        events.awaitConnected();
    }

    @Test
    void connect_unknownHostReportsError() throws InterruptedException {
        RecordingListener events = new RecordingListener();
        NetworkService client = newService(events);

        client.connect("no-such-host.invalid", 7777);

        assertNotNull(events.nextError());
        waitUntil(() -> !client.isOpening());
    }

    @Test
    void openHost_portInUseReportsError() throws Exception {
        ServerSocket occupied = new ServerSocket(0);
        resources.add(occupied);
        RecordingListener events = new RecordingListener();
        NetworkService host = newService(events);

        host.openHost(occupied.getLocalPort());

        assertTrue(events.nextError().contains(String.valueOf(occupied.getLocalPort())));
        assertFalse(host.isOpening());
        assertEquals(-1, host.getLocalPort());
    }

    @Test
    void openHost_cannotStartTwice() throws InterruptedException {
        RecordingListener events = new RecordingListener();
        NetworkService host = newService(events);
        host.openHost(0);

        assertThrows(IllegalStateException.class, () -> host.openHost(0));
        assertThrows(IllegalStateException.class, () -> host.connect(LOCALHOST, 7777));
    }

    @Test
    void connect_cannotStartWhileConnected() throws InterruptedException {
        Pair pair = new Pair();

        assertThrows(IllegalStateException.class, () -> pair.client.connect(LOCALHOST, 7777));
        assertThrows(IllegalStateException.class, () -> pair.host.openHost(0));
    }

    @Test
    void disconnect_cancelsHostingSilentlyAndFreesPort() throws InterruptedException {
        RecordingListener events = new RecordingListener();
        NetworkService host = newService(events);
        host.openHost(0);
        int port = host.getLocalPort();

        host.disconnect();

        assertFalse(host.isOpening());
        assertEquals(-1, host.getLocalPort());
        Thread.sleep(100);
        assertTrue(events.errors.isEmpty(), "직접 취소한 대기는 오류가 아님: " + events.errors);
        assertEquals(0, events.disconnectedCount.get());

        // 같은 포트로 다시 대기 가능
        host.openHost(port);
        assertEquals(port, host.getLocalPort());
    }

    @Test
    void disconnect_cancelsPendingConnectSilently() throws InterruptedException {
        RecordingListener events = new RecordingListener();
        NetworkService client = newService(events);

        client.connect("10.255.255.1", 7777);   // 응답하지 않는 주소
        client.disconnect();

        Thread.sleep(FAST.connectTimeoutMillis() + 300);
        assertTrue(events.errors.isEmpty(), "취소한 접속 시도는 오류가 아님: " + events.errors);
        assertEquals(1, events.connected.getCount());
        assertFalse(client.isOpening());
    }

    @Test
    void connect_validatesArguments() {
        NetworkService client = newService(new RecordingListener());

        assertThrows(NullPointerException.class, () -> client.connect(null, 7777));
        assertThrows(IllegalArgumentException.class, () -> client.connect(" ", 7777));
        assertThrows(IllegalArgumentException.class, () -> client.connect(LOCALHOST, 0));
        assertThrows(IllegalArgumentException.class, () -> client.connect(LOCALHOST, 65536));
        assertThrows(IllegalArgumentException.class, () -> client.openHost(-1));
        assertFalse(client.isOpening());
    }

    // ------------------------------------------------------------ 송수신

    @Test
    void send_deliversBothDirectionsWithSequenceAndBattleId() throws InterruptedException {
        Pair pair = new Pair();

        pair.client.send(NetworkMessage.of(MessageType.READY, 4, new MessagePayload.Ready(true)));
        pair.client.send(NetworkTestData.attack(4, 2));
        pair.host.send(NetworkMessage.of(MessageType.START, 4, new MessagePayload.Start(1000)));

        NetworkMessage ready = pair.hostEvents.nextMessage();
        NetworkMessage attack = pair.hostEvents.nextMessage();
        NetworkMessage start = pair.clientEvents.nextMessage();

        assertEquals(MessageType.READY, ready.getType());
        assertEquals(4, ready.getBattleId());
        assertEquals(MessageType.ATTACK, attack.getType());
        assertEquals(NetworkTestData.attack(4, 2).getPayload(), attack.getPayload());
        assertTrue(attack.getSequence() > ready.getSequence(), "보낸 순서대로 번호 증가");
        assertEquals(MessageType.START, start.getType());
        assertTrue(start.getSequence() >= 1);
    }

    @Test
    void send_keepsEveryEventInOrder() throws InterruptedException {
        Pair pair = new Pair();
        int count = 500;

        for (int i = 0; i < count; i++) {
            pair.client.send(NetworkMessage.of(MessageType.GAME_OVER, 1, new MessagePayload.GameOver(i, i)));
        }

        long lastSequence = 0;
        for (int i = 0; i < count; i++) {
            NetworkMessage message = pair.hostEvents.nextMessage();
            assertEquals(i, message.getPayload(MessagePayload.GameOver.class).score(), "빠짐·순서 바뀜 없음");
            assertTrue(message.getSequence() > lastSequence);
            lastSequence = message.getSequence();
        }
    }

    @Test
    void send_snapshotsKeepLatestStateAndNeverGoBackwards() throws InterruptedException {
        Pair pair = new Pair();
        int count = 300;

        for (int i = 1; i <= count; i++) {
            pair.client.send(NetworkTestData.snapshotMessage(1, i));
        }
        pair.client.send(NetworkMessage.of(MessageType.PAUSE, 1));

        int lastScore = 0;
        int received = 0;
        boolean sawPause = false;
        while (lastScore < count || !sawPause) {
            NetworkMessage message = pair.hostEvents.nextMessage();
            if (message.getType() == MessageType.PAUSE) {
                sawPause = true;
                continue;
            }
            int score = message.getPayload(MessagePayload.Snapshot.class).score();
            assertTrue(score > lastScore, "오래된 상태가 최신 상태를 덮어쓰지 않음");
            lastScore = score;
            received++;
        }
        assertEquals(count, lastScore, "마지막(최신) 상태는 반드시 도착");
        assertTrue(received <= count);
    }

    @Test
    void send_eventsAreNotDroppedBetweenSnapshots() throws InterruptedException {
        Pair pair = new Pair();

        for (int i = 0; i < 100; i++) {
            pair.client.send(NetworkTestData.snapshotMessage(1, i));
            if (i % 10 == 0) {
                pair.client.send(NetworkMessage.of(MessageType.GAME_OVER, 1, new MessagePayload.GameOver(0, i)));
            }
        }

        List<Integer> events = new ArrayList<>();
        while (events.size() < 10) {
            NetworkMessage message = pair.hostEvents.nextMessage();
            if (message.getType() == MessageType.GAME_OVER) {
                events.add(message.getPayload(MessagePayload.GameOver.class).score());
            }
        }
        assertEquals(List.of(0, 10, 20, 30, 40, 50, 60, 70, 80, 90), events);
    }

    @Test
    void onMessage_isCalledFromSingleReaderThread() throws InterruptedException {
        Pair pair = new Pair();

        for (int i = 0; i < 20; i++) {
            pair.client.send(NetworkMessage.of(MessageType.RESUME, 1));
        }
        for (int i = 0; i < 20; i++) {
            pair.hostEvents.nextMessage();
        }

        synchronized (pair.hostEvents.threadNames) {
            assertTrue(pair.hostEvents.threadNames.stream().allMatch("p2p-reader"::equals),
                    pair.hostEvents.threadNames.toString());
        }
    }

    @Test
    void send_heartbeatMessagesAreNotDeliveredToListener() throws InterruptedException {
        Pair pair = new Pair();

        Thread.sleep(FAST.heartbeatIntervalMillis() * 5);

        assertTrue(pair.hostEvents.messages.isEmpty(), "PING·PONG은 리스너로 오지 않음");
        assertTrue(pair.clientEvents.messages.isEmpty());
    }

    @Test
    void send_rejectsHeartbeatTypes() throws InterruptedException {
        Pair pair = new Pair();

        assertThrows(IllegalArgumentException.class,
                () -> pair.client.send(NetworkMessage.of(MessageType.PING, 0, new MessagePayload.Ping(1))));
        assertThrows(NullPointerException.class, () -> pair.client.send(null));
    }

    @Test
    void send_whileDisconnectedIsIgnored() {
        NetworkService service = newService(new RecordingListener());

        assertDoesNotThrow(() -> service.send(NetworkMessage.of(MessageType.LEAVE, 1)));
    }

    @Test
    void onMessage_exceptionIsReportedAndLaterMessagesStillArrive() throws InterruptedException {
        RecordingListener hostEvents = new RecordingListener() {
            @Override
            public void onMessage(NetworkMessage message) {
                if (message.getType() == MessageType.PAUSE) {
                    throw new IllegalStateException("boom");
                }
                super.onMessage(message);
            }
        };
        RecordingListener clientEvents = new RecordingListener();
        NetworkService host = newService(hostEvents);
        NetworkService client = newService(clientEvents);
        host.openHost(0);
        client.connect(LOCALHOST, host.getLocalPort());
        hostEvents.awaitConnected();

        client.send(NetworkMessage.of(MessageType.PAUSE, 1));
        client.send(NetworkMessage.of(MessageType.RESUME, 1));

        assertTrue(hostEvents.nextError().contains("PAUSE"));
        assertEquals(MessageType.RESUME, hostEvents.nextMessage().getType());
        assertTrue(host.isConnected());
    }

    @Test
    void messageSentInsideOnConnectedIsDelivered() throws InterruptedException {
        RecordingListener hostEvents = new RecordingListener();
        NetworkService[] clientRef = new NetworkService[1];
        RecordingListener clientEvents = new RecordingListener() {
            @Override
            public void onConnected() {
                clientRef[0].send(NetworkMessage.of(MessageType.READY, 1, new MessagePayload.Ready(false)));
                super.onConnected();
            }
        };
        NetworkService host = newService(hostEvents);
        clientRef[0] = newService(clientEvents);
        host.openHost(0);

        clientRef[0].connect(LOCALHOST, host.getLocalPort());

        assertEquals(MessageType.READY, hostEvents.nextMessage().getType());
    }

    // ------------------------------------------------------------ 종료·끊김

    @Test
    void disconnect_notifiesOnlyThePeer() throws InterruptedException {
        Pair pair = new Pair();

        pair.client.disconnect();

        pair.hostEvents.awaitDisconnected();
        assertFalse(pair.client.isConnected());
        waitUntil(() -> !pair.host.isConnected());
        Thread.sleep(100);
        assertEquals(0, pair.clientEvents.disconnectedCount.get(), "직접 끊은 쪽은 끊김 통지 없음");
        assertEquals(1, pair.hostEvents.disconnectedCount.get(), "끊김 통지는 한 번만");
    }

    @Test
    void disconnect_byHostNotifiesClient() throws InterruptedException {
        Pair pair = new Pair();

        pair.host.disconnect();

        pair.clientEvents.awaitDisconnected();
        assertFalse(pair.client.isConnected());
    }

    @Test
    void disconnect_sendsQueuedLeaveBeforeClosing() throws InterruptedException {
        Pair pair = new Pair();

        pair.client.send(NetworkTestData.attack(1, 1));
        pair.client.send(NetworkMessage.of(MessageType.LEAVE, 1));
        pair.client.disconnect();

        assertEquals(MessageType.ATTACK, pair.hostEvents.nextMessage().getType());
        assertEquals(MessageType.LEAVE, pair.hostEvents.nextMessage().getType(), "나가기 메시지가 끊김보다 먼저 도착");
        pair.hostEvents.awaitDisconnected();
    }

    @Test
    void disconnect_stopsDeliveringMessagesAndIsIdempotent() throws InterruptedException {
        Pair pair = new Pair();

        pair.host.disconnect();
        pair.host.disconnect();
        pair.client.send(NetworkMessage.of(MessageType.PAUSE, 1));

        Thread.sleep(200);
        assertTrue(pair.hostEvents.messages.isEmpty(), "끊은 뒤에는 받은 메시지를 전달하지 않음");
        assertDoesNotThrow(() -> pair.host.send(NetworkMessage.of(MessageType.PAUSE, 1)));
    }

    @Test
    void reconnect_afterDisconnectStartsFreshSession() throws InterruptedException {
        Pair pair = new Pair();
        pair.client.send(NetworkMessage.of(MessageType.PAUSE, 1));
        pair.hostEvents.nextMessage();
        pair.client.disconnect();
        pair.hostEvents.awaitDisconnected();

        RecordingListener hostEvents = new RecordingListener();
        RecordingListener clientEvents = new RecordingListener();
        pair.host.setListener(hostEvents);
        pair.client.setListener(clientEvents);
        pair.host.openHost(0);
        pair.client.connect(LOCALHOST, pair.host.getLocalPort());
        hostEvents.awaitConnected();
        clientEvents.awaitConnected();

        pair.client.send(NetworkMessage.of(MessageType.RESUME, 2));
        NetworkMessage message = hostEvents.nextMessage();
        assertEquals(MessageType.RESUME, message.getType());
        assertEquals(2, message.getBattleId());
    }

    @Test
    void peerSocketClosedAbruptlyIsReportedAsDisconnect() throws Exception {
        try (ServerSocket rawServer = new ServerSocket(0)) {
            RecordingListener events = new RecordingListener();
            NetworkService client = newService(events);
            client.connect(LOCALHOST, rawServer.getLocalPort());
            Socket peer = rawServer.accept();
            events.awaitConnected();

            peer.close();

            events.awaitDisconnected();
            assertFalse(client.isConnected());
        }
    }

    @Test
    void silentPeerTimesOutAsDisconnect() throws Exception {
        try (ServerSocket rawServer = new ServerSocket(0)) {
            RecordingListener events = new RecordingListener();
            NetworkService client = newService(events);
            client.connect(LOCALHOST, rawServer.getLocalPort());
            Socket peer = rawServer.accept();   // 받기만 하고 아무 응답도 하지 않는 상대
            resources.add(peer);
            events.awaitConnected();

            assertTrue(events.awaitLagState(true), "응답이 끊기면 먼저 랙 상태로 표시");
            events.awaitDisconnected();
            assertTrue(events.nextError().contains("응답"));
            assertFalse(client.isConnected());
        }
    }

    @Test
    void malformedDataIsReportedAndConnectionClosed() throws Exception {
        try (ServerSocket rawServer = new ServerSocket(0)) {
            RecordingListener events = new RecordingListener();
            NetworkService client = newService(events);
            client.connect(LOCALHOST, rawServer.getLocalPort());
            Socket peer = rawServer.accept();
            resources.add(peer);
            events.awaitConnected();

            DataOutputStream out = new DataOutputStream(peer.getOutputStream());
            MessageCodec.writeFrame(out, new byte[] {99, 0, 0});

            assertTrue(events.nextError().contains("잘못된 데이터"));
            events.awaitDisconnected();
        }
    }

    @Test
    void oversizedFrameClosesConnection() throws Exception {
        try (ServerSocket rawServer = new ServerSocket(0)) {
            RecordingListener events = new RecordingListener();
            NetworkService client = newService(events);
            client.connect(LOCALHOST, rawServer.getLocalPort());
            Socket peer = rawServer.accept();
            resources.add(peer);
            events.awaitConnected();

            DataOutputStream out = new DataOutputStream(peer.getOutputStream());
            out.writeInt(MessageCodec.MAX_FRAME_BYTES + 1);
            out.flush();

            events.awaitDisconnected();
        }
    }

    // ------------------------------------------------------------ 지연(랙) 측정

    @Test
    void heartbeat_measuresRoundTripOnFastNetwork() throws InterruptedException {
        Pair pair = new Pair();

        Long rtt = pair.clientEvents.latencies.poll(RecordingListener.WAIT_SECONDS, TimeUnit.SECONDS);

        assertNotNull(rtt);
        assertTrue(rtt >= 0 && rtt < FAST.lagThresholdMillis(), "로컬 연결 왕복 지연: " + rtt);
        assertTrue(pair.client.getRoundTripMillis() >= 0);
        assertFalse(pair.client.isLagging());
        assertFalse(pair.clientEvents.lagStates.contains(true));
    }

    @Test
    void heartbeat_keepsIdleConnectionAlive() throws InterruptedException {
        Pair pair = new Pair();

        Thread.sleep(FAST.disconnectTimeoutMillis() * 2);

        assertTrue(pair.host.isConnected(), "메시지가 없어도 하트비트로 연결 유지");
        assertTrue(pair.client.isConnected());
        assertEquals(0, pair.hostEvents.disconnectedCount.get());
    }

    @Test
    void heartbeat_detectsLagWhenPeerRespondsSlowly() throws Exception {
        try (ServerSocket rawServer = new ServerSocket(0)) {
            RecordingListener events = new RecordingListener();
            NetworkService client = newService(events);
            client.connect(LOCALHOST, rawServer.getLocalPort());
            Socket peer = rawServer.accept();
            resources.add(peer);
            events.awaitConnected();

            // PING에 300ms 늦게 PONG을 보내는 느린 상대
            Thread slowPeer = new Thread(() -> echoPongs(peer, 300));
            slowPeer.setDaemon(true);
            slowPeer.start();

            assertTrue(events.awaitLagState(true), "왕복 지연이 기준(150ms)을 넘으면 랙");
            waitUntil(() -> client.getRoundTripMillis() > FAST.lagThresholdMillis());
            assertTrue(client.isLagging());
            assertTrue(client.isConnected(), "느리기만 하면 끊지 않음");
        }
    }

    @Test
    void getRoundTripMillisAndLaggingWithoutConnection() {
        NetworkService service = newService(new RecordingListener());

        assertEquals(-1, service.getRoundTripMillis());
        assertFalse(service.isLagging());
        assertFalse(service.isConnected());
        assertNull(service.getRemoteAddress());
        assertEquals(-1, service.getLocalPort());
    }

    // ------------------------------------------------------------ 기타

    @Test
    void setListener_nullIsAllowed() throws InterruptedException {
        RecordingListener hostEvents = new RecordingListener();
        NetworkService host = newService(hostEvents);
        NetworkService client = newService(null);
        client.setListener(null);
        host.openHost(0);

        client.connect(LOCALHOST, host.getLocalPort());

        hostEvents.awaitConnected();
        waitUntil(client::isConnected);
    }

    @Test
    void getLocalAddresses_returnsIpv4Addresses() throws Exception {
        List<String> addresses = NetworkService.getLocalAddresses();

        assertFalse(addresses.isEmpty());
        for (String address : addresses) {
            assertTrue(address.matches("\\d{1,3}(\\.\\d{1,3}){3}"), address);
            assertNotNull(InetAddress.getByName(address));
        }
    }

    @Test
    void timing_validatesValues() {
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(0, 50, 150, 800));
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(1000, 0, 150, 800));
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(1000, 50, 0, 800));
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(1000, 50, 150, 50));
        assertThrows(NullPointerException.class, () -> new NetworkService(null));
        assertEquals(200, NetworkService.Timing.DEFAULT.lagThresholdMillis(), "요구사항: 지연 200ms 이하");
    }

    @Test
    void defaultConstructorUsesDefaultTiming() throws InterruptedException {
        NetworkService service = new NetworkService();
        services.add(service);
        service.openHost(0);

        assertTrue(service.getLocalPort() > 0);
    }

    // ------------------------------------------------------------ 도우미

    private void echoPongs(Socket peer, long delayMillis) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(peer.getInputStream()));
            DataOutputStream out = new DataOutputStream(peer.getOutputStream());
            while (!peer.isClosed()) {
                NetworkMessage message = codec.decode(MessageCodec.readFrame(in));
                if (message.getType() == MessageType.PING) {
                    Thread.sleep(delayMillis);
                    NetworkMessage pong = NetworkMessage.of(MessageType.PONG, 0, message.getPayload()).withSequence(1);
                    MessageCodec.writeFrame(out, codec.encode(pong));
                }
            }
        } catch (IOException | InterruptedException e) {
            // 테스트 종료로 소켓이 닫힘
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(RecordingListener.WAIT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition not met in time");
            }
            Thread.sleep(10);
        }
    }
}
