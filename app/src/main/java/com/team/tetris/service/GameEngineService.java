package com.team.tetris.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

public class GameEngineService {

    //한 판 최대 스킵 횟수
    private static final int MAX_SKIPS = 7;

    private final Board board;
    private final BlockFactory blockFactory;
    private final ScoreService scoreService;
    private final LevelService levelService;
    private final AttackService attackService;

    private Block currentBlock;
    private Block nextBlock;
    private boolean blockLocked;
    private int clearedLines;
    private int skipsUsed;

    private boolean paused;
    private boolean gameOver;

    private Difficulty difficulty;
    private boolean itemMode;
    private boolean isItemBlockTurn;
    private ClearResult pendingLineClear;
    private AttackRows pendingAttack; // 미뤄둔 줄 삭제가 끝나면 보낼 공격
    private boolean weightCollided;
    private final Deque<ClearResult> clearEvents = new ArrayDeque<>();

    public GameEngineService() {
        this(new Board(), new BlockFactory(), new ScoreService(), new LevelService());
    }

    public GameEngineService(Board board,
                             BlockFactory blockFactory,
                             ScoreService scoreService,
                             LevelService levelService) {
        // 싱글 모드: 받은 공격도 없고 보낼 상대도 없음
        this(board, blockFactory, scoreService, levelService,
                new AttackService(board, new AttackQueue(), attack -> { }));
    }

    // 대전 모드: 자신의 보드로 만든 AttackService를 함께 넘긴다
    public GameEngineService(Board board,
                             BlockFactory blockFactory,
                             ScoreService scoreService,
                             LevelService levelService,
                             AttackService attackService) {
        this.board = board;
        this.blockFactory = blockFactory;
        this.scoreService = scoreService;
        this.levelService = levelService;
        this.attackService = attackService;
    }

    //변수 초기화
    public void startGame(Difficulty difficulty, boolean itemMode) {
        this.difficulty = difficulty;
        this.itemMode = itemMode;

        board.reset();
        scoreService.reset(difficulty);
        levelService.reset(difficulty);
        levelService.updateLevel(scoreService.getLineScore());

        currentBlock = null;
        blockLocked = false;
        clearedLines = 0;
        skipsUsed = 0;
        isItemBlockTurn = false;
        nextBlock = createNextBlock();
        pendingLineClear = null;
        pendingAttack = null;
        weightCollided = false;
        clearEvents.clear();

        paused = false;
        gameOver = false;
    }

    //틱당 실행할 메소드
    public void tick() {
        //isItemBlockTurn = Math.random() < 0.5; // 테스트용 라인입니당 ^_^

        // 지난 틱에서 미뤄진 라인 삭제가 있으면 이번 틱에 처리만 하고 끝
        if (pendingLineClear != null) {
            ClearResult pending = pendingLineClear;
            pendingLineClear = null;
            board.applyClear(pending);
            applyScoreAndCounter(
                    pending.getScoreLineCount(),
                    pending.getItemCounterLineCount()
            );
            sendPendingAttack();
            return;
        }

        // 현재 블럭이 없으면 다음 블럭을 올리고, 다음 블럭 새로 생성
        if (currentBlock == null) {
            // 대전 모드: 현재 블럭의 삭제 처리가 모두 끝난 뒤, 다음 블럭 생성 전에 받은 공격 적용
            if (attackService.applyAttack()) {
                gameOver = true;
                return;
            }

            currentBlock = nextBlock;
            nextBlock = createNextBlock();
            weightCollided = false;

            // 생성 위치에 놓을 수 없으면 게임오버
            if (!board.canPlace(currentBlock)) {
                gameOver = true;
                return;
            }
        }

        //아래 움직이기 (유저 인풋과 동일), 못 움직이면 내부에서 락
        if (!blockLocked) {
            moveDown();
        }

        // 블럭이 고정됐으면 아이템 효과만 즉시 처리, 꽉 찬 행은 다음 틱으로 미룸
        if (blockLocked) {
            if (currentBlock != null && currentBlock.hasItem()) {
                ClearResult itemResult = board.findItemEffect(currentBlock);
                // 대전 모드: 행 삭제 전에 공격 생성 → 삭제 → 전송
                AttackRows itemAttack = board.createAttackRows(itemResult, currentBlock.getCells());
                board.applyClear(itemResult);
                attackService.sendAttack(itemAttack);
                applyScoreAndCounter(
                        itemResult.getScoreLineCount(),
                        itemResult.getItemCounterLineCount()
                );
                if (!itemResult.isEmpty() || itemResult.getItemType() == ItemType.WEIGHT) {
                    clearEvents.add(itemResult);
                }
            }

            ClearResult pending = board.findFullLines();
            if (!pending.isEmpty()) {
                // 대전 모드: 행이 지워지기 전에 공격 생성, 실제 삭제 후 전송
                pendingAttack = board.createAttackRows(pending, currentBlock.getCells());
                pendingLineClear = pending;
                clearEvents.add(pending);
            }

            currentBlock = null;
            blockLocked = false;
        }
    }

