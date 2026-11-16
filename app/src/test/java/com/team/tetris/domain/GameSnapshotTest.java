package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

// [대전 모드] 게임 상태 스냅샷이 원본과 분리된 복사본인지 검증
class GameSnapshotTest {

    private BlockType[][] emptyBoard() {
        return new BlockType[Board.ROWS][Board.COLS];
    }

    private GameSnapshot snapshot(BlockType[][] board, boolean[][] attack, Block current) {
        return new GameSnapshot(board, attack, current,
                new Block(BlockType.I, new Position(0, 3)), 120, 2, 5, null, false);
    }

    @Test
    void returnsGivenValues() {
        GameSnapshot snap = snapshot(emptyBoard(), new boolean[Board.ROWS][Board.COLS],
                new Block(BlockType.T, new Position(4, 5)));

        assertEquals(120, snap.getScore());
        assertEquals(2, snap.getLevel());
        assertEquals(5, snap.getRemainingSkips());
        assertFalse(snap.isGameOver());
        assertTrue(snap.getPendingAttack().isEmpty(), "대기 공격이 없으면 빈 값");
        assertEquals(BlockType.T, snap.getCurrentBlock().getType());
        assertEquals(BlockType.I, snap.getNextBlock().getType());
    }

    @Test
    void changingSourceAfterCreationDoesNotChangeSnapshot() {
        BlockType[][] board = emptyBoard();
        boolean[][] attack = new boolean[Board.ROWS][Board.COLS];
        Block current = new Block(BlockType.T, new Position(4, 5));
        GameSnapshot snap = snapshot(board, attack, current);

        board[24][0] = BlockType.O;
        attack[24][0] = true;
        current.move(1, 0);

        assertNull(snap.getBoard()[24][0]);
        assertFalse(snap.getAttackCells()[24][0]);
        assertEquals(4, snap.getCurrentBlock().getPosition().getRow());
    }

    @Test
    void changingReturnedValuesDoesNotChangeSnapshot() {
        GameSnapshot snap = snapshot(emptyBoard(), new boolean[Board.ROWS][Board.COLS],
                new Block(BlockType.T, new Position(4, 5)));

        snap.getBoard()[0][0] = BlockType.Z;
        snap.getAttackCells()[0][0] = true;
        snap.getCurrentBlock().move(3, 0);

        assertNull(snap.getBoard()[0][0]);
        assertFalse(snap.getAttackCells()[0][0]);
        assertEquals(4, snap.getCurrentBlock().getPosition().getRow());
        assertNotSame(snap.getCurrentBlock(), snap.getCurrentBlock());
    }

    @Test
    void missingBlocksAreNull() {
        GameSnapshot snap = new GameSnapshot(emptyBoard(), new boolean[Board.ROWS][Board.COLS],
                null, null, 0, 1, 7, AttackRows.empty(), true);

        assertNull(snap.getCurrentBlock());
        assertNull(snap.getNextBlock());
        assertTrue(snap.isGameOver());
    }

    @Test
    void canBeSerializedForNetwork() throws Exception {
        Block current = new Block(BlockType.L, new Position(6, 2));
        current.rotate();
        GameSnapshot snap = snapshot(emptyBoard(), new boolean[Board.ROWS][Board.COLS], current);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(snap);
        }
        GameSnapshot restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (GameSnapshot) in.readObject();
        }

        assertEquals(snap.getScore(), restored.getScore());
        assertEquals(BlockType.L, restored.getCurrentBlock().getType());
        assertEquals(1, restored.getCurrentBlock().getRotation(), "회전 상태도 유지");
        assertEquals(6, restored.getCurrentBlock().getPosition().getRow());
    }
}
