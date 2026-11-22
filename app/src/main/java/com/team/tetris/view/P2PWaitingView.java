package com.team.tetris.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * P2P 대기실 화면.
 * 요구사항: 연결 완료 후 서버와 클라이언트가 모두 '게임 시작' 버튼을 누르면 대전을 시작한다.
 * 버튼은 마우스 클릭과 Enter 키 모두로 누를 수 있다. (Enter는 P2PWaitingController에서 처리)
 */
public class P2PWaitingView {
    static final String START_TEXT = "게임 시작";
    static final String START_PRESSED_TEXT = "게임 시작 완료";

    private final VBox root = new VBox(18);
    private final Label status = new Label("연결 대기 중...");
    private final Label info = new Label();
    private final Button start = new Button(START_TEXT);
    private final Label startGuide = new Label();
    private final Label lag = new Label();
    private final Label back = new Label("TAB : 뒤로가기");

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

        root.getChildren().addAll(title, status, info, start, startGuide, lag, back);
        setStartEnabled(false);
    }

    public Parent getRoot() { return root; }
    public void setStatus(String text) { status.setText(text); }
    public void setInfo(String text) { info.setText(text == null ? "" : text); }

    /** 게임 시작 버튼을 눌렀을 때 실행할 동작 */
    public void setOnStart(Runnable action) {
        onStart = action == null ? () -> {} : action;
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
