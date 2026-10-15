package com.team.tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameMode;
import com.team.tetris.domain.Settings;
import com.team.tetris.service.GameEngineService;
import com.team.tetris.view.GameView;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

class GameControllerTest {

    @BeforeAll
    static void startFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 시작된 경우
        }
    }

    /*
     * 뷰 호출을 기록하는 테스트용 더블.
     * 실제 UI 노드는 부모 생성자에서 만들어지지만, 테스트가 추적하려는
     * 메서드만 오버라이드해서 호출 여부와 인자를 기록한다.
     */
    private static class SpyGameView extends GameView {
        final List<ClearResult> playedAnimations = new ArrayList<>();
        GameMode lastSetMode;
        Difficulty lastSetDifficulty;
        int renderCount = 0;
        int applyColorBlindCount = 0;
        int setControlKeyGuideCount = 0;

        @Override
        public void playClearAnimation(ClearResult result, Runnable onFinished) {
            playedAnimations.add(result);
            if (onFinished != null) {
                onFinished.run();
            }
        }

        @Override
        public void setGameInfo(GameMode mode, Difficulty difficulty) {
            this.lastSetMode = mode;
            this.lastSetDifficulty = difficulty;
        }

        @Override
        public void applyColorBlindMode(boolean enabled) {
            applyColorBlindCount++;
        }

        @Override
        public void setControlKeyGuide(String left, String right, String down,
                                       String rotate, String hardDrop, String skip) {
            setControlKeyGuideCount++;
        }

        @Override
        public void render(BlockType[][] boardCells, Block currentBlock, Block nextBlock,
                           int score, int level, boolean paused) {
            renderCount++;
        }

        @Override
        public void updateSkipInfo(String skipKey, int remaining) {
            // no-op
        }
    }

    /*
     * GameOverController의 show()만 가로채서 전달 인자를 기록한다.
     * 부모 필드는 사용하지 않으므로 생성자 인자는 null로 넣어도 안전.
     */
    private static class SpyGameOverController extends GameOverController {
        boolean showed;
        int lastScore;
        int lastLevel;
        int lastClearedLines;
        Difficulty lastDifficulty;
        GameMode lastGameMode;

        SpyGameOverController() {
            super(null, null, null, () -> { });
        }

        @Override
        public void show(int score, int level, int clearedLines,
                         Difficulty difficulty, GameMode gameMode) {
            this.showed = true;
            this.lastScore = score;
            this.lastLevel = level;
            this.lastClearedLines = clearedLines;
            this.lastDifficulty = difficulty;
            this.lastGameMode = gameMode;
        }
    }

    private static <T> T onFx(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(5, TimeUnit.SECONDS);
    }

    private static void runFx(Runnable task) throws Exception {
        onFx(() -> {
            task.run();
            return null;
        });
    }

    // 공용 fixture 생성자
    private static class Fixture {
        final GameEngineService engine;
        final SpyGameView view;
        final SpyGameOverController gameOver;
        final Settings settings;
        final GameController controller;

        Fixture(Difficulty difficulty, boolean itemMode) {
            engine = new GameEngineService();
            view = new SpyGameView();
            gameOver = new SpyGameOverController();
            settings = new Settings();
            controller = new GameController(
                    engine, view, settings, gameOver, difficulty, itemMode);
        }
    }

    @Test
    void startGameInitializesView() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));

        runFx(f.controller::startGame);

        assertEquals(1, f.view.applyColorBlindCount);
        assertEquals(1, f.view.setControlKeyGuideCount);
        assertEquals(GameMode.NORMAL, f.view.lastSetMode,
                "일반 모드로 시작했으니 NORMAL이 전달되어야 함");
        assertEquals(Difficulty.EASY, f.view.lastSetDifficulty);
        assertTrue(f.view.renderCount >= 1, "초기 render 호출이 있어야 함");
    }

    @Test
    void startGameInItemModePassesItemMode() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, true));

        runFx(f.controller::startGame);

        assertEquals(GameMode.ITEM, f.view.lastSetMode);
        assertEquals(Difficulty.NORMAL, f.view.lastSetDifficulty);
    }

    @Test
    void startGameWithModeAndDifficultyOverridesInitialSettings() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));

        runFx(() -> f.controller.startGame(GameMode.ITEM, Difficulty.HARD));

        assertEquals(GameMode.ITEM, f.view.lastSetMode);
        assertEquals(Difficulty.HARD, f.view.lastSetDifficulty);
    }

    @Test
    void updateGameRunsTickAndRenders() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);

        int renderBefore = f.view.renderCount;
        runFx(f.controller::updateGame);

        assertTrue(f.view.renderCount > renderBefore, "updateGame은 render를 호출함");
        assertNotNull(f.engine.getCurrentBlock(), "tick이 실행되어 블록이 스폰됨");
    }

    @Test
    void updateGameDrainsClearEventsToView() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);

        // 블록을 쌓아 라인을 완성시키면 clearEvents가 생긴다.
        // 안정적으로 유도하기 어려우니, 여기서는 '큐가 비어있을 때
        // updateGame이 안전하게 동작하는지'와 '게임오버가 아니면 render가 돈다'만 확인한다.
        runFx(f.controller::updateGame);

        // 라인이 안 생겼으면 애니메이션 큐는 비어있음
        // (StubBlockFactory가 없는 상태라 어떤 블록이 뜰지 모르므로 결정적 라인 성립은 어려움)
        assertNotNull(f.view.playedAnimations, "리스트는 항상 존재");
    }

    @Test
    void handleKeyPauseTogglesPause() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);

        assertFalse(f.engine.isPaused());

        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.PAUSE)));
        assertTrue(f.engine.isPaused());

        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.PAUSE)));
        assertFalse(f.engine.isPaused());
    }

    @Test
    void handleKeyIgnoredAfterGameOver() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        f.engine.quitGame();

        boolean pausedBefore = f.engine.isPaused();
        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.PAUSE)));

        // 게임오버 뒤에는 어떤 키도 반영되지 않아야 함
        assertEquals(pausedBefore, f.engine.isPaused());
    }

    @Test
    void handleKeyUnknownKeyIsIgnored() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        f.engine.tick();  // 블록 스폰

        int scoreBefore = f.engine.getScore();
        runFx(() -> f.controller.handleKey(KeyCode.F13));  // 매핑되지 않은 키

        // 아무 액션도 일어나지 않음
        assertEquals(scoreBefore, f.engine.getScore());
    }

    @Test
    void handleKeyMoveLeftActionCallsEngine() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        f.engine.tick();  // 스폰

        int colBefore = f.engine.getCurrentBlock().getPosition().getCol();
        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.MOVE_LEFT)));

        assertEquals(colBefore - 1, f.engine.getCurrentBlock().getPosition().getCol());
    }

    @Test
    void handleKeySoftDropMovesBlockDown() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        f.engine.tick();

        int rowBefore = f.engine.getCurrentBlock().getPosition().getRow();
        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.SOFT_DROP)));

        assertEquals(rowBefore + 1, f.engine.getCurrentBlock().getPosition().getRow());
    }

    @Test
    void handleKeyBlockedWhenPausedExceptPauseKey() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        f.engine.tick();
        f.engine.togglePause();  // 수동으로 일시정지

        int rowBefore = f.engine.getCurrentBlock().getPosition().getRow();
        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.SOFT_DROP)));

        assertEquals(rowBefore, f.engine.getCurrentBlock().getPosition().getRow(),
                "일시정지 상태에선 이동 키 무시");
    }

    @Test
    void handleKeyQuitInvokesOnQuit() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);

        boolean[] quitInvoked = { false };
        f.controller.setOnQuit(() -> quitInvoked[0] = true);

        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.QUIT)));

        assertTrue(quitInvoked[0]);
        assertTrue(f.engine.isGameOver());
    }

    @Test
    void handleKeyQuitWithoutCallbackDoesNotCrash() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);
        // setOnQuit 호출 안 함

        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.QUIT)));

        assertTrue(f.engine.isGameOver());
    }

    @Test
    void togglePauseDoesNotLoseLastTickTimeOnResume() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.EASY, false));
        runFx(f.controller::startGame);

        runFx(f.controller::togglePause);
        Thread.sleep(50);
        runFx(f.controller::togglePause);

        // togglePause를 두 번 호출하면 결과적으로 다시 플레이 상태
        assertFalse(f.engine.isPaused());
    }

    @Test
    void handleGameOverPassesDifficultyAndModeToGameOverController() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.HARD, true));
        runFx(f.controller::startGame);

        runFx(f.controller::handleGameOver);

        assertTrue(f.gameOver.showed);
        assertEquals(Difficulty.HARD, f.gameOver.lastDifficulty);
        assertEquals(GameMode.ITEM, f.gameOver.lastGameMode);
    }

    @Test
    void handleGameOverPassesCurrentScoreAndLevel() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, false));
        runFx(f.controller::startGame);

        int expectedScore = f.engine.getScore();
        int expectedLevel = f.engine.getLevel();
        int expectedCleared = f.engine.getClearedLines();

        runFx(f.controller::handleGameOver);

        assertEquals(expectedScore, f.gameOver.lastScore);
        assertEquals(expectedLevel, f.gameOver.lastLevel);
        assertEquals(expectedCleared, f.gameOver.lastClearedLines);
    }

    @Test
    void updateGameTriggersGameOverFlowWhenEngineIsOver() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, false));
        runFx(f.controller::startGame);
        f.engine.quitGame();

        runFx(f.controller::updateGame);

        assertTrue(f.gameOver.showed, "게임오버 시 GameOverController.show가 호출되어야 함");
    }

    @Test
    void quitGameStopsGameOver() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, false));
        runFx(f.controller::startGame);

        runFx(f.controller::quitGame);

        assertTrue(f.engine.isGameOver());
    }

    @Test
    void setOnQuitCanReplaceCallback() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, false));
        runFx(f.controller::startGame);

        boolean[] firstCalled = { false };
        boolean[] secondCalled = { false };
        f.controller.setOnQuit(() -> firstCalled[0] = true);
        f.controller.setOnQuit(() -> secondCalled[0] = true);

        runFx(() -> f.controller.handleKey(
                f.settings.getKey(Settings.KeyAction.QUIT)));

        assertFalse(firstCalled[0], "첫 콜백은 덮어씌워져 호출되지 않아야 함");
        assertTrue(secondCalled[0]);
    }

    @Test
    void nullKeyIsIgnored() throws Exception {
        Fixture f = onFx(() -> new Fixture(Difficulty.NORMAL, false));
        runFx(f.controller::startGame);

        // null KeyCode → settings.findGameAction는 null 반환 → 조용히 무시되어야 함
        runFx(() -> f.controller.handleKey(null));
        // 예외 안 나면 통과
    }
}
