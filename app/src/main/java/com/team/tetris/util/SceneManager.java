package com.team.tetris.util;

import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.SettingsView;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage stage;

    private final MainMenuView mainMenuView;
    private final SettingsView settingsView;

    private final Scene mainMenuScene;
    private final Scene settingsScene;

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView
    ) {
        this.stage = stage;
        this.mainMenuView = mainMenuView;
        this.settingsView = settingsView;

        this.mainMenuScene = createScene(mainMenuView.getRoot());
        this.settingsScene = createScene(settingsView.getRoot());
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

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root, 800, 800);

        var css = getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        return scene;
    }

    public Stage getStage() {
        return stage;
    }
}