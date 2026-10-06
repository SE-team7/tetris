package com.team.tetris.controller;

/**
 * 스코어보드 화면에서 선택 가능한 메뉴.
 */
public enum ScoreboardMenu {
    MAIN_MENU("메인 메뉴");

    private final String label;

    ScoreboardMenu(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
