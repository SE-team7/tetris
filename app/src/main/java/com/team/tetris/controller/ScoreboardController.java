package com.team.tetris.controller;

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
    private int highlightRank = -1;

    public ScoreboardController(ScoreboardService scoreboardService,
                                ScoreboardView scoreboardView,
                                Runnable showScoreboardScene,
                                Runnable showMainMenuScene) {
        this.scoreboardService = scoreboardService;
        this.scoreboardView = scoreboardView;
        this.showScoreboardScene = showScoreboardScene;
        this.showMainMenuScene = showMainMenuScene;
    }

    /** 메인 메뉴에서 진입할 때. 강조 없이 표시. */
    public void show() {
        show(-1);
    }

    /** 게임오버 후 진입할 때. 방금 등록된 등수를 강조해서 표시. */
    public void show(int highlightRank) {
        this.highlightRank = highlightRank;
        this.selectedMenu = ScoreboardMenu.MAIN_MENU;
        scoreboardView.showKeyGuide("UP / DOWN", "ENTER");
        render();
        showScoreboardScene.run();
    }

    // TODO: Settings.findMenuAction(KeyCode)가 만들어지면 사용자 설정 메뉴 키로 판정하도록 변경
    public void handleKey(KeyCode key) {
        if (key == null) {
            return;
        }
        switch (key) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
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

    private void moveSelection(int delta) {
        ScoreboardMenu[] menus = ScoreboardMenu.values();
        int next = Math.floorMod(selectedMenu.ordinal() + delta, menus.length);
        selectedMenu = menus[next];
        render();
    }

    private void render() {
        scoreboardView.render(scoreboardService.getTopScores(), highlightRank, selectedMenu);
    }
}
