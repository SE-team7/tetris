package com.team.tetris.view;

import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * P2P 대기실 화면.
 * 요구사항: 연결 완료 후 서버와 클라이언트가 모두 '게임 시작' 버튼을 누르면 대전을 시작한다.
 * 버튼은 마우스 클릭과 Enter 키 모두로 누를 수 있다. (Enter는 P2PWaitingController에서 처리)
 */
public class P2PWaitingView {
    static final String START_TEXT = "게임 시작";
    static final String START_PRESSED_TEXT = "게임 시작 완료";
    private static final PseudoClass SELF_MESSAGE = PseudoClass.getPseudoClass("self");

    private final VBox root = new VBox(18);
    private final Label status = new Label("연결 대기 중...");
    private final Label info = new Label();
    private final Button start = new Button(START_TEXT);
    private final Label startGuide = new Label();
    private final Label lag = new Label();
    private final Label back = new Label("TAB : 뒤로가기");
    private final ListView<String> chatMessages = new ListView<>();
    private final TextField chatInput = new TextField();
    private final Button sendChat = new Button("전송");
    private final Label chatError = new Label();
    private final VBox chatPanel = new VBox(8);
    private final VBox chatSlot = new VBox();

    private Runnable onStart = () -> {};

    public P2PWaitingView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("p2p-waiting-root");

        Label title = new Label("P2P WAITING ROOM");
        title.getStyleClass().add("p2p-waiting-title");
        status.getStyleClass().add("p2p-waiting-status");
        info.getStyleClass().add("p2p-waiting-info");
        start.getStyleClass().add("p2p-waiting-start");
        startGuide.getStyleClass().add("p2p-waiting-ready");
        lag.getStyleClass().add("p2p-waiting-lag");
        back.getStyleClass().add("p2p-waiting-back");

        // TAB(뒤로가기)·Space가 버튼 포커스 이동·중복 클릭으로 처리되지 않도록 포커스는 받지 않는다
        start.setFocusTraversable(false);
        start.setOnAction(e -> onStart.run());
        start.setMinWidth(180);

        Label chatTitle = new Label("채팅");
        chatTitle.getStyleClass().add("p2p-chat-title");
        chatPanel.getStyleClass().add("p2p-chat-panel");
        chatPanel.setMaxWidth(480);
        chatSlot.setAlignment(Pos.CENTER);
        chatMessages.getStyleClass().add("p2p-chat-messages");
        chatMessages.setPrefHeight(124);
        chatMessages.setMinHeight(76);
        chatMessages.setFocusTraversable(false);
        chatMessages.setAccessibleText("채팅 기록");
        Label placeholder = new Label("아직 메시지가 없습니다.");
        placeholder.getStyleClass().add("p2p-chat-placeholder");
        chatMessages.setPlaceholder(placeholder);
        chatMessages.setCellFactory(list -> new ListCell<>() {
            private final Label sender = new Label();
            private final Label body = new Label();
            private final HBox row = new HBox(8, sender, body);

            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                row.setAlignment(Pos.TOP_LEFT);
                row.setMinWidth(0);
                row.prefWidthProperty().bind(list.widthProperty().subtract(34));
                row.setMaxWidth(HBox.USE_PREF_SIZE);
                sender.getStyleClass().add("p2p-chat-sender");
                body.getStyleClass().add("p2p-chat-body");
                body.setWrapText(true);
                body.setMinWidth(0);
            }

            @Override
            protected void updateItem(String message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setGraphic(null);
                    setAccessibleText(null);
                    return;
                }
                int separator = message.indexOf(": ");
                sender.setText(message.substring(0, separator));
                sender.pseudoClassStateChanged(SELF_MESSAGE, "나".equals(sender.getText()));
                body.setText(message.substring(separator + 2));
                setGraphic(row);
                setAccessibleText(message);
            }
        });
        chatInput.setPromptText("채팅 메시지");
        chatInput.getStyleClass().add("p2p-chat-input");
        chatInput.setOnAction(event -> sendChat.fire());
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        sendChat.setFocusTraversable(false);
        sendChat.getStyleClass().add("p2p-chat-send");
        sendChat.setMinWidth(Button.USE_PREF_SIZE);
        HBox chatEntry = new HBox(8, chatInput, sendChat);
        chatError.getStyleClass().add("p2p-chat-error");
        chatError.setWrapText(true);
        chatPanel.getChildren().addAll(chatTitle, chatMessages, chatEntry, chatError);
        setChatError(null);
        restoreChatPanel();

        root.getChildren().addAll(title, status, info, start, startGuide, chatSlot, lag, back);
        setStartEnabled(false);
        setChatEnabled(false);
    }

    public Parent getRoot() { return root; }
    public void setStatus(String text) { status.setText(text); }
    public void setInfo(String text) { info.setText(text == null ? "" : text); }

    /** 게임 시작 버튼을 눌렀을 때 실행할 동작 */
    public void setOnStart(Runnable action) {
        onStart = action == null ? () -> {} : action;
    }

    public void setOnSendChat(Consumer<String> action) {
        sendChat.setOnAction(action == null ? null : event -> action.accept(chatInput.getText()));
    }

    public void appendChat(String sender, String text) {
        chatMessages.getItems().add(sender + ": " + text);
        chatMessages.scrollTo(chatMessages.getItems().size() - 1);
    }

    public void setChatEnabled(boolean enabled) {
        chatInput.setDisable(!enabled);
        sendChat.setDisable(!enabled);
    }

    public boolean isChatInputFocused() {
        return chatInput.isFocused();
    }

    public void clearChatInput() {
        chatInput.clear();
    }

    public void clearChat() {
        chatMessages.getItems().clear();
        clearChatInput();
        setChatError(null);
    }

    public void setChatError(String message) {
        chatError.setText(message);
        chatError.setVisible(message != null && !message.isBlank());
        chatError.setManaged(chatError.isVisible());
    }

    public Parent getChatPanel() { return chatPanel; }

    public void restoreChatPanel() {
        if (chatPanel.getParent() != chatSlot) chatSlot.getChildren().setAll(chatPanel);
    }

    /** 게임 시작을 이미 눌렀는지 표시. 누른 뒤에는 버튼을 잠가 중복 입력을 막는다 */
    public void setReady(boolean ready) {
        start.setText(ready ? START_PRESSED_TEXT : START_TEXT);
        start.setDisable(ready);
        startGuide.setText(ready ? "상대방이 게임 시작을 누르면 대전이 시작됩니다." : "ENTER 또는 클릭 : 게임 시작");
    }

    /** 연결 완료·설정 확정 후에만 버튼 활성화 */
    public void setStartEnabled(boolean enabled) {
        start.setText(START_TEXT);
        start.setDisable(!enabled);
        startGuide.setText(enabled ? "ENTER 또는 클릭 : 게임 시작" : "연결 및 게임 설정 대기 중...");
    }

    public void setLag(long ms, boolean lagging) {
        lag.setText(lagging ? "NETWORK LAG : " + ms + "ms" : "");
    }

    // 화면 상태 조회 (테스트·화면 연결 확인용)
    public Button getStartButton() { return start; }
    public String getStatusText() { return status.getText(); }
    public String getInfoText() { return info.getText(); }
    public String getStartGuideText() { return startGuide.getText(); }
    public String getLagText() { return lag.getText(); }
}
