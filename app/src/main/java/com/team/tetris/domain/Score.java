package com.team.tetris.domain;

/**
 * 스코어보드에 저장되는 기록 1건.
 */
public class Score {

    private final String name;
    private final int score;
    private final int level;
    private final int clearedLines;

    public Score(String name, int score, int level, int clearedLines) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.clearedLines = clearedLines;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getClearedLines() {
        return clearedLines;
    }
}
