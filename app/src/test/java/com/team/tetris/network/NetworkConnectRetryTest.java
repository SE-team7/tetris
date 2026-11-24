package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * [회의 7차 문제 6·7] 클라이언트가 서버보다 먼저 접속한 경우.
 *  - 문제 6: 서버가 아직 열리지 않았으면 무한 대기하지 않는다
 *  - 문제 7: 3초마다 다시 접속을 시도하고, 30초 동안 서버를 못 찾으면 포기(시간 초과)한다
 * 테스트 시간을 줄이려고 간격을 줄여서 같은 규칙(재시도 간격·포기 시간)을 확인한다.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class NetworkConnectRetryTest {

    /** 200ms마다 재시도, 1.5초 후 포기 */
    private static final NetworkService.Timing RETRY =
            new NetworkService.Timing(1000, 50, 150, 800, 200, 1500);
    private static final String LOCALHOST = "127.0.0.1";

    private final List<NetworkService> services = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        services.forEach(NetworkService::shutdown);
    }

    private NetworkService newService(NetworkService.Timing timing, NetworkListener listener) {
        NetworkService service = new NetworkService(timing);
        service.setListener(listener);
        services.add(service);
        return service;
    }

    /** 재시도·실패 통지까지 기록 */
    private static final class RetryListener extends RecordingListener {
        final List<Integer> attempts = new CopyOnWriteArrayList<>();
        final List<Long> remaining = new CopyOnWriteArrayList<>();
        final List<Long> retryAtNanos = new CopyOnWriteArrayList<>();
        final List<String> failures = new CopyOnWriteArrayList<>();

        @Override
        public void onConnectRetry(int attempt, long remainingMillis) {
            attempts.add(attempt);
            remaining.add(remainingMillis);
            retryAtNanos.add(System.nanoTime());
        }

        @Override
        public void onConnectFailed(String message) {
            failures.add(message);
        }
    }

    /** 지금 아무도 듣고 있지 않은 포트 */
    private static int unusedPort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    // ================================================================ 기본값

    @Test
    void defaultTiming_retriesEvery3SecondsAndGivesUpAfter30Seconds() {
        assertEquals(3000, NetworkService.Timing.DEFAULT.connectRetryIntervalMillis(), "문제 7: 3초마다 재시도");
        assertEquals(30_000, NetworkService.Timing.DEFAULT.connectGiveUpMillis(), "문제 7: 30초 후 시간 초과");
    }

    @Test
    void timing_validatesRetryValues() {
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(1000, 50, 150, 800, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new NetworkService.Timing(1000, 50, 150, 800, 200, -1));
        NetworkService.Timing once = new NetworkService.Timing(1000, 50, 150, 800);
        assertEquals(0, once.connectGiveUpMillis(), "4개짜리 생성자는 재시도 없이 한 번만 시도");
    }

    // ================================================================ 문제 6: 클라이언트가 먼저 접속

    @Test
    void clientFirst_thenServerOpens_connectsWithoutRestarting() throws Exception {
        int port = unusedPort();
        RetryListener clientEvents = new RetryListener();
        NetworkService client = newService(RETRY, clientEvents);

        client.connect(LOCALHOST, port);   // 아직 서버 없음
        waitUntil(() -> clientEvents.attempts.size() >= 2);
        assertTrue(client.isOpening(), "재시도 중에는 접속 시도 상태 유지");
        assertFalse(client.isConnected());

        RecordingListener hostEvents = new RecordingListener();
        NetworkService host = newService(RETRY, hostEvents);
        host.openHost(port);   // 늦게 서버를 연다

        clientEvents.awaitConnected();
        hostEvents.awaitConnected();
        assertTrue(client.isConnected());
        assertTrue(clientEvents.failures.isEmpty(), "포기하기 전에 연결됨");
        assertTrue(clientEvents.errors.isEmpty(), clientEvents.errors.toString());
    }

    // ================================================================ 문제 7: 3초마다 재시도, 30초 후 시간 초과

    @Test
    void noServer_retriesAtIntervalThenFailsAfterGiveUpTime() throws Exception {
        RetryListener events = new RetryListener();
        NetworkService client = newService(RETRY, events);
        long start = System.nanoTime();

        client.connect(LOCALHOST, unusedPort());

        waitUntil(() -> !events.failures.isEmpty());
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis >= 1200 && elapsedMillis < 3000, "포기까지 걸린 시간: " + elapsedMillis + "ms");

        // 1.5초 / 0.2초 간격 → 마지막 시도 전까지 6번 이상 재시도 통지
        assertTrue(events.attempts.size() >= 5, "재시도 횟수: " + events.attempts);
        for (int i = 0; i < events.attempts.size(); i++) {
            assertEquals(i + 1, events.attempts.get(i), "시도 번호는 1부터 차례로");
        }
        for (int i = 1; i < events.retryAtNanos.size(); i++) {
            long gap = TimeUnit.NANOSECONDS.toMillis(events.retryAtNanos.get(i) - events.retryAtNanos.get(i - 1));
            assertTrue(gap >= 150 && gap < 600, "재시도 간격: " + gap + "ms");
        }
        for (int i = 1; i < events.remaining.size(); i++) {
            assertTrue(events.remaining.get(i) < events.remaining.get(i - 1), "남은 시간은 줄어듦: " + events.remaining);
        }

        assertEquals(1, events.failures.size(), "실패 통지는 한 번");
        String message = events.failures.get(0);
        assertTrue(message.contains("찾을 수 없습니다"), message);
        assertTrue(message.contains("IP 주소를 다시 확인"), message);
        assertFalse(client.isOpening());
        assertFalse(client.isConnected());
        assertEquals(0, events.connectedCount.get());
    }

    @Test
    void afterTimeout_canTryAgain() throws Exception {
        RetryListener events = new RetryListener();
        NetworkService client = newService(RETRY, events);
        int port = unusedPort();
        client.connect(LOCALHOST, port);
        waitUntil(() -> !events.failures.isEmpty());

        RecordingListener hostEvents = new RecordingListener();
        newService(RETRY, hostEvents).openHost(port);
        client.connect(LOCALHOST, port);   // 주소 재확인 후 다시 접속

        events.awaitConnected();
        hostEvents.awaitConnected();
    }

    @Test
    void cancelDuringRetry_stopsSilentlyAndAllowsNewConnection() throws Exception {
        RetryListener events = new RetryListener();
        NetworkService client = newService(RETRY, events);
        client.connect(LOCALHOST, unusedPort());
        waitUntil(() -> !events.attempts.isEmpty());

        client.disconnect();   // 대기 화면에서 뒤로가기
        int attemptsAtCancel = events.attempts.size();
        Thread.sleep(RETRY.connectGiveUpMillis() + 300);

        assertFalse(client.isOpening());
        assertTrue(events.attempts.size() <= attemptsAtCancel + 1, "취소 후에는 재시도 멈춤: " + events.attempts);
        assertTrue(events.failures.isEmpty(), "스스로 취소한 것은 실패로 알리지 않음");
        assertTrue(events.errors.isEmpty());

        RecordingListener hostEvents = new RecordingListener();
        NetworkService host = newService(RETRY, hostEvents);
        host.openHost(0);
        client.connect(LOCALHOST, host.getLocalPort());
        events.awaitConnected();
    }

    @Test
    void singleAttemptTiming_failsImmediatelyThroughOnError() throws Exception {
        RecordingListener events = new RecordingListener();   // onConnectFailed 기본 구현 → onError
        NetworkService client = newService(new NetworkService.Timing(1000, 50, 150, 800), events);
        long start = System.nanoTime();

        client.connect(LOCALHOST, unusedPort());

        String error = events.nextError();
        assertTrue(error.contains("연결할 수 없습니다"), error);
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1500, "재시도 없이 바로 실패");
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition not met in time");
            }
            Thread.sleep(5);
        }
    }
}
