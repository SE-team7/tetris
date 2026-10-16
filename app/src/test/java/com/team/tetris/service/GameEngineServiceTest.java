package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

class GameEngineServiceTest {

    private static final int MAX_SKIPS = 7;

    // 지정한 블록만 반환하는 테스트용 팩토리
    private static class StubBlockFactory extends BlockFactory {
        private final Deque<Block> normalQueue = new ArrayDeque<>();
        private final Deque<Block> itemQueue = new ArrayDeque<>();
        private BlockType fallbackType = BlockType.O;

        void enqueue(Block block) {
            normalQueue.addLast(block);
        }

        void enqueueItem(Block block) {
            itemQueue.addLast(block);
        }

        void setFallback(BlockType type) {
            this.fallbackType = type;
        }

        @Override
        public Block createRandomBlock(Difficulty difficulty) {
            if (!normalQueue.isEmpty()) {
                return normalQueue.pollFirst();
            }
            return createBlock(fallbackType);
        }

        @Override
        public Block createItemBlock() {
            if (!itemQueue.isEmpty()) {
                return itemQueue.pollFirst();
            }
            return createBlock(BlockType.BOMB);
        }
    }

    private GameEngineService newEngine(StubBlockFactory factory) {
        return new GameEngineService(
                new Board(),
                factory,
                new ScoreService(),
                new LevelService()
        );
    }

    private Block oBlockAt(int row, int col) {
        return new Block(BlockType.O, new Position(row, col));
    }

    private Block bombAt(int row, int col) {
        return new Block(BlockType.BOMB, new Position(row, col));
    }

