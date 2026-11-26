package com.team.tetris.controller;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.Consumer;
import com.team.tetris.network.RecentHostStore.HostEntry;

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
    private final Supplier<List<HostEntry>> recentHosts;
    private List<HostEntry> cachedHosts = List.of();
    private int recentIndex = -1;

    public P2PConnectionController(
        P2PConnectionView view,
        Runnable onHost,
        Consumer<String> onJoin,
        Runnable onBack
    ) {
        this(view, onHost, onJoin, onBack, List::of);
    }

    public P2PConnectionController(P2PConnectionView view, Runnable onHost,
                                   Consumer<String> onJoin, Runnable onBack,
                                   Supplier<List<HostEntry>> recentHosts) {
        this.recentHosts = recentHosts;
        this.view = view;
        this.onHost = onHost;
        this.onJoin = onJoin;
        this.onBack = onBack;
    }

    public void show() {
        selected = P2PItem.HOST;
        enteringIp = false;
        recentIndex = -1;

        view.clearIp();
        view.showIpField(false);
        view.setStatus("");
        view.render(selected);
    }

    /**
     * 서버를 찾지 못했을 때 (회의 7차 문제 6·7): 주소 재확인 안내를 보여주고
     * '서버 접속'의 IP 입력란으로 바로 이동한다. 입력했던 IP를 채워 두어 고쳐서 다시 접속할 수 있다.
     */
    public void showAddressCheck(String ip, String message) {
        selected = P2PItem.JOIN;
        view.render(selected);
        enteringIp = true;
        recentIndex = -1;
        cachedHosts = recentHosts.get();
        view.showRecentHosts(cachedHosts);
        view.setIp(ip);
        view.setStatus(message == null || message.isBlank()
                ? "서버를 찾을 수 없습니다. IP 주소를 다시 확인해주세요."
                : message);
        view.focusIp();
    }

    public boolean isEnteringIp() {
        return enteringIp;
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
        if ((key == KeyCode.UP || key == KeyCode.DOWN) && !cachedHosts.isEmpty()) {
            recentIndex = Math.floorMod(recentIndex + (key == KeyCode.DOWN ? 1 : -1), cachedHosts.size());
            view.setIp(cachedHosts.get(recentIndex).host());
            view.focusIp();
            return;
        }
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
        P2PItem[] items = { P2PItem.HOST, P2PItem.JOIN };

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
                recentIndex = -1;
                cachedHosts = recentHosts.get();
                view.showRecentHosts(cachedHosts);
                view.focusIp();
            }

        }
    }

    public void showHostIp(String ip) {
    }

    public void showConnecting() {
    }

    public void showConnected() {
    }

    public void showError(String message) {
        view.setStatus(message == null ? "" : "오류: " + message);
    }
}