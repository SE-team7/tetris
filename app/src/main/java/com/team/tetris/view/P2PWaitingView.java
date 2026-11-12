package com.team.tetris.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class P2PWaitingView {

    private final VBox root = new VBox(18);
    private final Label statusLabel = new Label("연결 대기 중...");
    private final Label infoLabel = new Label("");
    private final Label readyLabel = new Label("ENTER : READY");
    private final Label backLabel = new Label("ESC : 뒤로가기");

    public P2PWaitingView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("p2p-waiting-root");

        Label title = new Label("P2P WAITING ROOM");
        title.getStyleClass().add("p2p-waiting-title");

        statusLabel.getStyleClass().add("p2p-waiting-status");
        infoLabel.getStyleClass().add("p2p-waiting-info");
        readyLabel.getStyleClass().add("p2p-waiting-ready");
        backLabel.getStyleClass().add("p2p-waiting-back");

        root.getChildren().addAll(
            title,
            statusLabel,
            infoLabel,
            readyLabel,
            backLabel
        );
    }

    public Parent getRoot() {
        return root;
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setInfo(String text) {
        infoLabel.setText(text == null ? "" : text);
    }

    public void setReady(boolean ready) {
        readyLabel.setText(
            ready ? "READY ✓" : "ENTER : READY"
        );
    }
}