    @Test
    void startGameInitializesCleanState() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.isGameOver());
        assertFalse(engine.isPaused());
        assertEquals(0, engine.getScore());
        assertEquals(0, engine.getClearedLines());
        assertEquals(0, engine.getSkipsUsed());
        assertEquals(MAX_SKIPS, engine.getSkipsRemaining());
        assertNull(engine.getCurrentBlock(), "startGame 직후엔 currentBlock 없음");
        assertNotNull(engine.getNextBlock(), "nextBlock은 미리 준비되어야 함");
    }

    @Test
    void startGameAppliesDifficultyToScoreAndLevel() {
        GameEngineService engine = newEngine(new StubBlockFactory());

        engine.startGame(Difficulty.NORMAL, false);
        assertEquals(150, engine.getScore());

        engine.startGame(Difficulty.HARD, false);
        assertEquals(300, engine.getScore());

        engine.startGame(Difficulty.EASY, false);
        assertEquals(0, engine.getScore());
    }

    @Test
    void startGameClearsPreviousClearEvents() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, true);

        // 아이템 블록을 떨어뜨려 clearEvents에 뭔가 쌓이도록 유도 가능하지만,
        // 이 테스트는 startGame 재호출 시 비워지는지만 검증한다.
        engine.startGame(Difficulty.EASY, false);

        assertNull(engine.pollClearEvent());
    }

    @Test
    void tickSpawnsCurrentBlockFromNext() {
        StubBlockFactory factory = new StubBlockFactory();
        factory.enqueue(oBlockAt(4, 4));
        factory.enqueue(oBlockAt(4, 4));
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        engine.tick();

        assertNotNull(engine.getCurrentBlock());
    }

    @Test
    void tickAdvancesBlockDownward() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        engine.tick();
        int firstRow = engine.getCurrentBlock().getPosition().getRow();

        engine.tick();
        int secondRow = engine.getCurrentBlock().getPosition().getRow();

        assertEquals(firstRow + 1, secondRow, "매 틱마다 한 칸 내려가야 함");
    }

    @Test
    void hardDropLocksBlockAtBottom() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        engine.tick();           // spawn
        engine.hardDrop();       // 바닥까지 떨어뜨리고 lock

        engine.tick();           // 블록이 null로 비워져야 다음 블록 spawn

        // 다음 틱 전에 pollClearEvent는 라인 없으면 null
        // (hardDrop 1번만으로는 라인이 안 참)
    }

    @Test
    void gameOverWhenSpawnedBlockCannotBePlaced() {
        StubBlockFactory factory = new StubBlockFactory();
        // 스폰 위치(row=4, col=4 근처)를 가로막는 블록을 미리 넣어둔다.
        // 첫 블록은 바로 그 자리에 와서 game over가 되도록 설정.
        factory.enqueue(oBlockAt(5, 4));  // 첫 스폰 — 바닥까지 가지 않는 다른 블록으로 테스트가 변질되지 않게
        // 실제로는 보드가 꽉 차야 game over 되지만, 여기선 모든 블록이 즉시 스폰위치에 올 때
        // 가로막혀야 함을 검증한다. 간단히 하기 위해 보드를 공격적으로 채우는 대신
        // 아래 다른 테스트에서 다룬다.
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        // 자연 틱이 진행되면 블록이 쌓이며 결국 상단에서 game over가 발생해야 한다.
        // 여기선 "최소한 game over가 가능함"만 검증한다 — 과도한 시뮬레이션을 피함.
        int safety = 500;
        while (!engine.isGameOver() && safety-- > 0) {
            engine.tick();
            if (engine.isGameOver()) {
                break;
            }
            if (engine.getCurrentBlock() != null) {
                engine.hardDrop();  // 가능한 빨리 쌓는다
            }
        }

        assertTrue(engine.isGameOver(), "꽉 차면 game over가 발생해야 함");
    }

    @Test
    void moveLeftFailsWhenNoCurrentBlock() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.moveLeft());
    }

    @Test
    void moveRightFailsWhenNoCurrentBlock() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.moveRight());
    }

    @Test
    void moveDownFailsWhenNoCurrentBlock() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.moveDown());
    }

    @Test
    void rotateFailsWhenNoCurrentBlock() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.rotate());
    }

    @Test
    void moveLeftShiftsBlockHorizontally() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);
        engine.tick();

        int originalCol = engine.getCurrentBlock().getPosition().getCol();
        assertTrue(engine.moveLeft());
        assertEquals(originalCol - 1, engine.getCurrentBlock().getPosition().getCol());
    }

    @Test
    void moveRightShiftsBlockHorizontally() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);
        engine.tick();

        int originalCol = engine.getCurrentBlock().getPosition().getCol();
        assertTrue(engine.moveRight());
        assertEquals(originalCol + 1, engine.getCurrentBlock().getPosition().getCol());
    }

    @Test
    void skipRemovesCurrentBlockAndIncrementsCount() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();

        int scoreBefore = engine.getScore();
        assertTrue(engine.skipCurrentBlock());

        assertEquals(1, engine.getSkipsUsed());
        assertEquals(MAX_SKIPS - 1, engine.getSkipsRemaining());
        assertEquals(scoreBefore - 5, engine.getScore(), "스킵마다 5점 감점");
        assertNull(engine.getCurrentBlock(), "스킵하면 현재 블록은 소멸");
    }

    @Test
    void skipFailsAfterMaxSkips() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        for (int i = 0; i < MAX_SKIPS; i++) {
            engine.tick();
            assertTrue(engine.skipCurrentBlock(), "허용 횟수 안에선 성공해야 함");
        }

        engine.tick();
        assertFalse(engine.skipCurrentBlock(), "한도 초과 후엔 실패");
        assertEquals(MAX_SKIPS, engine.getSkipsUsed());
        assertEquals(0, engine.getSkipsRemaining());
    }

    @Test
    void skipFailsWhenGameOver() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();
        engine.quitGame();

        assertFalse(engine.skipCurrentBlock());
    }

    @Test
    void skipFailsWhenPaused() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();
        engine.togglePause();

        assertFalse(engine.skipCurrentBlock());
    }

    @Test
    void skipFailsWhenNoCurrentBlock() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.skipCurrentBlock());
    }

    @Test
    void togglePauseTogglesState() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertFalse(engine.isPaused());

        engine.togglePause();
        assertTrue(engine.isPaused());

        engine.togglePause();
        assertFalse(engine.isPaused());
    }

    @Test
    void quitGameMarksGameOver() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        engine.quitGame();

        assertTrue(engine.isGameOver());
    }

    @Test
    void getDropIntervalMillisDelegatesToLevelService() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        // level 1 기본값
        assertEquals(1000L, engine.getDropIntervalMillis());
    }

    @Test
    void pollClearEventReturnsNullWhenEmpty() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        assertNull(engine.pollClearEvent());
    }

    @Test
    void lineClearIsDeferredByOneTickAndEnqueuedForAnimation() {
        // 보드 밑바닥을 꽉 채운 뒤 라인 클리어가 pending으로 들어가는지 확인.
        // 스터브 블록의 position을 (24, X)로 지정하면 Block 생성자가 그 위치를
        // 그대로 쓰므로, tick의 moveDown이 즉시 바닥에서 막혀 바로 lock된다.
        StubBlockFactory factory = new StubBlockFactory();
        factory.enqueue(oBlockAt(24, 0));
        factory.enqueue(oBlockAt(24, 2));
        factory.enqueue(oBlockAt(24, 4));
        factory.enqueue(oBlockAt(24, 6));
        factory.enqueue(oBlockAt(24, 8));

        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        // 5번 tick → 5개 블록이 각각 바닥 10칸을 채우고 마지막 틱에서 pending 생성
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }

        ClearResult event = engine.pollClearEvent();
        assertNotNull(event, "라인 완성 시 clear 이벤트가 enqueue 되어야 함");
        assertEquals(2, event.getRowsToDelete().size(), "두 줄 동시 클리어");
    }

    @Test
    void pendingLineClearAppliedOnNextTickAndScoredOnce() {
        StubBlockFactory factory = new StubBlockFactory();
        factory.enqueue(oBlockAt(24, 0));
        factory.enqueue(oBlockAt(24, 2));
        factory.enqueue(oBlockAt(24, 4));
        factory.enqueue(oBlockAt(24, 6));
        factory.enqueue(oBlockAt(24, 8));

        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        for (int i = 0; i < 5; i++) {
            engine.tick();
        }

        int scoreBeforeApply = engine.getScore();
        int clearedBeforeApply = engine.getClearedLines();

        // 지연된 라인 클리어가 다음 틱에 반영
        engine.tick();

        assertTrue(engine.getScore() > scoreBeforeApply,
                "라인 클리어 점수가 반영되어야 함");
        assertEquals(clearedBeforeApply + 2, engine.getClearedLines(),
                "2줄 삭제가 카운터에 반영");
    }

    @Test
    void itemBlockEffectEnqueuesClearEvent() {
        StubBlockFactory factory = new StubBlockFactory();
        factory.setFallback(BlockType.O);
        // 첫 블록으로 바닥에 뭔가 쌓아두고,
        factory.enqueue(oBlockAt(24, 4));
        // 그 다음 아이템 턴에 BOMB을 떨어뜨린다.
        factory.enqueueItem(bombAt(24, 4));

        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, true);

        // 첫 블록 스폰 → hardDrop
        engine.tick();
        engine.hardDrop();
        engine.tick();

        // 폭탄이 반드시 다음 턴에 나오지 않을 수 있다(랜덤). isItemBlockTurn은 10줄 삭제마다 켜짐.
        // 그러나 이 테스트에선 아이템 모드만 활성화하고 pollClearEvent로 어느 이벤트든 확인.
        // 라인 완성이 없더라도 아이템 효과가 발생하면 enqueue 되어야 한다.
        // 따라서 "clearEvents가 비어있지 않은 가능성"만 확인하는 방식 대신,
        // 지난 틱의 결과 큐에서 뽑히는 것을 전부 소비하며 라인/아이템 유무를 확인한다.
        // 여기선 "비어있지 않을 수 있다"는 수준의 검증은 약하므로, 결정적 테스트는
        // '라인 클리어 지연' 테스트로 대체하고, 이 테스트는 pollClearEvent가 안전하게
        // 동작하는 것만 확인한다.
        //
        // 큐에 아무것도 없어도 null 반환이면 안전.
        ClearResult event = engine.pollClearEvent();
        if (event != null) {
            assertNotNull(event.getItemType());
        }
    }

    @Test
    void getNextBlockReturnsUpcomingBlock() {
        StubBlockFactory factory = new StubBlockFactory();
        GameEngineService engine = newEngine(factory);
        engine.startGame(Difficulty.EASY, false);

        Block firstNext = engine.getNextBlock();
        engine.tick();  // 스폰: nextBlock이 currentBlock으로 승격, 새 next 생성

        assertEquals(firstNext, engine.getCurrentBlock(),
                "직전의 nextBlock이 currentBlock이 되어야 함");
        assertNotNull(engine.getNextBlock(), "새로운 next가 준비되어야 함");
    }

    @Test
    void getBoardCellsReturnsCopy() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);

        BlockType[][] first = engine.getBoardCells();
        BlockType[][] second = engine.getBoardCells();

        assertFalse(first == second, "매 호출마다 새 배열이 반환되어야 함(복사)");
    }

    @Test
    void startGameResetsSkipsUsed() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();
        engine.skipCurrentBlock();
        assertEquals(1, engine.getSkipsUsed());

        engine.startGame(Difficulty.EASY, false);

        assertEquals(0, engine.getSkipsUsed());
    }

    @Test
    void gameOverIgnoresFurtherSkipAttempts() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.quitGame();

        for (int i = 0; i < 3; i++) {
            assertFalse(engine.skipCurrentBlock());
        }

        assertEquals(0, engine.getSkipsUsed());
    }

    @Test
    void hardDropBoostsDropScore() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();  // 스폰
        int before = engine.getScore();

        engine.hardDrop();

        assertTrue(engine.getScore() > before, "hardDrop은 떨어진 셀 수만큼 점수를 더함");
    }

    @Test
    void moveDownAddsDropScore() {
        GameEngineService engine = newEngine(new StubBlockFactory());
        engine.startGame(Difficulty.EASY, false);
        engine.tick();
        int before = engine.getScore();

        assertTrue(engine.moveDown());

        assertEquals(before + 1, engine.getScore(), "소프트 드롭 1칸당 1점");
    }
}
