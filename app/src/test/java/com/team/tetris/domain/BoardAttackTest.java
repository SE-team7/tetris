package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

//2줄 이상 삭제될때 공격 생성, 마지막 블록 제외, 삽입과 표시 이동, 상단 초과 검증
class BoardAttackTest {

    private void putCell(Board board, int row, int col, BlockType type) {
        board.lockBlock(new Block(type, new Position(row, col)));
    }

    private void fillRow(Board board, int row) {
        for (int col = 0; col < Board.COLS; col++) {
            putCell(board, row, col, BlockType.BOMB);
        }
    }

    @Test
    void singleLineDoesNotCreateAttack() {
        Board board = new Board();
        fillRow(board, 24);

        AttackRows attack = board.createAttackRows(
                board.findFullLines(),
                new Position[] { new Position(24, 3) }
        );

        assertEquals(0, attack.getRowCount());
    }

    @Test
    void twoLinesCreateAttackExcludingLastLockedCells() {
        Board board = new Board();
        fillRow(board, 23);
        fillRow(board, 24);

        AttackRows attack = board.createAttackRows(
                board.findFullLines(),
                new Position[] {
                        new Position(23, 3),
                        new Position(24, 3)
                }
        );

        assertEquals(2, attack.getRowCount());

        BlockType[][] rows = attack.getRows();

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                assertEquals(
                        col == 3 ? null : BlockType.BOMB,
                        rows[row][col]
                );
            }
        }

        // 공격 생성 시점에는 원본 보드를 변경하지 않음
        assertEquals(BlockType.BOMB, board.getCells()[23][3]);
        assertEquals(BlockType.BOMB, board.getCells()[24][3]);
    }

    @Test
    void insertingAttackMovesFixedCellsUpAndMarksIncomingCells() {
        Board board = new Board();
        putCell(board, 20, 4, BlockType.CROSS);

        BlockType[][] incoming = new BlockType[2][Board.COLS];
        incoming[0][1] = BlockType.BOMB;
        incoming[1][2] = BlockType.CROSS;

        assertFalse(board.insertAttackRows(new AttackRows(incoming)));

        assertEquals(BlockType.CROSS, board.getCells()[18][4]);
        assertNull(board.getCells()[20][4]);
        assertFalse(board.isAttackCell(18, 4));

        assertEquals(BlockType.BOMB, board.getCells()[23][1]);
        assertEquals(BlockType.CROSS, board.getCells()[24][2]);
        assertTrue(board.isAttackCell(23, 1));
        assertTrue(board.isAttackCell(24, 2));

        assertNull(board.getCells()[23][0]);
        assertFalse(board.isAttackCell(23, 0));
    }

    @Test
    void laterAttackMovesExistingAttackMarkersUp() {
        Board board = new Board();

        BlockType[][] first = new BlockType[1][Board.COLS];
        first[0][1] = BlockType.BOMB;
        board.insertAttackRows(new AttackRows(first));

        BlockType[][] second = new BlockType[1][Board.COLS];
        second[0][2] = BlockType.CROSS;
        board.insertAttackRows(new AttackRows(second));

        assertEquals(BlockType.BOMB, board.getCells()[23][1]);
        assertTrue(board.isAttackCell(23, 1));

        assertEquals(BlockType.CROSS, board.getCells()[24][2]);
        assertTrue(board.isAttackCell(24, 2));

        assertNull(board.getCells()[24][1]);
        assertFalse(board.isAttackCell(24, 1));
    }

    @Test
    void insertingAttackDetectsCellsPushedOutsideBoard() {
        Board board = new Board();
        putCell(board, 0, 3, BlockType.BOMB);

        BlockType[][] incoming = new BlockType[1][Board.COLS];
        incoming[0][1] = BlockType.CROSS;

        assertTrue(board.insertAttackRows(new AttackRows(incoming)));
    }

    @Test
    void cellAtBoundaryMovesToTopWithoutOverflow() {
        Board board = new Board();
        putCell(board, 2, 3, BlockType.BOMB);

        BlockType[][] incoming = new BlockType[2][Board.COLS];
        incoming[0][1] = BlockType.CROSS;
        incoming[1][1] = BlockType.CROSS;

        assertFalse(board.insertAttackRows(new AttackRows(incoming)));
        assertEquals(BlockType.BOMB, board.getCells()[0][3]);
    }

    @Test
    void normalLineClearMovesAttackMarkersWithCells() {
        Board board = new Board();

        BlockType[][] incoming = new BlockType[2][Board.COLS];
        incoming[0][1] = BlockType.BOMB;

        // 가장 아래 공격 행은 한 칸만 비워 둠
        for (int col = 0; col < Board.COLS - 1; col++) {
            incoming[1][col] = BlockType.BOMB;
        }

        board.insertAttackRows(new AttackRows(incoming));
        putCell(board, 24, Board.COLS - 1, BlockType.CROSS);

        board.applyClear(board.findFullLines());

        assertEquals(BlockType.BOMB, board.getCells()[24][1]);
        assertTrue(board.isAttackCell(24, 1));

        assertNull(board.getCells()[23][1]);
        assertFalse(board.isAttackCell(23, 1));
    }
}