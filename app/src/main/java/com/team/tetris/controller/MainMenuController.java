package com.team.tetris.controller;

import com.team.tetris.util.SceneManager;
import com.team.tetris.view.MainMenuView;
import com.team.tetris.view.MainMenuView.MainMenuItem;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class MainMenuController {

    private final MainMenuView mainMenuView;
    private final SceneManager sceneManager;

    private final GameController gameController;
    private final ScoreboardController scoreboardController;

    private MainMenuItem selectedMenu;

    public MainMenuController(
        MainMenuView mainMenuView,
        SceneManager sceneManager,
        GameController gameController,
        ScoreboardController scoreboardController
    ) {
        this.mainMenuView = mainMenuView;
        this.sceneManager = sceneManager;
        this.gameController = gameController;
        this.scoreboardController = scoreboardController;

        this.selectedMenu =
            MainMenuItem.START_GAME;
    }

    public void show() {

        selectedMenu =
            MainMenuItem.START_GAME;

        mainMenuView.render(selectedMenu);

        mainMenuView.showKeyGuide(
            "W/S 또는 ↑/↓",
            "ENTER"
        );

        sceneManager.showMainMenu();
    }

    public void handleKey(KeyCode key) {

        if (key == KeyCode.W
            || key == KeyCode.UP) {

            moveSelection(-1);
            return;
        }

        if (key == KeyCode.S
            || key == KeyCode.DOWN) {

            moveSelection(1);
            return;
        }

        if (key == KeyCode.ENTER) {
            selectMenu(selectedMenu);
        }
    }

    private void moveSelection(
        int direction
    ) {

        MainMenuItem[] items =
            MainMenuItem.values();

        int nextIndex =
            (
                selectedMenu.ordinal()
                + direction
                + items.length
            )
            % items.length;

        selectedMenu =
            items[nextIndex];

        mainMenuView.render(selectedMenu);
    }

    public void selectMenu(
        MainMenuItem menuItem
    ) {

        switch (menuItem) {

            case START_GAME:
                sceneManager.showGame();
                gameController.startGame();
                break;

            case SETTINGS:
                sceneManager.showSettings();
                break;

            case SCOREBOARD:
                scoreboardController.show();
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