    public ClearResult pollClearEvent() {
        return clearEvents.pollFirst();
    }

    //애니메이션 종료 시점에 호출: 미뤄둔 라인 클리어를 즉시 반영한다.
    public boolean applyPendingLineClear() {
        if (pendingLineClear == null) {
            return false;
        }
        ClearResult pending = pendingLineClear;
        pendingLineClear = null;
        board.applyClear(pending);
        applyScoreAndCounter(
                pending.getScoreLineCount(),
                pending.getItemCounterLineCount()
        );
        sendPendingAttack();
        return true;
    }

    //미뤄둔 줄 삭제가 반영된 직후 공격 전송
    private void sendPendingAttack() {
        if (pendingAttack == null) {
            return;
        }
        AttackRows attack = pendingAttack;
        pendingAttack = null;
        attackService.sendAttack(attack);
    }

    private Block createNextBlock() {
        if (isItemBlockTurn && itemMode) {
            isItemBlockTurn = false;
            return blockFactory.createItemBlock();
        }
        return blockFactory.createRandomBlock(difficulty);
    }

    //점수/카운터/레벨 반영 (아이템 효과·라인 삭제 공통)
    private void applyScoreAndCounter(int scoreLines, int counterLines) {
        if (counterLines > 0) {
            clearedLines += counterLines;
            if (clearedLines % 10 == 0) {
                isItemBlockTurn = true;
            }
        }
        if (scoreLines > 0) {
            int lineScore = scoreService.calculateLineClearScore(scoreLines);
            scoreService.addLineScore(lineScore);

            int levelBefore = levelService.getLevel();
            levelService.updateLevel(scoreService.getLineScore());
            int levelUpCount = levelService.getLevel() - levelBefore;
            if (levelUpCount > 0) {
                scoreService.addSpeedBonus(levelUpCount);
            }
        }
    }

    //게임 종료
    public void quitGame() {
        gameOver = true;
    }

    public boolean moveLeft() {
        if (!hasCurrentBlock()) {
            return false;
        }
        if (currentBlock.getType() == BlockType.WEIGHT && weightCollided) {
            return false;
        }
        if (board.canMove(currentBlock, 0, -1)) {
            currentBlock.move(0, -1);
            return true;
        }
        return false;
    }

    public boolean moveRight() {
        if (!hasCurrentBlock()) {
            return false;
        }
        if (currentBlock.getType() == BlockType.WEIGHT && weightCollided) {
            return false;
        }
        if (board.canMove(currentBlock, 0, 1)) {
            currentBlock.move(0, 1);
            return true;
        }
        return false;
    }

    //아래로 움직였으면 true, 못움직이면 lock하고 false 리턴
    public boolean moveDown() {
        if (!hasCurrentBlock()) {
            return false;
        }
        if (currentBlock.getType() == BlockType.WEIGHT) {
            return moveDownWeight();
        }
        if (board.canMove(currentBlock, 1, 0)) {
            currentBlock.move(1, 0);
            scoreService.addDropScore(1);
            return true;
        }
        board.lockBlock(currentBlock);
        blockLocked = true;
        return false;
    }

