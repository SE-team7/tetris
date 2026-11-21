package com.team.tetris.controller;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Settings;
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
    private final Settings settings;

    private MainMenuItem selectedMenu = MainMenuItem.START_GAME;

    public MainMenuController(
        MainMenuView mainMenuView,
        SceneManager sceneManager,
        GameController gameController,
        ScoreboardController scoreboardController,
        Settings settings
    ) {
        this.mainMenuView = mainMenuView;
        this.sceneManager = sceneManager;
        this.gameController = gameController;
        this.scoreboardController = scoreboardController;
        this.settings = settings;
    }

    public void show() {
        selectedMenu = MainMenuItem.START_GAME;
        mainMenuView.render(selectedMenu);
        mainMenuView.showKeyGuide("↑/↓", "ENTER");
        sceneManager.showMainMenu();
    }

    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }

        switch (key) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case ENTER -> selectMenu(selectedMenu);
            default -> {
            }
        }
    }

    private void moveSelection(int direction) {
        MainMenuItem[] items = MainMenuItem.values();

        int index =
            (selectedMenu.ordinal()
                + direction
                + items.length)
                % items.length;

        selectedMenu = items[index];
        mainMenuView.render(selectedMenu);
    }

    private void selectMenu(MainMenuItem menuItem) {
        switch (menuItem) {
            case START_GAME -> {
                gameController.startGame(
                    GameMode.NORMAL,
                    settings.getDifficulty()
                );

                sceneManager.showGame();
            }

            case START_ITEM_GAME -> {
                gameController.startGame(
                    GameMode.ITEM,
                    Difficulty.EASY
                );

                sceneManager.showGame();
            }

            case BATTLE_MODE ->
                sceneManager.showBattleMode();

            case SETTINGS ->
                sceneManager.showSettings();

            case SCOREBOARD ->
                scoreboardController.show();

            case EXIT ->
                Platform.exit();
        }
    }

    public MainMenuItem getSelectedMenu() {
        return selectedMenu;
    }
}