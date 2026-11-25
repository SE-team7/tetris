package com.team.tetris.util;

import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.domain.Settings.Theme;
import com.team.tetris.view.*;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage stage;

    private final Parent mainMenuRoot;
    private final Parent settingsRoot;
    private final Parent scoreboardRoot;
    private final Parent gameRoot;
    private final Parent gameOverRoot;

    private final Parent battleModeRoot;
    private final Parent battleTypeRoot;
    private final Parent timeLimitRoot;
    private final Parent p2pConnectionRoot;
    private final Parent p2pWaitingRoot;
    private final Parent battleRoot;

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

    private Theme theme = Theme.LIGHT;

    public SceneManager(
        Stage stage,
        MainMenuView mainMenu,
        SettingsView settings,
        ScoreboardView scoreboard,
        GameView game,
        GameOverView gameOver
    ) {
        this(
            stage,
            mainMenu,
            settings,
            scoreboard,
            game,
            gameOver,
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
        MainMenuView mainMenu,
        SettingsView settings,
        ScoreboardView scoreboard,
        GameView game,
        GameOverView gameOver,
        BattleView battle
    ) {
        this(
            stage,
            mainMenu,
            settings,
            scoreboard,
            game,
            gameOver,
            new BattleModeView(),
            new BattleTypeView(),
            new TimeLimitView(),
            new P2PConnectionView(),
            new P2PWaitingView(),
            battle
        );
    }

    public SceneManager(
        Stage stage,
        MainMenuView mainMenu,
        SettingsView settings,
        ScoreboardView scoreboard,
        GameView game,
        GameOverView gameOver,
        BattleModeView battleMode,
        BattleTypeView battleType,
        TimeLimitView timeLimit,
        P2PConnectionView p2pConnection,
        P2PWaitingView p2pWaiting,
        BattleView battle
    ) {
        this.stage = stage;

        mainMenuRoot = mainMenu.getRoot();
        settingsRoot = settings.getRoot();
        scoreboardRoot = scoreboard.getRoot();
        gameRoot = game.getRoot();
        gameOverRoot = gameOver.getRoot();

        battleModeRoot = battleMode.getRoot();
        battleTypeRoot = battleType.getRoot();
        timeLimitRoot = timeLimit.getRoot();
        p2pConnectionRoot = p2pConnection.getRoot();
        p2pWaitingRoot = p2pWaiting.getRoot();
        battleRoot = battle.getRoot();

        mainMenuScene = scene(mainMenuRoot);
        settingsScene = scene(settingsRoot);
        scoreboardScene = scene(scoreboardRoot);
        gameScene = scene(gameRoot);
        gameOverScene = scene(gameOverRoot);

        battleModeScene = scene(battleModeRoot);
        battleTypeScene = scene(battleTypeRoot);
        timeLimitScene = scene(timeLimitRoot);
        p2pConnectionScene = scene(p2pConnectionRoot);
        p2pWaitingScene = scene(p2pWaitingRoot);
        battleScene = scene(battleRoot);

        applyTheme(Theme.LIGHT);
    }

    public void showMainMenu() {
        show(mainMenuScene, mainMenuRoot);
    }

    public void showSettings() {
        show(settingsScene, settingsRoot);
    }

    public void showScoreboard() {
        show(scoreboardScene, scoreboardRoot);
    }

    public void showGame() {
        menuTheme(gameRoot);
        show(gameScene, gameRoot);
    }

    public void showGameOver() {
        menuTheme(gameOverRoot);
        show(gameOverScene, gameOverRoot);
    }

    public void showBattleMode() {
        menuTheme(battleModeRoot);
        show(battleModeScene, battleModeRoot);
    }

    public void showBattleType() {
        menuTheme(battleTypeRoot);
        show(battleTypeScene, battleTypeRoot);
    }

    public void showTimeLimit() {
        menuTheme(timeLimitRoot);
        show(timeLimitScene, timeLimitRoot);
    }

    public void showP2PConnection() {
        menuTheme(p2pConnectionRoot);
        show(p2pConnectionScene, p2pConnectionRoot);
    }

    public void showP2PWaiting() {
        menuTheme(p2pWaitingRoot);
        show(p2pWaitingScene, p2pWaitingRoot);
    }

    public void showBattle() {
        menuTheme(battleRoot);
        show(battleScene, battleRoot);
    }

    public void applyTheme(Theme value) {
        theme = value == null ? Theme.LIGHT : value;

        menuTheme(mainMenuRoot);
        menuTheme(settingsRoot);
        menuTheme(scoreboardRoot);
        menuTheme(battleModeRoot);
        menuTheme(battleTypeRoot);
        menuTheme(timeLimitRoot);
        menuTheme(p2pConnectionRoot);
        menuTheme(p2pWaitingRoot);

        menuTheme(gameRoot);
        menuTheme(gameOverRoot);
        menuTheme(battleRoot);
    }

    private void menuTheme(Parent root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(
            theme == Theme.DARK ? "theme-dark" : "theme-light"
        );
    }

    private Scene scene(Parent root) {
        Scene scene = new Scene(root);
        var css = getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        return scene;
    }

    private void show(Scene scene, Parent root) {
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> {
            if (stage.getScene() == scene) root.requestFocus();
        });
    }

    public void applyScreenSize(ScreenSize size) {
        if (size == null) return;

        stage.setWidth(size.getWidth());
        stage.setHeight(size.getHeight());
        stage.centerOnScreen();
    }

    public Stage getStage() {
        return stage;
    }
}