package com.team.tetris.view;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BattleView {

    private final BorderPane root;
    private final Label timerLabel;
    private final Label statusLabel;
    private Runnable onBackAction;

    public BattleView() {
        root = new BorderPane();
        root.getStyleClass().add("battle-root");

        Label titleLabel = new Label("BATTLE MODE");
        titleLabel.getStyleClass().add("battle-title");

        timerLabel = new Label("TIME 05:00");
        timerLabel.getStyleClass().add("battle-timer");

        statusLabel = new Label("READY");
        statusLabel.getStyleClass().add("battle-status");

        Label backLabel = new Label("ESC : 뒤로가기");
        backLabel.getStyleClass().add("battle-back");

        VBox topBox = new VBox(
            6,
            titleLabel,
            timerLabel,
            statusLabel,
            backLabel
        );

        topBox.setAlignment(Pos.CENTER);
        topBox.getStyleClass().add("battle-top");

        HBox playersBox = new HBox(
            40,
            createPlayerArea("PLAYER 1"),
            createPlayerArea("PLAYER 2")
        );

        playersBox.setAlignment(Pos.CENTER);
        playersBox.getStyleClass().add("battle-players");

        root.setTop(topBox);
        root.setCenter(playersBox);

        root.setFocusTraversable(true);

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (onBackAction != null) {
                    onBackAction.run();
                }

                event.consume();
            }
        });
    }

    private VBox createPlayerArea(String name) {
        Label playerLabel = new Label(name);
        playerLabel.getStyleClass().add("battle-player-title");

        Label boardLabel = new Label();
        boardLabel.getStyleClass().add("battle-board");

        Label nextLabel = new Label("NEXT");
        nextLabel.getStyleClass().add("battle-info-title");

        Label scoreLabel = new Label("SCORE : 0");
        scoreLabel.getStyleClass().add("battle-score");

        Label attackLabel = new Label("INCOMING : 0 / 10");
        attackLabel.getStyleClass().add("battle-attack");

        VBox playerBox = new VBox(
            10,
            playerLabel,
            boardLabel,
            nextLabel,
            scoreLabel,
            attackLabel
        );

        playerBox.setAlignment(Pos.CENTER);
        playerBox.getStyleClass().add("battle-player");

        return playerBox;
    }

    public Parent getRoot() {
        return root;
    }

    public void setTimer(String text) {
        timerLabel.setText(text);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    public void requestFocus() {
        root.requestFocus();
    }
}