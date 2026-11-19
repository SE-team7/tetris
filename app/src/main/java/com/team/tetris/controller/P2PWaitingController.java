package com.team.tetris.controller;

import java.util.function.Consumer;
import com.team.tetris.view.P2PWaitingView;
import javafx.scene.input.KeyCode;

public class P2PWaitingController {
    private final P2PWaitingView view;
    private final Runnable onStart, onBack;
    private final Consumer<Boolean> onReadyChange;
    private boolean ready, enabled;

    public P2PWaitingController(P2PWaitingView view, Runnable onStart, Runnable onBack) {
        this(view, onStart, onBack, r -> {});
    }

    public P2PWaitingController(P2PWaitingView view, Runnable onStart,
                                Runnable onBack, Consumer<Boolean> onReadyChange) {
        this.view = view;
        this.onStart = onStart;
        this.onBack = onBack;
        this.onReadyChange = onReadyChange;
    }

    public void show() {
        reset();
        view.setInfo("");
    }

    public void showHost(String ip, String mode) {
        reset();
        view.setInfo("HOST IP : " + ip + "\nMODE : " + mode);
    }

    public void showClient(String ip) {
        reset();
        view.setInfo("SERVER IP : " + ip + "\n서버의 게임 설정을 기다리는 중...");
    }

    private void reset() {
        ready = false;
        enabled = false;
        view.setStatus("상대방을 기다리는 중...");
        view.setStartEnabled(false);
        view.setLag(0, false);
    }

    public void setStartEnabled(boolean value) {
        enabled = value && !ready;
        if (!ready) view.setStartEnabled(enabled);
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.ENTER) {
            if (!enabled || ready) return;
            onReadyChange.accept(true);
            ready = true;
            enabled = false;
            view.setReady(true);
            view.setStatus("상대방의 시작을 기다리는 중...");
        } else if (key == KeyCode.TAB) {
            onBack.run();
        }
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showWaiting() {
        view.setStatus("상대방을 기다리는 중...");
    }

    public void showOpponentReady() {
        view.setStatus("상대방 준비 완료");
    }

    public void startBattle() {
        view.setStatus("START");
        onStart.run();
    }

    public void showError(String message) {
        view.setStatus("오류: " + message);
    }

    public void showLag(long ms, boolean lagging) {
        view.setLag(ms, lagging);
    }

    public boolean isReady() { return ready; }
}