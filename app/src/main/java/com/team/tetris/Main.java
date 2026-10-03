package com.team.tetris;

import com.team.tetris.controller.MainMenuController;
import com.team.tetris.controller.SettingsController;
import com.team.tetris.util.SceneManager;
import com.team.tetris.view.GameView;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.SettingsView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        stage.setTitle("Tetris");

        MainMenuView mainMenuView = new MainMenuView();
        SettingsView settingsView = new SettingsView();
        GameView gameView = new GameView();

        SceneManager sceneManager =
            new SceneManager(stage, mainMenuView, settingsView);

        Runnable startGameAction = () -> {
            Scene gameScene = new Scene(gameView.getRoot());

            var css = getClass().getResource("/style.css");
            if (css != null) {
                gameScene.getStylesheets().add(css.toExternalForm());
            }

            stage.setScene(gameScene);
            stage.show();
        };

        Runnable showScoreboardAction = () ->
            System.out.println(
                "ScoreboardController.show() 연결 예정"
            );

        MainMenuController mainMenuController =
            new MainMenuController(
                mainMenuView,
                sceneManager,
                startGameAction,
                showScoreboardAction
            );

        SettingsController settingsController =
            new SettingsController(
                settingsView,
                sceneManager,
                () -> System.out.println(
                    "ScoreboardService.resetScoreboard() 연결 예정"
                ),
                () -> System.out.println(
                    "SettingsService.save() 연결 예정"
                ),
                () -> System.out.println(
                    "SettingsService.resetToDefault() 연결 예정"
                )
            );

      
       mainMenuView.getRoot().setOnKeyPressed(
    event -> mainMenuController.handleKey(event.getCode())
);

        @SuppressWarnings("unused")
        SettingsController controllerReference =
            settingsController;

        mainMenuController.show();

        mainMenuView.getRoot().requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}