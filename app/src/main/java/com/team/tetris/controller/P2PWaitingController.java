package com.team.tetris.controller;

import com.team.tetris.view.P2PWaitingView;

import javafx.scene.input.KeyCode;

public class P2PWaitingController {

    private final P2PWaitingView view;
    private final Runnable onStart;
    private final Runnable onBack;

    private boolean ready;

    public P2PWaitingController(
        P2PWaitingView view,
        Runnable onStart,
        Runnable onBack
    ) {
        this.view = view;
        this.onStart = onStart;
        this.onBack = onBack;
    }

    public void show() {
        ready = false;

        view.setReady(false);
        view.setStatus(
            "상대방을 기다리는 중..."
        );
        view.setInfo("");
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.ENTER) {
            toggleReady();
        } else if (key == KeyCode.ESCAPE) {
            onBack.run();
        }
    }

    private void toggleReady() {
        ready = !ready;

        view.setReady(ready);

        if (ready) {
            view.setStatus("READY");
        } else {
            view.setStatus(
                "상대방을 기다리는 중..."
            );
        }
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showWaiting() {
        view.setStatus(
            "상대방을 기다리는 중..."
        );
    }

    public void showOpponentReady() {
        view.setInfo(
            "상대방 READY"
        );
    }

    public void startBattle() {
        view.setStatus("START");
        onStart.run();
    }

    public void showError(String message) {
        view.setStatus(
            "오류: " + message
        );
    }

    public boolean isReady() {
        return ready;
    }
}