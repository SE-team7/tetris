package com.team.tetris.util;

import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.SettingsView;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage stage;
    private final MainMenuView mainMenuView;
    private final SettingsView settingsView;

    public SceneManager(
        Stage stage,
        MainMenuView mainMenuView,
        SettingsView settingsView
    ) {
        this.stage = stage;
        this.mainMenuView = mainMenuView;
        this.settingsView = settingsView;
    }

    public void showMainMenu() {
        showScene(mainMenuView.getRoot());
    }

    public void showSettings() {
        showScene(settingsView.getRoot());
    }

    private void showScene(Parent root) {
        Scene scene = new Scene(root);

        var css = getClass().getResource("/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }
}
