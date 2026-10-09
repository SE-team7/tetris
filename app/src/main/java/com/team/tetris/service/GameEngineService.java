package com.team.tetris.service;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;

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
    public void startGame() {
        board.reset();
        scoreService.reset();
        levelService.reset();

        currentBlock = null;
        nextBlock = blockFactory.createRandomBlock();
        blockLocked = false;
        clearedLines = 0;
        skipsUsed = 0;

        paused = false;
        gameOver = false;
    }

    //틱당 실행할 메소드
    public void tick() {
        // 현재 블럭이 없으면 다음 블럭을 올리고, 다음 블럭 새로 생성
        if (currentBlock == null) {
            currentBlock = nextBlock;
            nextBlock = blockFactory.createRandomBlock();

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

        // 블럭이 고정됐으면 줄 지우기, 현재 블럭 null
        if (blockLocked) {
            int lines = board.clearLines();
            if (lines > 0) {
                clearedLines += lines;

                int lineScore = scoreService.calculateLineClearScore(lines);
                scoreService.addLineScore(lineScore);

                int levelBefore = levelService.getLevel();
                levelService.updateLevel(scoreService.getLineScore());
                int levelUpCount = levelService.getLevel() - levelBefore;
                if (levelUpCount > 0) {
                    scoreService.addSpeedBonus(levelUpCount);
                }
            }

            currentBlock = null;
            blockLocked = false;
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
        if (board.canMove(currentBlock, 1, 0)) {
            currentBlock.move(1, 0);
            scoreService.addDropScore(1);
            return true;
        }
        board.lockBlock(currentBlock);
        blockLocked = true;
        return false;
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