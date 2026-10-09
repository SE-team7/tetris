package com.team.tetris.service;

public class ScoreService {

    //줄 한번에 여러개 지울때의 점수
    private static final int[] LINE_CLEAR_SCORES = {0, 10, 30, 50, 80};

    //단계당 보너스 점수
    private static final int SPEED_BONUS_PER_LEVEL = 10;

    //스킵 1회당 감점
    private static final int SKIP_PENALTY = 50;

    private int lineScore;
    private int speedBonusScore;
    private int dropScore;
    private int skipPenalty;

    public void reset() {
        lineScore = 0;
        speedBonusScore = 0;
        dropScore = 0;
        skipPenalty = 0;
    }

    public void applySkipPenalty() {
        skipPenalty += SKIP_PENALTY;
    }

    public int calculateLineClearScore(int clearedLines) {
        if (clearedLines <= 0) {
            return 0;
        }
        int index = Math.min(clearedLines, LINE_CLEAR_SCORES.length - 1);
        return LINE_CLEAR_SCORES[index];
    }

    public void addLineScore(int delta) {
        lineScore += delta;
    }

    public void addSpeedBonus(int levelUpCount) {
        speedBonusScore += levelUpCount * SPEED_BONUS_PER_LEVEL;
    }

    public void addDropScore(int cells) {
        dropScore += cells;
    }

    public int getLineScore() {
        return lineScore;
    }

    public int getScore() {
        return lineScore + speedBonusScore + dropScore - skipPenalty;
    }
}