package com.team.tetris.controller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.team.tetris.network.MessagePayload;
import com.team.tetris.network.NetworkEvents;

/** NetworkController가 화면에 보낸 통지를 기록 */
class RecordingNetworkEvents implements NetworkEvents {

    final List<String> statuses = new CopyOnWriteArrayList<>();
    final List<String> errors = new CopyOnWriteArrayList<>();
    final List<NetworkController.State> states = new CopyOnWriteArrayList<>();
    final List<MessagePayload.Config> configs = new CopyOnWriteArrayList<>();
    final List<Boolean> peerReady = new CopyOnWriteArrayList<>();
    final List<Long> startingDelays = new CopyOnWriteArrayList<>();
    final List<Boolean> pauses = new CopyOnWriteArrayList<>();
    final List<MessagePayload.Result> results = new CopyOnWriteArrayList<>();
    final List<Boolean> rematchAnswers = new CopyOnWriteArrayList<>();
    final List<Boolean> lagStates = new CopyOnWriteArrayList<>();
    final List<Boolean> closedByPeerLeft = new CopyOnWriteArrayList<>();
    volatile int battleStarted;
    volatile int rematchRequests;

    @Override
    public void showConnectionStatus(String status) {
        statuses.add(status);
    }

    @Override
    public void showError(String message) {
        errors.add(message);
    }

    @Override
    public void onStateChanged(NetworkController.State state) {
        states.add(state);
    }

    @Override
    public void onConfigChanged(MessagePayload.Config config) {
        configs.add(config);
    }

    @Override
    public void onPeerReadyChanged(boolean ready) {
        peerReady.add(ready);
    }

    @Override
    public void onBattleStarting(long delayMillis) {
        startingDelays.add(delayMillis);
    }

    /** 마지막 대전 시작 시각 (System.nanoTime) */
    volatile long battleStartedAtNanos;

    @Override
    public void onBattleStarted() {
        battleStartedAtNanos = System.nanoTime();
        battleStarted++;
    }

    @Override
    public void onPauseChanged(boolean paused) {
        pauses.add(paused);
    }

    @Override
    public void onBattleFinished(MessagePayload.Result result) {
        results.add(result);
    }

    @Override
    public void onRematchRequested() {
        rematchRequests++;
    }

    @Override
    public void onRematchAnswered(boolean accepted) {
        rematchAnswers.add(accepted);
    }

    @Override
    public void onLagChanged(long roundTripMillis, boolean lagging) {
        lagStates.add(lagging);
    }

    @Override
    public void onConnectionClosed(String reason, boolean peerLeft) {
        closedByPeerLeft.add(peerLeft);
    }
}
