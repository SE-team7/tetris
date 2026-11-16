package com.team.tetris.controller;

import java.util.function.Consumer;

import com.team.tetris.view.P2PConnectionView;
import com.team.tetris.view.P2PConnectionView.P2PItem;

import javafx.scene.input.KeyCode;

public class P2PConnectionController {

    private final P2PConnectionView view;
    private final Runnable onHost;
    private final Consumer<String> onJoin;
    private final Runnable onBack;

    private P2PItem selected = P2PItem.HOST;

    public P2PConnectionController(
        P2PConnectionView view,
        Runnable onHost,
        Consumer<String> onJoin,
        Runnable onBack
    ) {
        this.view = view;
        this.onHost = onHost;
        this.onJoin = onJoin;
        this.onBack = onBack;
    }

    public void show() {
        selected = P2PItem.HOST;
        view.clearIp();
        view.setHostIp("");
        view.setStatus("연결 대기 중");
        view.render(selected);
    }

    public void handleKey(KeyCode key) {
        if (key == null) return;

        if (view.isIpFocused()) {
            if (key == KeyCode.ENTER) {
                join();
            } else if (key == KeyCode.ESCAPE) {
                view.getRoot().requestFocus();
            }
            return;
        }

        switch (key) {
            case UP -> move(-1);
            case DOWN -> move(1);
            case ENTER -> select();
            case ESCAPE -> onBack.run();
            default -> {}
        }
    }

    private void move(int direction) {
        P2PItem[] items = P2PItem.values();
        selected = items[Math.floorMod(selected.ordinal() + direction, items.length)];
        view.render(selected);
    }

    private void select() {
        switch (selected) {
            case HOST -> onHost.run();
            case JOIN -> view.focusIp();
            case BACK -> onBack.run();
        }
    }

    private void join() {
        String ip = view.getIp();

        if (ip.isBlank()) {
            view.setStatus("IP 주소를 입력해주세요.");
            return;
        }

        onJoin.accept(ip);
    }

    public void showHostIp(String ip) {
        view.setHostIp(ip);
        view.setStatus("상대방의 접속을 기다리는 중...");
    }

    public void showConnecting() {
        view.setStatus("연결 중...");
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showError(String message) {
        view.setStatus("오류: " + message);
    }
}