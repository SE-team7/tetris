package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;

class ScoreServiceTest {

    @Test
    void newInstanceHasZeroScoreBeforeReset() {
        ScoreService service = new ScoreService();

        assertEquals(0, service.getScore());
        assertEquals(0, service.getLineScore());
    }

    @Test
    void defaultResetGivesNormalStartingScore() {
        ScoreService service = new ScoreService();
        service.reset();

        assertEquals(150, service.getLineScore());
        assertEquals(150, service.getScore());
    }

    @Test
    void easyStartsAtZero() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        assertEquals(0, service.getLineScore());
    }

    @Test
    void normalStartsAt150() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.NORMAL);

        assertEquals(150, service.getLineScore());
    }

    @Test
    void hardStartsAt300() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.HARD);

        assertEquals(300, service.getLineScore());
    }

    @Test
    void resetClearsAccumulatedPoints() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);
        service.addLineScore(500);
        service.addSpeedBonus(5);
        service.addDropScore(100);
        service.applySkipPenalty();

        service.reset(Difficulty.NORMAL);

        assertEquals(150, service.getScore());
    }

    @Test
    void calculateLineClearScoreZeroReturnsZero() {
        ScoreService service = new ScoreService();

        assertEquals(0, service.calculateLineClearScore(0));
    }

    @Test
    void calculateLineClearScoreNegativeReturnsZero() {
        ScoreService service = new ScoreService();

        assertEquals(0, service.calculateLineClearScore(-5));
    }

    @Test
    void calculateLineClearScoreForEachLineCount() {
        ScoreService service = new ScoreService();

        assertEquals(10, service.calculateLineClearScore(1));
        assertEquals(30, service.calculateLineClearScore(2));
        assertEquals(50, service.calculateLineClearScore(3));
        assertEquals(80, service.calculateLineClearScore(4));
    }

    @Test
    void calculateLineClearScoreClampsAboveTetris() {
        // 테트리스(4줄) 이상은 모두 80점에 상한
        ScoreService service = new ScoreService();

        assertEquals(80, service.calculateLineClearScore(5));
        assertEquals(80, service.calculateLineClearScore(100));
    }

    @Test
    void addLineScoreAccumulates() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addLineScore(10);
        service.addLineScore(30);

        assertEquals(40, service.getLineScore());
    }

    @Test
    void addSpeedBonusAddsTenPerLevelUp() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addSpeedBonus(3);

        assertEquals(30, service.getScore());
    }

    @Test
    void addSpeedBonusWithZeroDoesNothing() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addSpeedBonus(0);

        assertEquals(0, service.getScore());
    }

    @Test
    void addDropScoreAccumulates() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addDropScore(5);
        service.addDropScore(3);

        assertEquals(8, service.getScore());
    }

    @Test
    void skipPenaltyDeductsFive() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.NORMAL);

        service.applySkipPenalty();

        assertEquals(145, service.getScore());
    }

    @Test
    void multipleSkipPenaltiesAccumulate() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.NORMAL);

        for (int i = 0; i < 4; i++) {
            service.applySkipPenalty();
        }

        assertEquals(150 - 20, service.getScore());
    }

    @Test
    void scoreCanGoNegativeWhenPenaltyExceedsPositive() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.applySkipPenalty();
        service.applySkipPenalty();

        assertEquals(-10, service.getScore());
    }

    @Test
    void totalScoreCombinesAllComponents() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addLineScore(100);     // +100
        service.addSpeedBonus(2);      // +20
        service.addDropScore(15);      // +15
        service.applySkipPenalty();    // -5

        assertEquals(130, service.getScore());
    }

    @Test
    void getLineScoreReflectsOnlyLineComponent() {
        ScoreService service = new ScoreService();
        service.reset(Difficulty.EASY);

        service.addLineScore(50);
        service.addSpeedBonus(10);
        service.addDropScore(100);

        // getLineScore는 레벨 산출용이므로 line만 포함
        assertEquals(50, service.getLineScore());
    }

    @Test
    void allDifficultiesHaveDistinctStartingScores() {
        ScoreService easy = new ScoreService();
        ScoreService normal = new ScoreService();
        ScoreService hard = new ScoreService();
        easy.reset(Difficulty.EASY);
        normal.reset(Difficulty.NORMAL);
        hard.reset(Difficulty.HARD);

        assertTrue(easy.getLineScore() < normal.getLineScore());
        assertTrue(normal.getLineScore() < hard.getLineScore());
    }
}
