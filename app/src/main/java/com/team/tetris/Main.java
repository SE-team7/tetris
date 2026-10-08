package com.team.tetris;

import com.team.tetris.controller.GameController;
import com.team.tetris.controller.GameOverController;
import com.team.tetris.controller.MainMenuController;
import com.team.tetris.controller.ScoreboardController;
import com.team.tetris.controller.SettingsController;

import com.team.tetris.domain.Settings;

import com.team.tetris.service.GameEngineService;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.service.SettingsService;

import com.team.tetris.util.FileManager;
import com.team.tetris.util.SceneManager;

import com.team.tetris.view.GameOverView;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        stage.setTitle("Tetris");

        /*
         * Views
         */
        MainMenuView mainMenuView =
            new MainMenuView();

        SettingsView settingsView =
            new SettingsView();

        ScoreboardView scoreboardView =
            new ScoreboardView();

        GameView gameView =
            new GameView();

        GameOverView gameOverView =
            new GameOverView();

        /*
         * File / Services
         */
        FileManager fileManager =
            new FileManager();

        ScoreboardService scoreboardService =
            new ScoreboardService(
                fileManager
            );

        SettingsService settingsService =
            new SettingsService(
                fileManager
            );

        Settings settings =
            settingsService.load();

        GameEngineService gameEngine =
            new GameEngineService();

        /*
         * Scene Manager
         */
        SceneManager sceneManager =
            new SceneManager(
                stage,
                mainMenuView,
                settingsView,
                scoreboardView,
                gameView,
                gameOverView
            );

        sceneManager.applyScreenSize(
            settings.getScreenSize()
        );

        /*
         * Scoreboard Controller
         */
        ScoreboardController scoreboardController =
            new ScoreboardController(
                scoreboardService,
                scoreboardView,
                sceneManager::showScoreboard,
                sceneManager::showMainMenu
            );

        /*
         * Game Over Controller
         */
        GameOverController gameOverController =
            new GameOverController(
                scoreboardService,
                scoreboardController,
                gameOverView,
                sceneManager::showGameOver
            );

        /*
         * Game Controller
         */
        GameController gameController =
            new GameController(
                gameEngine,
                gameView,
                settings,
                gameOverController
            );

        /*
         * Main Menu Controller
         */
        MainMenuController mainMenuController =
            new MainMenuController(
                mainMenuView,
                sceneManager,
                gameController,
                scoreboardController
            );

        /*
         * Settings Controller
         */
        SettingsController settingsController =
            new SettingsController(
                settingsView,
                settingsService,
                scoreboardService,
                sceneManager,
                settings
            );

        /*
         * Game buttons
         */
        gameView.setGameActions(
            gameController::togglePause,
            () -> {
                gameController.quitGame();
                sceneManager.showMainMenu();
            }
        );

        /*
         * Keyboard input
         */
        mainMenuView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    mainMenuController
                        .handleKey(
                            event.getCode()
                        )
            );

        scoreboardView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    scoreboardController
                        .handleKey(
                            event.getCode()
                        )
            );

        gameView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    gameController
                        .handleKey(
                            event.getCode()
                        )
            );

        gameOverView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    gameOverController
                        .handleKey(
                            event.getCode()
                        )
            );

        /*
         * Initial screen
         */
        mainMenuController.show();
    }

    public static void main(
        String[] args
    ) {

        launch(args);
    }
}