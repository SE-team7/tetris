package com.team.tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 게임오버 화면. SceneManager는 getRoot()로 화면을 가져가 표시한다.
 */
public class GameOverView {

    private static final String HIGHLIGHT_STYLE = "-fx-text-fill: #f5c542; -fx-font-weight: bold;";

    private final VBox root = new VBox(16);
    private final Label resultLabel = new Label();
    private final VBox nameInputBox = new VBox(8);
    private final Label newRecordLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label keyGuide = new Label();

    public GameOverView() {
        Label title = new Label("GAME OVER");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        newRecordLabel.setStyle(HIGHLIGHT_STYLE);
        nameLabel.setStyle("-fx-font-size: 20px;");
        nameInputBox.setAlignment(Pos.CENTER);
        nameInputBox.getChildren().addAll(newRecordLabel, nameLabel);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("game-over");
        root.getChildren().addAll(title, resultLabel, nameInputBox, keyGuide);
    }

    public Parent getRoot() {
        return root;
    }

    /** 최종 결과 표시. 이름 입력 영역은 숨긴다. */
    public void showResult(int score, int level, int clearedLines) {
        resultLabel.setText("점수: " + score + "   레벨: " + level + "   줄: " + clearedLines);
        nameInputBox.setVisible(false);
        nameInputBox.setManaged(false);
        keyGuide.setText("ENTER: 스코어보드 보기");
    }

    /** 스코어보드 진입 시 이름 입력 영역 표시. */
    public void showNameInput(int score) {
        newRecordLabel.setText("새 기록! " + score + "점 - 이름을 입력하세요");
        updateName("");
        nameInputBox.setVisible(true);
        nameInputBox.setManaged(true);
        keyGuide.setText("영문/숫자: 입력   BACKSPACE: 지우기   ENTER: 저장");
    }

    /** 입력 중인 이름 갱신. */
    public void updateName(String name) {
        nameLabel.setText(name + "_");
    }
}
