package com.team.tetris.controller;

import static com.team.tetris.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.team.tetris.FxTestSupport;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Score;
import com.team.tetris.service.ScoreboardService;
import com.team.tetris.util.FileManager;
import com.team.tetris.view.GameOverView;
import com.team.tetris.view.ScoreboardView;

import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

class GameOverControllerTest {

    @TempDir
    Path tempDir;

    private ScoreboardService service;
    private ScoreboardView scoreboardView;
    private GameOverView gameOverView;
    private GameOverController controller;
    private int gameOverShown;
    private int scoreboardShown;

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new ScoreboardService(new FileManager(tempDir));
        gameOverShown = 0;
        scoreboardShown = 0;
        onFx(() -> {
            scoreboardView = new ScoreboardView();
            gameOverView = new GameOverView();
            ScoreboardController scoreboardController = new ScoreboardController(
                    service, scoreboardView, () -> scoreboardShown++, () -> { });
            controller = new GameOverController(
                    service, scoreboardController, gameOverView, () -> gameOverShown++);
        });
    }

    private void type(KeyCode... keys) {
        for (KeyCode key : keys) {
            controller.handleKey(key);
        }
    }

    private boolean nameInputVisible() {
        return ((VBox) gameOverView.getRoot()).getChildren().get(2).isVisible();
    }

    private String typedName() {
        VBox nameBox = (VBox) ((VBox) gameOverView.getRoot()).getChildren().get(2);
        return ((Label) nameBox.getChildren().get(1)).getText();
    }

    /** 일반 모드에 높은 점수 10개를 채워, 낮은 점수는 순위에 못 들게 함 */
    private void fillNormalTopTen() {
        for (int i = 1; i <= ScoreboardService.MAX_SCORES; i++) {
            service.addScore("TOP" + i, 1000 + i, 1, 0);
        }
    }

    @Test
    void highScore_showsNameInput_andSavesWithDifficultyAndMode() throws Exception {
        onFx(() -> {
            controller.show(500, 3, 7, Difficulty.HARD, GameMode.ITEM);
            assertEquals(1, gameOverShown);
            assertTrue(nameInputVisible());

            type(KeyCode.K, KeyCode.I, KeyCode.M, KeyCode.DIGIT1, KeyCode.ENTER);

            List<Score> item = service.getTopScores(GameMode.ITEM);
            assertEquals(1, item.size());
            Score saved = item.get(0);
            assertEquals("KIM1", saved.getName());
            assertEquals(500, saved.getScore());
            assertEquals(3, saved.getLevel());
            assertEquals(7, saved.getClearedLines());
            assertEquals(Difficulty.HARD, saved.getDifficulty());
            assertTrue(service.getTopScores(GameMode.NORMAL).isEmpty());

            // 저장 후 해당 모드 스코어보드로 이동
            assertEquals(1, scoreboardShown);
            assertEquals(GameMode.ITEM, scoreboardView.getSelectedMode());
        });
    }

    @Test
    void typing_updatesNameOnScreen_andBackspaceDeletes() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            type(KeyCode.L, KeyCode.E, KeyCode.E);
            assertEquals("LEE_", typedName());

            type(KeyCode.BACK_SPACE);
            assertEquals("LE_", typedName());

            type(KeyCode.BACK_SPACE, KeyCode.BACK_SPACE, KeyCode.BACK_SPACE); // 빈 상태에서 지워도 문제없음
            assertEquals("_", typedName());
        });
    }

    @Test
    void onlyLettersAndTopRowDigits_areAccepted() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            type(KeyCode.A, KeyCode.SPACE, KeyCode.NUMPAD1, KeyCode.SHIFT, KeyCode.DIGIT9, KeyCode.COMMA);

            assertEquals("A9_", typedName());
        });
    }

    @Test
    void name_isLimitedToMaxLength() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            for (int i = 0; i < GameOverController.MAX_NAME_LENGTH + 5; i++) {
                controller.handleKey(KeyCode.Z);
            }
            controller.handleKey(KeyCode.ENTER);

            assertEquals("Z".repeat(GameOverController.MAX_NAME_LENGTH),
                    service.getTopScores().get(0).getName());
        });
    }

    @Test
    void emptyName_isNotSaved() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            controller.handleKey(KeyCode.ENTER);

            assertTrue(service.getTopScores().isEmpty());
            assertEquals(0, scoreboardShown);
            assertTrue(nameInputVisible());
        });
    }

    @Test
    void afterSaving_enterDoesNotSaveAgain() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            type(KeyCode.A, KeyCode.ENTER);
            type(KeyCode.B, KeyCode.ENTER);

            assertEquals(1, service.getTopScores().size());
            assertEquals("A", service.getTopScores().get(0).getName());
        });
    }

    @Test
    void notHighScore_hidesNameInput_andEnterShowsScoreboard() throws Exception {
        fillNormalTopTen();
        onFx(() -> {
            controller.show(10, 1, 0);
            assertEquals(1, gameOverShown);
            assertFalse(nameInputVisible());

            type(KeyCode.A, KeyCode.BACK_SPACE); // 이름 입력 단계가 아니므로 무시
            assertEquals(1, gameOverShown);
            assertEquals(0, scoreboardShown);

            controller.handleKey(KeyCode.ENTER);
            assertEquals(1, scoreboardShown);
            assertEquals(ScoreboardService.MAX_SCORES, service.getTopScores().size());
        });
    }

    @Test
    void submitName_directlySavesTrimmedName() throws Exception {
        onFx(() -> {
            controller.show(300, 2, 4, Difficulty.EASY, GameMode.NORMAL);
            controller.submitName("  PARK  ");

            Score saved = service.getTopScores().get(0);
            assertEquals("PARK", saved.getName());
            assertEquals(Difficulty.EASY, saved.getDifficulty());
        });
    }

    @Test
    void submitName_isIgnoredWhenNotInputtingOrBlank() throws Exception {
        fillNormalTopTen();
        onFx(() -> {
            controller.show(10, 1, 0); // 순위권 밖 → 이름 입력 단계 아님
            controller.submitName("LOW");
            assertEquals(0, scoreboardShown);

            controller.show(5000, 1, 0); // 순위권
            controller.submitName(null);
            controller.submitName("   ");
            assertEquals(0, scoreboardShown);
            assertEquals("TOP10", service.getTopScores().get(0).getName());
        });
    }

    @Test
    void showWithoutDifficultyAndMode_savesAsNormal() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            type(KeyCode.A, KeyCode.ENTER);

            Score saved = service.getTopScores(GameMode.NORMAL).get(0);
            assertEquals(Difficulty.NORMAL, saved.getDifficulty());
            assertEquals(GameMode.NORMAL, saved.getGameMode());
        });
    }

    @Test
    void nullDifficultyAndMode_areTreatedAsNormal() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1, null, null);
            type(KeyCode.A, KeyCode.ENTER);

            Score saved = service.getTopScores(GameMode.NORMAL).get(0);
            assertEquals(Difficulty.NORMAL, saved.getDifficulty());
        });
    }

    @Test
    void nullKey_isIgnored() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            controller.handleKey(null);

            assertEquals("_", typedName());
        });
    }

    @Test
    void newShow_resetsPreviousName() throws Exception {
        onFx(() -> {
            controller.show(100, 1, 1);
            type(KeyCode.A, KeyCode.B);
            controller.show(200, 1, 1);

            assertEquals("_", typedName());
        });
    }
}
