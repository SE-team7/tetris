package com.team.tetris.controller;

import com.team.tetris.domain.GameMode;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.view.ScoreboardView;

import javafx.scene.input.KeyCode;

/**
 * 스코어보드 화면 진행 담당.
 *
 * 화면 전환은 SceneManager(3번)가 담당하므로, 생성 시 전환 동작을 전달받는다.
 * 예) new ScoreboardController(service, view, sceneManager::showScoreboard, sceneManager::showMainMenu)
 */
public class ScoreboardController {

    private final ScoreboardService scoreboardService;
    private final ScoreboardView scoreboardView;
    private final Runnable showScoreboardScene;
    private final Runnable showMainMenuScene;

    private ScoreboardMenu selectedMenu = ScoreboardMenu.MAIN_MENU;
    private GameMode currentMode = GameMode.NORMAL;
    // 강조할 기록: 어느 모드 목록의 몇 번째 줄인지
    private GameMode highlightMode = GameMode.NORMAL;
    private int highlightPosition = -1;

    public ScoreboardController(ScoreboardService scoreboardService,
                                ScoreboardView scoreboardView,
                                Runnable showScoreboardScene,
                                Runnable showMainMenuScene) {
        this.scoreboardService = scoreboardService;
        this.scoreboardView = scoreboardView;
        this.showScoreboardScene = showScoreboardScene;
        this.showMainMenuScene = showMainMenuScene;
        this.scoreboardView.setOnModeSelected(this::selectMode);
    }

    /** 메인 메뉴에서 진입할 때. 일반 모드, 강조 없이 표시. */
    public void show() {
        show(GameMode.NORMAL, -1);
    }

    /** 일반 모드 기록 중 해당 줄을 강조해서 표시. */
    public void show(int highlightPosition) {
        show(GameMode.NORMAL, highlightPosition);
    }

    /** 게임오버 후 진입할 때. 방금 등록된 기록의 모드를 열고 그 줄을 강조해서 표시. */
    public void show(GameMode gameMode, int highlightPosition) {
        if (gameMode == null) {
            gameMode = GameMode.NORMAL;
        }
        this.currentMode = gameMode;
        this.highlightMode = gameMode;
        this.highlightPosition = highlightPosition;
        this.selectedMenu = ScoreboardMenu.MAIN_MENU;
        scoreboardView.showKeyGuide("UP / DOWN", "ENTER");
        render();
        showScoreboardScene.run();
    }

    // 메뉴 조작키는 설정 대상이 아니므로 방향키·ENTER·ESC 고정
    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }
        switch (key) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case LEFT -> moveMode(-1);
            case RIGHT -> moveMode(1);
            case ENTER -> selectMenu(selectedMenu);
            case ESCAPE -> selectMenu(ScoreboardMenu.MAIN_MENU);
            default -> { }
        }
    }

    public void selectMenu(ScoreboardMenu menu) {
        switch (menu) {
            case MAIN_MENU -> showMainMenuScene.run();
        }
    }

    /** 보여줄 게임 모드 선택 (모드 버튼 클릭 또는 LEFT / RIGHT 키). */
    public void selectMode(GameMode gameMode) {
        if (gameMode == null) {
            return;
        }
        this.currentMode = gameMode;
        render();
    }

    private void moveMode(int delta) {
        GameMode[] modes = GameMode.values();
        selectMode(modes[Math.floorMod(currentMode.ordinal() + delta, modes.length)]);
    }

    private void moveSelection(int delta) {
        ScoreboardMenu[] menus = ScoreboardMenu.values();
        int next = Math.floorMod(selectedMenu.ordinal() + delta, menus.length);
        selectedMenu = menus[next];
        render();
    }

    private void render() {
        int highlight = currentMode == highlightMode ? highlightPosition : -1;
        scoreboardView.render(scoreboardService.getTopScores(currentMode), highlight, selectedMenu, currentMode);
    }
}
