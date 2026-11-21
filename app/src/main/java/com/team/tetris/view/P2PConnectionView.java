package com.team.tetris.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class P2PConnectionView {

    public enum P2PItem {
        HOST,
        JOIN,
        BACK
    }

    private final VBox root = new VBox(16);

    private final Label hostLabel =
        new Label("서버 만들기");

    private final Label joinLabel =
        new Label("서버 접속");

    private final Label backLabel =
        new Label("뒤로가기");

    private final TextField ipField =
        new TextField();

    private final Label statusLabel =
        new Label("연결 대기 중");

    private final Label hostIpLabel =
        new Label("");

    public P2PConnectionView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("p2p-root");

        Label title = new Label("P2P BATTLE");
        title.getStyleClass().add("p2p-title");

        hostLabel.getStyleClass().add("p2p-item");
        joinLabel.getStyleClass().add("p2p-item");
        backLabel.getStyleClass().add("p2p-item");

        ipField.setPromptText("IP 주소 입력");
        ipField.getStyleClass().add("p2p-ip-field");
        ipField.setMaxWidth(260);

        statusLabel.getStyleClass().add("p2p-status");
        hostIpLabel.getStyleClass().add("p2p-host-ip");

        root.getChildren().addAll(
            title,
            hostLabel,
            joinLabel,
            ipField,
            hostIpLabel,
            statusLabel,
            backLabel
        );

        render(P2PItem.HOST);
    }

    public Parent getRoot() {
        return root;
    }

    public String getIp() {
        return ipField.getText().trim();
    }

    public void setIp(String ip) {
        ipField.setText(ip == null ? "" : ip);
    }

    public void setHostIp(String ip) {
        hostIpLabel.setText(
            ip == null || ip.isBlank()
                ? ""
                : "HOST IP : " + ip
        );
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void render(P2PItem selected) {
        Label[] items = {
            hostLabel,
            joinLabel,
            backLabel
        };

        for (int i = 0; i < items.length; i++) {
            items[i].getStyleClass().removeAll(
                "p2p-selected",
                "p2p-unselected"
            );

            items[i].getStyleClass().add(
                i == selected.ordinal()
                    ? "p2p-selected"
                    : "p2p-unselected"
            );
        }
    }
}