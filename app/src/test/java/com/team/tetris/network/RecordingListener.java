package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 통신 스레드에서 오는 콜백을 기록해 테스트 스레드에서 기다릴 수 있게 하는 리스너 */
class RecordingListener implements NetworkListener {

    static final long WAIT_SECONDS = 5;

    final CountDownLatch connected = new CountDownLatch(1);
    final CountDownLatch disconnected = new CountDownLatch(1);
    final AtomicInteger connectedCount = new AtomicInteger();
    final AtomicInteger disconnectedCount = new AtomicInteger();
    final BlockingQueue<NetworkMessage> messages = new LinkedBlockingQueue<>();
    final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
    final BlockingQueue<Long> latencies = new LinkedBlockingQueue<>();
    final BlockingQueue<Boolean> lagStates = new LinkedBlockingQueue<>();
    final List<String> threadNames = new ArrayList<>();

    @Override
    public void onConnected() {
        connectedCount.incrementAndGet();
        connected.countDown();
    }

    @Override
    public void onMessage(NetworkMessage message) {
        synchronized (threadNames) {
            threadNames.add(Thread.currentThread().getName());
        }
        messages.add(message);
    }

    @Override
    public void onDisconnected() {
        disconnectedCount.incrementAndGet();
        disconnected.countDown();
    }

    @Override
    public void onError(String message) {
        errors.add(message);
    }

    @Override
    public void onLatencyChanged(long roundTripMillis, boolean lagging) {
        latencies.add(roundTripMillis);
        lagStates.add(lagging);
    }

    void awaitConnected() throws InterruptedException {
        if (!connected.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
            fail("onConnected not called. errors=" + errors);
        }
    }

    void awaitDisconnected() throws InterruptedException {
        if (!disconnected.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
            fail("onDisconnected not called");
        }
    }

    NetworkMessage nextMessage() throws InterruptedException {
        NetworkMessage message = messages.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(message, "no message received");
        return message;
    }

    String nextError() throws InterruptedException {
        String error = errors.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(error, "no error reported");
        return error;
    }

    /** lagging 값이 expected인 통지가 올 때까지 기다린다 */
    boolean awaitLagState(boolean expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            Boolean state = lagStates.poll(100, TimeUnit.MILLISECONDS);
            if (state != null && state == expected) {
                return true;
            }
        }
        return false;
    }
}
