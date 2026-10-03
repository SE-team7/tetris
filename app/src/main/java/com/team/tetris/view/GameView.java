package com.team.tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class GameView {

    // 팀 회의에서 정한 내부 보드 크기
    private static final int ROWS = 25;
    private static final int COLS = 10;

    private static final double CELL_SIZE = 22;

    private final BorderPane root;
    private final GridPane boardGrid;
    private final GridPane nextBlockGrid;

    private final Label scoreLabel;
    private final Label pauseLabel;

    public GameView() {

        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #202020;");

        /*
         * BOARD
         */
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        createEmptyBoard();

        /*
         * NEXT BLOCK
         */
        Label nextTitle = new Label("NEXT");
        nextTitle.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );

        nextBlockGrid = new GridPane();
        nextBlockGrid.setAlignment(Pos.CENTER);

        createEmptyNextBlock();

        /*
         * SCORE
         */
        Label scoreTitle = new Label("SCORE");
        scoreTitle.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );

        scoreLabel = new Label("0");
        scoreLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 24px;"
        );

        /*
         * PAUSE
         */
        pauseLabel = new Label();
        pauseLabel.setStyle(
            "-fx-text-fill: yellow;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );

        VBox sidePanel = new VBox(
            20,
            nextTitle,
            nextBlockGrid,
            scoreTitle,
            scoreLabel,
            pauseLabel
        );

        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPadding(new Insets(20));
        sidePanel.setPrefWidth(180);

        root.setCenter(boardGrid);
        root.setRight(sidePanel);
    }

    private void createEmptyBoard() {

        boardGrid.getChildren().clear();

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                Rectangle cell =
                    new Rectangle(CELL_SIZE, CELL_SIZE);

                cell.setStyle(
                    "-fx-fill: #111111;" +
                    "-fx-stroke: #555555;" +
                    "-fx-stroke-width: 0.5;"
                );

                boardGrid.add(cell, col, row);
            }
        }
    }

    private void createEmptyNextBlock() {

        nextBlockGrid.getChildren().clear();

        for (int row = 0; row < 4; row++) {

            for (int col = 0; col < 4; col++) {

                Rectangle cell =
                    new Rectangle(20, 20);

                cell.setStyle(
                    "-fx-fill: #111111;" +
                    "-fx-stroke: #555555;"
                );

                nextBlockGrid.add(cell, col, row);
            }
        }
    }

    public Parent getRoot() {
        return root;
    }

    public void updateScore(int score) {
        scoreLabel.setText(
            String.valueOf(score)
        );
    }

    public void setPaused(boolean paused) {

        if (paused) {
            pauseLabel.setText("PAUSED");
        } else {
            pauseLabel.setText("");
        }
    }

    /*
     * 이후 BlockType / currentBlock API 연결 시
     * 실제 블록을 Board 위에 렌더링할 예정
     */
}