    //WEIGHT 블럭: 바닥에 닿을 때까지 앞의 셀을 파괴하며 하강
    private boolean moveDownWeight() {
        for (Position cell : currentBlock.getMovedCells(1, 0)) {
            if (!board.isInside(cell.getRow(), cell.getCol())) {
                // 바닥 도달: 블럭을 보드에 쓰지 않고 소멸시킴
                blockLocked = true;
                return false;
            }
        }

        if (!board.canMove(currentBlock, 1, 0)) {
            weightCollided = true;
        }

        Set<Long> currentKeys = new HashSet<>();
        for (Position c : currentBlock.getCells()) {
            currentKeys.add(((long) c.getRow() << 32) | c.getCol());
        }
        List<Position> newBelow = new ArrayList<>();
        for (Position c : currentBlock.getMovedCells(1, 0)) {
            long key = ((long) c.getRow() << 32) | c.getCol();
            if (!currentKeys.contains(key)) {
                newBelow.add(new Position(c.getRow(), c.getCol()));
            }
        }
        if (!newBelow.isEmpty()) {
            board.applyClear(new ClearResult(
                    ItemType.WEIGHT,
                    List.of(),
                    newBelow,
                    List.of(),
                    -1,
                    0,
                    0
            ));
        }

        currentBlock.move(1, 0);
        scoreService.addDropScore(1);
        return true;
    }

    public boolean rotate() {
        if (!hasCurrentBlock() || !currentBlock.canRotate()) {
            return false;
        }

        // 현재 위치 → 왼쪽 1칸 → 오른쪽 1칸
        // → 왼쪽 2칸 → 오른쪽 2칸
        int[] colOffsets = {0, -1, 1, -2, 2};

        for (int deltaCol : colOffsets) {
            if (board.canRotate(currentBlock, 0, deltaCol)) {
                currentBlock.move(0, deltaCol);
                currentBlock.rotate();
                return true;
            }
        }

        // 모든 후보가 막혀 있으면 위치와 회전 상태 유지
        return false;
    }

    //현재 블럭 스킵. 성공 시 true, 못 쓰면 false
    public boolean skipCurrentBlock() {
        if (gameOver || paused) {
            return false;
        }
        if (currentBlock == null || blockLocked) {
            return false;
        }
        if (skipsUsed >= MAX_SKIPS) {
            return false;
        }
        currentBlock = null;
        skipsUsed++;
        scoreService.applySkipPenalty();
        return true;
    }

    public void hardDrop() {
        if (!hasCurrentBlock()) {
            return;
        }
        if (currentBlock.getType() == BlockType.WEIGHT) {
            while (hasCurrentBlock()) {
                moveDown();
            }
            return;
        }
        int dropped = 0;
        while (board.canMove(currentBlock, 1, 0)) {
            currentBlock.move(1, 0);
            dropped++;
        }
        scoreService.addDropScore(dropped);
        board.lockBlock(currentBlock);
        blockLocked = true;
    }

    //블럭이 조작가능한 상태인지 판단
    private boolean hasCurrentBlock() {
        return currentBlock != null && !blockLocked;
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public BlockType[][] getBoardCells() {
        return board.getCells();
    }

    public Block getCurrentBlock() {
        return currentBlock;
    }

    public Block getNextBlock() {
        return nextBlock;
    }

    public int getScore() {
        return scoreService.getScore();
    }

    public int getLevel() {
        return levelService.getLevel();
    }

    public int getClearedLines() {
        return clearedLines;
    }

    public int getSkipsUsed() {
        return skipsUsed;
    }

    public int getSkipsRemaining() {
        return MAX_SKIPS - skipsUsed;
    }

    //대전 모드: 현재 상태 복사본 (화면 표시·P2P 전송용). 엔진 스레드에서 호출한다
    public GameSnapshot getSnapshot() {
        boolean[][] attackCells = new boolean[Board.ROWS][Board.COLS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                attackCells[row][col] = board.isAttackCell(row, col);
            }
        }
        return new GameSnapshot(
                board.getCells(),
                attackCells,
                currentBlock,
                nextBlock,
                getScore(),
                getLevel(),
                getSkipsRemaining(),
                attackService.getPendingAttack(),
                gameOver
        );
    }

    public long getDropIntervalMillis() {
        return levelService.getDropIntervalMillis();
    }
}