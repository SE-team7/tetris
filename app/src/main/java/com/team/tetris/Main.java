package com.team.tetris;

import com.team.tetris.controller.MainMenuController;
import com.team.tetris.controller.ScoreboardController;
import com.team.tetris.controller.SettingsController;

import com.team.tetris.service.ScoreboardService;

import com.team.tetris.util.FileManager;
import com.team.tetris.util.SceneManager;

import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.ScoreboardView;
import com.team.tetris.view.SettingsView;

import javafx.application.Application;
import javafx.scene.Scene;
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

        /*
         * Services
         */
        FileManager fileManager =
            new FileManager();

        ScoreboardService scoreboardService =
            new ScoreboardService(fileManager);

        /*
         * Scene Manager
         */
        SceneManager sceneManager =
            new SceneManager(
                stage,
                mainMenuView,
                settingsView,
                scoreboardView
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
         * Temporary game start connection.
         *
         * GameController from member 2 is not
         * present in the current repository yet.
         */
        Runnable startGameAction = () -> {

            Scene gameScene =
                new Scene(gameView.getRoot(), 800, 800);

            var css =
                getClass().getResource("/style.css");

            if (css != null) {
                gameScene
                    .getStylesheets()
                    .add(css.toExternalForm());
            }

            stage.setScene(gameScene);
            stage.show();

            gameView
                .getRoot()
                .requestFocus();
        };

        /*
         * Main Menu Controller
         */
        MainMenuController mainMenuController =
            new MainMenuController(
                mainMenuView,
                sceneManager,
                startGameAction,
                scoreboardController::show
            );

        /*
         * Settings Controller
         *
         * Scoreboard reset can now use the real
         * ScoreboardService.
         *
         * SettingsService does not exist in the
         * current repository yet, so save/reset
         * callbacks remain temporary.
         */
        SettingsController settingsController =
            new SettingsController(
                settingsView,
                sceneManager,
                scoreboardService::resetScoreboard,
                () -> System.out.println(
                    "SettingsService.save() 연결 필요"
                ),
                () -> System.out.println(
                    "SettingsService.resetToDefault() 연결 필요"
                )
            );

        /*
         * Keyboard input
         */
        mainMenuView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    mainMenuController.handleKey(
                        event.getCode()
                    )
            );

        scoreboardView
            .getRoot()
            .setOnKeyPressed(
                event ->
                    scoreboardController.handleKey(
                        event.getCode()
                    )
            );

        /*
         * Keep reference while SettingsController
         * is connected through SettingsView.
         */
        @SuppressWarnings("unused")
        SettingsController controllerReference =
            settingsController;

        /*
         * Initial screen
         */
        mainMenuController.show();

        mainMenuView
            .getRoot()
            .requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}