package com.team.tetris.domain;

/**
 * 스코어보드에 저장되는 기록 1건.
 */
public class Score {

    private final String name;
    private final int score;
    private final int level;
    private final int clearedLines;
    private final Difficulty difficulty;
    private final GameMode gameMode;

    /** 난이도·모드 정보가 없는 기록은 NORMAL 난이도, 일반 모드로 본다. */
    public Score(String name, int score, int level, int clearedLines) {
        this(name, score, level, clearedLines, Difficulty.NORMAL, GameMode.NORMAL);
    }

    public Score(String name, int score, int level, int clearedLines,
                 Difficulty difficulty, GameMode gameMode) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.clearedLines = clearedLines;
        this.difficulty = difficulty != null ? difficulty : Difficulty.NORMAL;
        this.gameMode = gameMode != null ? gameMode : GameMode.NORMAL;
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

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}
