package com.team.tetris.controller;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.service.SettingsService;
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
    private final SettingsService settingsService;

    private MainMenuItem selectedMenu;

    public MainMenuController(
        MainMenuView mainMenuView,
        SceneManager sceneManager,
        GameController gameController,
        ScoreboardController scoreboardController,
        SettingsService settingsService
    ) {
        this.mainMenuView = mainMenuView;
        this.sceneManager = sceneManager;
        this.gameController = gameController;
        this.scoreboardController = scoreboardController;
        this.settingsService = settingsService;

        this.selectedMenu =
            MainMenuItem.START_GAME;
    }

    public void show() {

        selectedMenu =
            MainMenuItem.START_GAME;

        mainMenuView.render(
            selectedMenu
        );

        mainMenuView.showKeyGuide(
            "W/S 또는 ↑/↓",
            "ENTER"
        );

        sceneManager.showMainMenu();
    }

    public void handleKey(
        KeyCode key
    ) {

        if (
            key == KeyCode.W
            || key == KeyCode.UP
        ) {

            moveSelection(-1);
            return;
        }

        if (
            key == KeyCode.S
            || key == KeyCode.DOWN
        ) {

            moveSelection(1);
            return;
        }

        if (
            key == KeyCode.ENTER
        ) {

            selectMenu(
                selectedMenu
            );
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

        mainMenuView.render(
            selectedMenu
        );
    }

    public void selectMenu(
        MainMenuItem menuItem
    ) {

        switch (menuItem) {

            /*
             * 일반 모드
             * Settings에 저장된 난이도 사용
             */
            case START_GAME:

                sceneManager.showGame();

                gameController.startGame(
                    GameMode.NORMAL,
                    settingsService.getDifficulty()
                );

                break;

            /*
             * 아이템 모드
             * 난이도 NORMAL 고정
             */
            case START_ITEM_GAME:

                sceneManager.showGame();

                gameController.startGame(
                    GameMode.ITEM,
                    Difficulty.NORMAL
                );

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