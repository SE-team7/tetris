package com.team.tetris.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

public class GameEngineService {

    //한 판 최대 스킵 횟수
    private static final int MAX_SKIPS = 7;

    private final Board board;
    private final BlockFactory blockFactory;
    private final ScoreService scoreService;
    private final LevelService levelService;

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
    private boolean weightCollided;

    public GameEngineService() {
        this(new Board(), new BlockFactory(), new ScoreService(), new LevelService());
    }

    public GameEngineService(Board board,
                             BlockFactory blockFactory,
                             ScoreService scoreService,
                             LevelService levelService) {
        this.board = board;
        this.blockFactory = blockFactory;
        this.scoreService = scoreService;
        this.levelService = levelService;
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
        nextBlock = blockFactory.createRandomBlock(difficulty);
        blockLocked = false;
        clearedLines = 0;
        skipsUsed = 0;
        isItemBlockTurn = false;
        pendingLineClear = null;
        weightCollided = false;

        paused = false;
        gameOver = false;
    }

    //틱당 실행할 메소드
    public void tick() {
        isItemBlockTurn = Math.random() < 0.5; // 테스트용 라인입니당 ^_^

        // 지난 틱에서 미뤄진 라인 삭제가 있으면 이번 틱에 처리만 하고 끝
        if (pendingLineClear != null) {
            ClearResult pending = pendingLineClear;
            pendingLineClear = null;
            board.applyClear(pending);
            applyScoreAndCounter(
                    pending.getScoreLineCount(),
                    pending.getItemCounterLineCount()
            );
            return;
        }

        // 현재 블럭이 없으면 다음 블럭을 올리고, 다음 블럭 새로 생성
        if (currentBlock == null) {
            if (isItemBlockTurn && itemMode) {
                currentBlock = blockFactory.createItemBlock();
                isItemBlockTurn = false;
            } else {
                currentBlock = nextBlock;
                nextBlock = blockFactory.createRandomBlock(difficulty);
            }
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
                board.applyClear(itemResult);
                applyScoreAndCounter(
                        itemResult.getScoreLineCount(),
                        itemResult.getItemCounterLineCount()
                );
            }

            ClearResult pending = board.findFullLines();
            if (!pending.isEmpty()) {
                pendingLineClear = pending;
            }

            currentBlock = null;
            blockLocked = false;
        }
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
        if (!hasCurrentBlock()) {
            return false;
        }
        if (board.canRotate(currentBlock)) {
            currentBlock.rotate();
            return true;
        }
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

    public long getDropIntervalMillis() {
        return levelService.getDropIntervalMillis();
    }
}