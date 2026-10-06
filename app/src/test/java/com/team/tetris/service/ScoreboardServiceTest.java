package com.team.tetris.service;

import com.team.tetris.domain.Score;
import com.team.tetris.util.FileManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void addScore_returnsRankInDescendingOrder() {
        ScoreboardService service = new ScoreboardService(new FileManager(tempDir));

        assertEquals(1, service.addScore("A", 100, 1, 1));
        assertEquals(1, service.addScore("B", 300, 2, 3));
        assertEquals(2, service.addScore("C", 200, 1, 2));
        assertEquals(4, service.addScore("D", 100, 1, 1)); // 동점이면 기존 기록이 앞

        List<Score> top = service.getTopScores();
        assertEquals("B", top.get(0).getName());
        assertEquals("A", top.get(2).getName());
    }

    @Test
    void keepsOnlyTopTen() {
        ScoreboardService service = new ScoreboardService(new FileManager(tempDir));
        for (int i = 1; i <= 10; i++) {
            service.addScore("P" + i, i * 10, 1, 0);
        }

        assertFalse(service.isHighScore(10));
        assertEquals(-1, service.addScore("Low", 5, 1, 0));
        assertEquals(1, service.addScore("Top", 1000, 1, 0));
        assertEquals(ScoreboardService.MAX_SCORES, service.getTopScores().size());
    }

    @Test
    void persistsAndResets() {
        new ScoreboardService(new FileManager(tempDir)).addScore("Kim,\nLee", 500, 3, 7);

        ScoreboardService reloaded = new ScoreboardService(new FileManager(tempDir));
        Score s = reloaded.getTopScores().get(0);
        assertEquals("KimLee", s.getName());
        assertEquals(500, s.getScore());
        assertEquals(3, s.getLevel());
        assertEquals(7, s.getClearedLines());

        reloaded.resetScoreboard();
        assertTrue(new ScoreboardService(new FileManager(tempDir)).getTopScores().isEmpty());
    }
}
