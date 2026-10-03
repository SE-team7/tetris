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

    private static final int ROWS = 25;
    private static final int COLS = 10;
    private static final double CELL_SIZE = 25;

    private final BorderPane root;
    private final GridPane boardGrid;

    private final Label scoreLabel;
    private final Label pauseLabel;
    private final Label nextBlockLabel;

    public GameView() {
        root = new BorderPane();
        root.setPadding(new Insets(20));

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        createEmptyBoard();

        Label nextTitle = new Label("NEXT");
        nextTitle.getStyleClass().add("title");

        nextBlockLabel = new Label("다음 블록");

        Label scoreTitle = new Label("SCORE");
        scoreTitle.getStyleClass().add("title");

        scoreLabel = new Label("0");
        pauseLabel = new Label();

        VBox sidePanel = new VBox(
            15,
            nextTitle,
            nextBlockLabel,
            scoreTitle,
            scoreLabel,
            pauseLabel
        );

        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPadding(new Insets(20));

        root.setCenter(boardGrid);
        root.setRight(sidePanel);
    }

    private void createEmptyBoard() {
        boardGrid.getChildren().clear();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);

                cell.setStyle(
                    "-fx-fill: white;" +
                    "-fx-stroke: lightgray;"
                );

                boardGrid.add(cell, col, row);
            }
        }
    }

    public Parent getRoot() {
        return root;
    }

    public void updateScore(int score) {
        scoreLabel.setText(String.valueOf(score));
    }

    public void setPaused(boolean paused) {
        if (paused) {
            pauseLabel.setText("PAUSED");
        } else {
            pauseLabel.setText("");
        }
    }

    public void setNextBlockText(String text) {
        nextBlockLabel.setText(text);
    }
}