package com.team.tetris.controller;

import com.team.tetris.util.SceneManager;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.MainMenuView.MainMenuItem;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class MainMenuController {

    private final MainMenuView mainMenuView;
    private final SceneManager sceneManager;

    private final Runnable startGameAction;
    private final Runnable showScoreboardAction;

    private MainMenuItem selectedMenu;

    public MainMenuController(
        MainMenuView mainMenuView,
        SceneManager sceneManager,
        Runnable startGameAction,
        Runnable showScoreboardAction
    ) {
        this.mainMenuView = mainMenuView;
        this.sceneManager = sceneManager;
        this.startGameAction = startGameAction;
        this.showScoreboardAction = showScoreboardAction;

        this.selectedMenu = MainMenuItem.START_GAME;
    }

    public void show() {
        selectedMenu = MainMenuItem.START_GAME;

        mainMenuView.render(selectedMenu);
        mainMenuView.showKeyGuide(
            "W/S 또는 ↑/↓",
            "ENTER"
        );

        sceneManager.showMainMenu();
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.W || key == KeyCode.UP) {
            moveSelection(-1);
            return;
        }

        if (key == KeyCode.S || key == KeyCode.DOWN) {
            moveSelection(1);
            return;
        }

        if (key == KeyCode.ENTER) {
            selectMenu(selectedMenu);
        }
    }

    private void moveSelection(int direction) {
        MainMenuItem[] items = MainMenuItem.values();

        int nextIndex =
            (selectedMenu.ordinal() + direction + items.length)
            % items.length;

        selectedMenu = items[nextIndex);
        mainMenuView.render(selectedMenu);
    }

    public void selectMenu(MainMenuItem menuItem) {
        switch (menuItem) {
            case START_GAME:
                if (startGameAction != null) {
                    startGameAction.run();
                }
                break;

            case SETTINGS:
                sceneManager.showSettings();
                break;

            case SCOREBOARD:
                if (showScoreboardAction != null) {
                    showScoreboardAction.run();
                }
                break;

            case EXIT:
                Platform.exit();
                break;
        }
    }

    public MainMenuItem getSelectedMenu() {
        return selectedMenu;
    }
}