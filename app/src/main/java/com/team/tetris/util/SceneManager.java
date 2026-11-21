package com.team.tetris.util;

import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.view.BattleModeView;
import com.team.tetris.view.BattleTypeView;
import com.team.tetris.view.BattleView;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.P2PConnectionView;
import com.team.tetris.view.P2PWaitingView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;
import com.team.tetris.view.TimeLimitView;

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

    private final BattleModeView battleModeView;
    private final BattleTypeView battleTypeView;
    private final TimeLimitView timeLimitView;
    private final P2PConnectionView p2pConnectionView;
    private final P2PWaitingView p2pWaitingView;
    private final BattleView battleView;

    private final Scene mainMenuScene;
    private final Scene settingsScene;
    private final Scene scoreboardScene;
    private final Scene gameScene;
    private final Scene gameOverScene;

    private final Scene battleModeScene;
    private final Scene battleTypeScene;
    private final Scene timeLimitScene;
    private final Scene p2pConnectionScene;
    private final Scene p2pWaitingScene;
    private final Scene battleScene;

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView,
        ScoreboardView scoreboardView,
        GameView gameView,
        GameOverView gameOverView
    ) {
        this(
            stage,
            mainMenuView,
            settingsView,
            scoreboardView,
            gameView,
            gameOverView,
            new BattleModeView(),
            new BattleTypeView(),
            new TimeLimitView(),
            new P2PConnectionView(),
            new P2PWaitingView(),
            new BattleView()
        );
    }

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView,
        ScoreboardView scoreboardView,
        GameView gameView,
        GameOverView gameOverView,
        BattleView battleView
    ) {
        this(
            stage,
            mainMenuView,
            settingsView,
            scoreboardView,
            gameView,
            gameOverView,
            new BattleModeView(),
            new BattleTypeView(),
            new TimeLimitView(),
            new P2PConnectionView(),
            new P2PWaitingView(),
            battleView
        );
    }

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView,
        ScoreboardView scoreboardView,
        GameView gameView,
        GameOverView gameOverView,
        BattleModeView battleModeView,
        BattleTypeView battleTypeView,
        TimeLimitView timeLimitView,
        P2PConnectionView p2pConnectionView,
        P2PWaitingView p2pWaitingView,
        BattleView battleView
    ) {
        this.stage = stage;

        this.mainMenuView = mainMenuView;
        this.settingsView = settingsView;
        this.scoreboardView = scoreboardView;
        this.gameView = gameView;
        this.gameOverView = gameOverView;

        this.battleModeView = battleModeView;
        this.battleTypeView = battleTypeView;
        this.timeLimitView = timeLimitView;
        this.p2pConnectionView = p2pConnectionView;
        this.p2pWaitingView = p2pWaitingView;
        this.battleView = battleView;

        mainMenuScene = createScene(mainMenuView.getRoot());
        settingsScene = createScene(settingsView.getRoot());
        scoreboardScene = createScene(scoreboardView.getRoot());
        gameScene = createScene(gameView.getRoot());
        gameOverScene = createScene(gameOverView.getRoot());

        battleModeScene = createScene(battleModeView.getRoot());
        battleTypeScene = createScene(battleTypeView.getRoot());
        timeLimitScene = createScene(timeLimitView.getRoot());
        p2pConnectionScene = createScene(p2pConnectionView.getRoot());
        p2pWaitingScene = createScene(p2pWaitingView.getRoot());
        battleScene = createScene(battleView.getRoot());
    }

    public void showMainMenu() {
        show(mainMenuScene, mainMenuView.getRoot());
    }

    public void showSettings() {
        show(settingsScene, settingsView.getRoot());
    }

    public void showScoreboard() {
        show(scoreboardScene, scoreboardView.getRoot());
    }

    public void showGame() {
        show(gameScene, gameView.getRoot());
    }

    public void showGameOver() {
        show(gameOverScene, gameOverView.getRoot());
    }

    public void showBattleMode() {
        show(battleModeScene, battleModeView.getRoot());
    }

    public void showBattleType() {
        show(battleTypeScene, battleTypeView.getRoot());
    }

    public void showTimeLimit() {
        show(timeLimitScene, timeLimitView.getRoot());
    }

    public void showP2PConnection() {
        show(p2pConnectionScene, p2pConnectionView.getRoot());
    }

    public void showP2PWaiting() {
        show(p2pWaitingScene, p2pWaitingView.getRoot());
    }

    public void showBattle() {
        show(battleScene, battleView.getRoot());
    }

    private void show(Scene scene, Parent root) {
        stage.setScene(scene);
        stage.show();
        Platform.runLater(root::requestFocus);
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root);

        var css = getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        return scene;
    }

    public void applyScreenSize(ScreenSize screenSize) {
        if (screenSize == null) {
            return;
        }

        stage.setWidth(screenSize.getWidth());
        stage.setHeight(screenSize.getHeight());
        stage.centerOnScreen();
    }

    public Stage getStage() {
        return stage;
    }
}