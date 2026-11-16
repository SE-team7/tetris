package com.team.tetris.controller;

import java.util.function.Consumer;

import com.team.tetris.view.P2PWaitingView;

import javafx.scene.input.KeyCode;

public class P2PWaitingController {

    private final P2PWaitingView view;
    private final Runnable onStart;
    private final Runnable onBack;
    private final Consumer<Boolean> onReadyChange;

    private boolean ready;

    public P2PWaitingController(
        P2PWaitingView view,
        Runnable onStart,
        Runnable onBack
    ) {
        this(view, onStart, onBack, ready -> {});
    }

    public P2PWaitingController(
        P2PWaitingView view,
        Runnable onStart,
        Runnable onBack,
        Consumer<Boolean> onReadyChange
    ) {
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

        StringBuilder info = new StringBuilder();

        if (ip != null && !ip.isBlank()) {
            info.append("HOST IP : ").append(ip);
        }

        if (mode != null && !mode.isBlank()) {
            if (!info.isEmpty()) info.append("\n");
            info.append("MODE : ").append(mode);
        }

        view.setInfo(info.toString());
    }

    public void showClient(String ip) {
        reset();

        if (ip == null || ip.isBlank()) {
            view.setInfo("서버의 게임 설정을 기다리는 중...");
        } else {
            view.setInfo(
                "SERVER IP : " + ip
                    + "\n서버의 게임 설정을 기다리는 중..."
            );
        }
    }

    private void reset() {
        ready = false;
        view.setReady(false);
        view.setStatus("상대방을 기다리는 중...");
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
        view.setStatus(
            ready ? "READY" : "상대방을 기다리는 중..."
        );

        onReadyChange.accept(ready);
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showWaiting() {
        view.setStatus("상대방을 기다리는 중...");
    }

    public void showOpponentReady() {
        view.setInfo("상대방 READY");
    }

    public void startBattle() {
        view.setStatus("START");
        onStart.run();
    }

    public void showError(String message) {
        view.setStatus("오류: " + message);
    }

    public boolean isReady() {
        return ready;
    }
}