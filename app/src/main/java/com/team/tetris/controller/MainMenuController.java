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

    private final MainMenuView view;
    private final SceneManager scenes;
    private final GameController gameController;
    private final ScoreboardController scoreboardController;
    private final Settings settings;

    private MainMenuItem selected = MainMenuItem.START_GAME;

    public MainMenuController(
        MainMenuView view,
        SceneManager scenes,
        GameController gameController,
        ScoreboardController scoreboardController,
        Settings settings
    ) {
        this.view = view;
        this.scenes = scenes;
        this.gameController = gameController;
        this.scoreboardController = scoreboardController;
        this.settings = settings;
    }

    public void show() {
        selected = MainMenuItem.START_GAME;
        view.render(selected);
        view.showKeyGuide("↑/↓", "ENTER");
        scenes.showMainMenu();
    }

    public void handleKey(KeyCode key) {
        if (key == null) return;

        switch (key) {
            case UP -> move(-1);
            case DOWN -> move(1);
            case ENTER -> select();
            default -> {}
        }
    }

    private void move(int direction) {
        MainMenuItem[] items = MainMenuItem.values();
        selected = items[Math.floorMod(selected.ordinal() + direction, items.length)];
        view.render(selected);
    }

    private void select() {
        switch (selected) {
            case START_GAME -> {
                gameController.startGame(GameMode.NORMAL, settings.getDifficulty());
                scenes.showGame();
            }
            case START_ITEM_GAME -> {
                gameController.startGame(GameMode.ITEM, Difficulty.EASY);
                scenes.showGame();
            }
            case BATTLE_MODE -> scenes.showBattleType();
            case SETTINGS -> scenes.showSettings();
            case SCOREBOARD -> scoreboardController.show();
            case EXIT -> Platform.exit();
        }
    }

    public MainMenuItem getSelectedMenu() {
        return selected;
    }
}