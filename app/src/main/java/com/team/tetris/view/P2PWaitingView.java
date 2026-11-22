package com.team.tetris.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class P2PWaitingView {
    private final VBox root = new VBox(18);
    private final Label status = new Label("연결 대기 중...");
    private final Label info = new Label();
    private final Label start = new Label("연결을 기다리는 중...");
    private final Label lag = new Label();
    private final Label back = new Label("TAB : 뒤로가기");

    public P2PWaitingView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("p2p-waiting-root");

        Label title = new Label("P2P WAITING ROOM");
        title.getStyleClass().add("p2p-waiting-title");
        status.getStyleClass().add("p2p-waiting-status");
        info.getStyleClass().add("p2p-waiting-info");
        start.getStyleClass().add("p2p-waiting-ready");
        back.getStyleClass().add("p2p-waiting-back");

        root.getChildren().addAll(title, status, info, start, lag, back);
    }

    public Parent getRoot() { return root; }
    public void setStatus(String text) { status.setText(text); }
    public void setInfo(String text) { info.setText(text == null ? "" : text); }

    public void setReady(boolean ready) {
        start.setText(ready ? "게임 시작 요청 완료" : "ENTER : 게임 시작");
    }

    public void setStartEnabled(boolean enabled) {
        start.setText(enabled ? "ENTER : 게임 시작" : "연결 및 게임 설정 대기 중...");
    }

    public void setLag(long ms, boolean lagging) {
        lag.setText(lagging ? "NETWORK LAG : " + ms + "ms" : "");
    }
}