package com.team.tetris.controller;

import com.team.tetris.domain.GameMode;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.view.ScoreboardView;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class ScoreboardController {

    private final ScoreboardService service;
    private final ScoreboardView view;
    private final Runnable showScoreboard;
    private final Runnable showMainMenu;

    private ScoreboardMenu selectedMenu = ScoreboardMenu.MAIN_MENU;
    private GameMode currentMode = GameMode.NORMAL;
    private GameMode highlightMode = GameMode.NORMAL;
    private int highlightPosition = -1;

    public ScoreboardController(
        ScoreboardService service,
        ScoreboardView view,
        Runnable showScoreboard,
        Runnable showMainMenu
    ) {
        this.service = service;
        this.view = view;
        this.showScoreboard = showScoreboard;
        this.showMainMenu = showMainMenu;

        view.setOnModeSelected(this::selectMode);
    }

    public void show() {
        show(GameMode.NORMAL, -1);
    }

    public void show(int highlightPosition) {
        show(GameMode.NORMAL, highlightPosition);
    }

    public void show(GameMode mode, int highlightPosition) {
        currentMode = mode == null ? GameMode.NORMAL : mode;
        highlightMode = currentMode;
        this.highlightPosition = highlightPosition;
        selectedMenu = ScoreboardMenu.MAIN_MENU;

        view.showKeyGuide("← / →", "TAB");
        render();
        showScoreboard.run();
    }

    public void handleKey(KeyCode key) {
        if (key == null) return;

        if (key == KeyCode.ESCAPE) {
            Platform.exit();
            return;
        }

        switch (key) {
            case LEFT -> moveMode(-1);
            case RIGHT -> moveMode(1);
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case TAB -> showMainMenu.run();
            default -> {}
        }
    }

    public void selectMenu(ScoreboardMenu menu) {
        if (menu == ScoreboardMenu.MAIN_MENU) {
            showMainMenu.run();
        }
    }

    public void selectMode(GameMode mode) {
        if (mode == null) return;

        currentMode = mode;
        render();
    }

    private void moveMode(int direction) {
        GameMode[] modes = GameMode.values();

        currentMode = modes[
            Math.floorMod(
                currentMode.ordinal() + direction,
                modes.length
            )
        ];

        render();
    }

    private void moveSelection(int direction) {
        ScoreboardMenu[] menus = ScoreboardMenu.values();

        selectedMenu = menus[
            Math.floorMod(
                selectedMenu.ordinal() + direction,
                menus.length
            )
        ];

        render();
    }

    private void render() {
        int highlight =
            currentMode == highlightMode
                ? highlightPosition
                : -1;

        view.render(
            service.getTopScores(currentMode),
            highlight,
            selectedMenu,
            currentMode
        );
    }
}