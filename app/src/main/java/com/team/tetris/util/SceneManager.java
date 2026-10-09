package com.team.tetris.util;

import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage stage;

    private final MainMenuView mainMenuView;
    private final SettingsView settingsView;
    private final ScoreboardView scoreboardView;
    private final GameView gameView;
    private final GameOverView gameOverView;

    private final Scene mainMenuScene;
    private final Scene settingsScene;
    private final Scene scoreboardScene;
    private final Scene gameScene;
    private final Scene gameOverScene;

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView,
        ScoreboardView scoreboardView,
        GameView gameView,
        GameOverView gameOverView
    ) {
        this.stage = stage;

        this.mainMenuView = mainMenuView;
        this.settingsView = settingsView;
        this.scoreboardView = scoreboardView;
        this.gameView = gameView;
        this.gameOverView = gameOverView;

        this.mainMenuScene =
            createScene(mainMenuView.getRoot());

        this.settingsScene =
            createScene(settingsView.getRoot());

        this.scoreboardScene =
            createScene(scoreboardView.getRoot());

        this.gameScene =
            createScene(gameView.getRoot());

        this.gameOverScene =
            createScene(gameOverView.getRoot());
    }

    public void showMainMenu() {
        stage.setScene(mainMenuScene);
        stage.show();

        Platform.runLater(
            () -> mainMenuView.getRoot().requestFocus()
        );
    }

    public void showSettings() {
        stage.setScene(settingsScene);
        stage.show();

        Platform.runLater(
            () -> settingsView.getRoot().requestFocus()
        );
    }

    public void showScoreboard() {
        stage.setScene(scoreboardScene);
        stage.show();

        Platform.runLater(
            () -> scoreboardView.getRoot().requestFocus()
        );
    }

    public void showGame() {
        stage.setScene(gameScene);
        stage.show();

        Platform.runLater(
            () -> gameView.getRoot().requestFocus()
        );
    }

    public void showGameOver() {
        stage.setScene(gameOverScene);
        stage.show();

        Platform.runLater(
            () -> gameOverView.getRoot().requestFocus()
        );
    }

    public void applyScreenSize(ScreenSize screenSize) {

        if (screenSize == null) {
            return;
        }

        stage.setWidth(screenSize.getWidth());
        stage.setHeight(screenSize.getHeight());
    }

    private Scene createScene(Parent root) {

        Scene scene =
            new Scene(root, 800, 800);

        var css =
            getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets()
                .add(css.toExternalForm());
        }

        return scene;
    }

    public Stage getStage() {
        return stage;
    }
}