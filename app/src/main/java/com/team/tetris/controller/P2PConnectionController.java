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
        view.render(selected);
        view.setStatus("연결 대기 중");
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.UP) {
            move(-1);
        } else if (key == KeyCode.DOWN) {
            move(1);
        } else if (key == KeyCode.ENTER) {
            select();
        } else if (key == KeyCode.ESCAPE) {
            onBack.run();
        }
    }

    private void move(int direction) {
        P2PItem[] items = P2PItem.values();

        int index =
            (selected.ordinal()
                + direction
                + items.length)
                % items.length;

        selected = items[index];
        view.render(selected);
    }

    private void select() {
        switch (selected) {
            case HOST -> onHost.run();

            case JOIN ->
                onJoin.accept(
                    view.getIp()
                );

            case BACK ->
                onBack.run();
        }
    }

    public void showHostIp(String ip) {
        view.setHostIp(ip);
        view.setStatus(
            "상대방의 접속을 기다리는 중..."
        );
    }

    public void showConnecting() {
        view.setStatus("연결 중...");
    }

    public void showConnected() {
        view.setStatus("연결 완료");
    }

    public void showError(String message) {
        view.setStatus(
            "오류: " + message
        );
    }
}