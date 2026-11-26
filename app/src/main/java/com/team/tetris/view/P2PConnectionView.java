package com.team.tetris.view;

import java.util.List;
import com.team.tetris.network.RecentHostStore.HostEntry;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class P2PConnectionView {

    public enum P2PItem {
        HOST,
        JOIN
    }

    private final VBox root = new VBox(16);

    private final Label hostLabel = new Label("서버 만들기");
    private final Label joinLabel = new Label("서버 접속");
    private final Label backHint = new Label("TAB : 뒤로가기");

    private final TextField ipField = new TextField();
    private final Label message = new Label();
    private final VBox recentHostsBox = new VBox(4);

    public P2PConnectionView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("p2p-root");

        Label title = new Label("P2P BATTLE");
        title.getStyleClass().add("p2p-title");

        hostLabel.getStyleClass().add("p2p-item");
        joinLabel.getStyleClass().add("p2p-item");
        backHint.getStyleClass().add("battle-back-hint");
        VBox.setMargin(backHint, new Insets(28, 0, 0, 0));

        ipField.setPromptText("IP 주소 입력");
        ipField.getStyleClass().add("p2p-ip-field");
        ipField.setMaxWidth(260);
        recentHostsBox.setAlignment(Pos.CENTER);
        recentHostsBox.setMaxWidth(320);
        recentHostsBox.getStyleClass().add("p2p-recent-hosts");
        recentHostsBox.setManaged(false);
        recentHostsBox.setVisible(false);
        message.getStyleClass().add("p2p-status");
        message.setWrapText(true);
        message.setMaxWidth(420);
        message.setAlignment(Pos.CENTER);
        setStatus("");

        root.getChildren().addAll(
            title,
            hostLabel,
            joinLabel,
            ipField,
            recentHostsBox,
            message,
            backHint
        );

        showIpField(false);
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

    public void clearIp() {
        ipField.clear();
    }

    public void showIpField(boolean visible) {
        ipField.setVisible(visible);
        ipField.setManaged(visible);
        if (!visible) { recentHostsBox.setVisible(false); recentHostsBox.setManaged(false); }
    }

    public void showRecentHosts(List<HostEntry> hosts) {
        recentHostsBox.getChildren().clear();
        if (hosts != null && !hosts.isEmpty()) {
            Label heading = new Label("RECENT IP");
            heading.getStyleClass().add("p2p-recent-title");
            recentHostsBox.getChildren().add(heading);
            for (HostEntry entry : hosts) {
                Label item = new Label(entry.toString());
                item.getStyleClass().add("p2p-recent-item");
                item.setMaxWidth(Double.MAX_VALUE);
                item.setOnMouseClicked(e -> {
                    setIp(entry.host());
                    focusIp();
                });
                recentHostsBox.getChildren().add(item);
            }
        }
        boolean visible = hosts != null && !hosts.isEmpty();
        recentHostsBox.setVisible(visible);
        recentHostsBox.setManaged(visible);
    }

    public void focusIp() {
        showIpField(true);
        ipField.requestFocus();
        ipField.positionCaret(ipField.getText().length());
    }

    public boolean isIpFocused() {
        return ipField.isFocused();
    }

    public void render(P2PItem selected) {
        Label[] items = {
            hostLabel,
            joinLabel
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

   
    /** 안내·오류 문구 (비어 있으면 숨김) */
    public void setStatus(String text) {
        String value = text == null ? "" : text;
        message.setText(value);
        message.setVisible(!value.isEmpty());
        message.setManaged(!value.isEmpty());
    }

    public String getStatus() {
        return message.getText();
    }

    public void setHostIp(String ip) {
    }
}