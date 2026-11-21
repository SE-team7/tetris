package com.team.tetris.domain;

import java.io.Serializable;

/**
 * 한 시점의 게임 상태 복사본.
 * 화면 표시(3번)와 P2P 상태 전송(4번)에 사용한다.
 * 생성 시점과 조회 시점 모두 복사하므로 바깥에서 바꿔도 원본 게임에 영향이 없다.
 */
public final class GameSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BlockType[][] board;
    private final boolean[][] attackCells;
    private final Block currentBlock;
    private final Block nextBlock;
    private final int score;
    private final int level;
    private final int remainingSkips;
    private final AttackRows pendingAttack;
    private final boolean gameOver;

    public GameSnapshot(BlockType[][] board,
                        boolean[][] attackCells,
                        Block currentBlock,
                        Block nextBlock,
                        int score,
                        int level,
                        int remainingSkips,
                        AttackRows pendingAttack,
                        boolean gameOver) {
        this.board = copy(board);
        this.attackCells = copy(attackCells);
        this.currentBlock = currentBlock == null ? null : currentBlock.copy();
        this.nextBlock = nextBlock == null ? null : nextBlock.copy();
        this.score = score;
        this.level = level;
        this.remainingSkips = remainingSkips;
        this.pendingAttack = pendingAttack == null ? AttackRows.empty() : pendingAttack;
        this.gameOver = gameOver;
    }

    // 고정 보드 복사본 (빈칸은 null)
    public BlockType[][] getBoard() {
        return copy(board);
    }

    // 공격으로 들어온 고정 셀이면 true
    public boolean[][] getAttackCells() {
        return copy(attackCells);
    }

    // 현재 블럭 복사본, 없으면 null
    public Block getCurrentBlock() {
        return currentBlock == null ? null : currentBlock.copy();
    }

    // NEXT 블럭 복사본, 없으면 null
    public Block getNextBlock() {
        return nextBlock == null ? null : nextBlock.copy();
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getRemainingSkips() {
        return remainingSkips;
    }

    // 표시용 대기 공격 (없으면 빈 AttackRows)
    public AttackRows getPendingAttack() {
        return pendingAttack;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private static BlockType[][] copy(BlockType[][] source) {
        BlockType[][] result = new BlockType[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }

    private static boolean[][] copy(boolean[][] source) {
        boolean[][] result = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }
}
