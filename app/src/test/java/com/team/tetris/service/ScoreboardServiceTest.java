package com.team.tetris.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;
import com.team.tetris.util.FileManager;

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

        assertTrue(service.isHighScore(10));  // 10위와 동점이면 포함
        assertFalse(service.isHighScore(9));
        assertEquals(-1, service.addScore("Low", 5, 1, 0));
        assertEquals(1, service.addScore("Top", 1000, 1, 0));
        assertEquals(ScoreboardService.MAX_SCORES, service.getTopScores().size()); // 11위(10점)는 제거
    }

    @Test
    void keepsAllTiesWithinTopTen() {
        ScoreboardService service = new ScoreboardService(new FileManager(tempDir));
        for (int i = 1; i <= 9; i++) {
            service.addScore("P" + i, 100 + i, 1, 0);
        }
        // 10위 동점자 3명 → 모두 10위로 남아 12개
        service.addScore("T1", 50, 1, 0);
        service.addScore("T2", 50, 1, 0);
        assertEquals(12, service.addScore("T3", 50, 1, 0));

        List<Score> top = service.getTopScores();
        assertEquals(12, top.size());
        int[] ranks = ScoreboardService.calculateRanks(top);
        assertEquals(10, ranks[9]);
        assertEquals(10, ranks[11]);

        // 더 높은 점수가 들어오면 동점자들은 11위가 되어 모두 제거
        service.addScore("Top", 1000, 1, 0);
        assertEquals(ScoreboardService.MAX_SCORES, service.getTopScores().size());
    }

    @Test
    void calculateRanks_givesSameRankToTies() {
        List<Score> scores = List.of(
                new Score("A", 300, 1, 0), new Score("B", 200, 1, 0),
                new Score("C", 200, 1, 0), new Score("D", 100, 1, 0));

        assertArrayEquals(new int[] {1, 2, 2, 4}, ScoreboardService.calculateRanks(scores));
    }

    @Test
    void separatesGameModesAndKeepsDifficulty() {
        ScoreboardService service = new ScoreboardService(new FileManager(tempDir));
        service.addScore("N", 100, 1, 1, Difficulty.EASY, GameMode.NORMAL);
        assertEquals(1, service.addScore("I", 50, 1, 1, Difficulty.HARD, GameMode.ITEM));

        assertEquals(1, service.getTopScores(GameMode.NORMAL).size());
        assertEquals(1, service.getTopScores(GameMode.ITEM).size());

        ScoreboardService reloaded = new ScoreboardService(new FileManager(tempDir));
        Score item = reloaded.getTopScores(GameMode.ITEM).get(0);
        assertEquals("I", item.getName());
        assertEquals(Difficulty.HARD, item.getDifficulty());
        assertEquals(Difficulty.EASY, reloaded.getTopScores(GameMode.NORMAL).get(0).getDifficulty());
    }

    @Test
    void modesHaveSeparateTopTen() {
        ScoreboardService service = new ScoreboardService(new FileManager(tempDir));
        for (int i = 1; i <= 10; i++) {
            service.addScore("N" + i, 1000 + i, 1, 0, Difficulty.NORMAL, GameMode.NORMAL);
        }

        assertFalse(service.isHighScore(1, GameMode.NORMAL));
        assertTrue(service.isHighScore(1, GameMode.ITEM));
    }

    @Test
    void nullDifficultyAndModeSavedAsNormal() {
        Score s = new Score("A", 100, 1, 1, null, null);

        assertEquals(Difficulty.NORMAL, s.getDifficulty());
        assertEquals(GameMode.NORMAL, s.getGameMode());
    }

    @Test
    void loadsOldFormatAsNormal() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("scoreboard.csv", "OLD,300,2,5\n");

        Score s = new ScoreboardService(fileManager).getTopScores(GameMode.NORMAL).get(0);
        assertEquals("OLD", s.getName());
        assertEquals(Difficulty.NORMAL, s.getDifficulty());
        assertEquals(GameMode.NORMAL, s.getGameMode());
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

    @Test
    void fileErrors_doNotStopGame() throws IOException {
        // 저장 폴더 자리에 일반 파일이 있어 읽기·쓰기 모두 실패하는 상황
        Path blocker = Files.writeString(tempDir.resolve("blocker"), "");
        ScoreboardService service = assertDoesNotThrow(() -> new ScoreboardService(new FileManager(blocker)));

        assertEquals(1, assertDoesNotThrow(() -> service.addScore("A", 100, 1, 1)));
        assertEquals(1, service.getTopScores().size()); // 이번 실행 동안은 기록 유지
        assertDoesNotThrow(service::resetScoreboard);
    }
}
