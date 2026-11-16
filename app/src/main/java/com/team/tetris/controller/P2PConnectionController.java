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
    private boolean enteringIp;

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
        enteringIp = false;

        view.clearIp();
        view.showIpField(false);
        view.render(selected);
    }

    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }

        if (enteringIp) {
            handleIpInput(key);
            return;
        }

        switch (key) {
            case UP -> move(-1);
            case DOWN -> move(1);
            case ENTER -> select();
            case ESCAPE -> onBack.run();
            default -> {
            }
        }
    }

    private void handleIpInput(KeyCode key) {
        if (key == KeyCode.ESCAPE) {
            enteringIp = false;
            view.showIpField(false);
            view.getRoot().requestFocus();
            return;
        }

        if (key == KeyCode.ENTER) {
            String ip = view.getIp();

            if (!ip.isBlank()) {
                enteringIp = false;
                onJoin.accept(ip);
            }
        }
    }

    private void move(int direction) {
        P2PItem[] items = P2PItem.values();

        selected = items[
            Math.floorMod(
                selected.ordinal() + direction,
                items.length
            )
        ];

        view.render(selected);
    }

    private void select() {
        switch (selected) {
            case HOST -> onHost.run();

            case JOIN -> {
                enteringIp = true;
                view.focusIp();
            }

            case BACK -> onBack.run();
        }
    }

    public void showHostIp(String ip) {
    }

    public void showConnecting() {
    }

    public void showConnected() {
    }

    public void showError(String message) {
    }
}