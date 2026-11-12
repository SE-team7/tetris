package com.team.tetris.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Position;

// 콜백 전송, 로컬 상대 연결, 보드 적용, 상단 초과 반환, 초기화 검증
class AttackServiceTest {

    private AttackRows attack(int count) {
        BlockType[][] rows = new BlockType[count][Board.COLS];

        for (int i = 0; i < count; i++) {
            rows[i][0] = BlockType.BOMB;
        }

        return new AttackRows(rows);
    }

    @Test
    void sendPassesAttackToSender() {
        AtomicReference<AttackRows> received = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        AttackService service = new AttackService(
                new Board(),
                new AttackQueue(),
                rows -> {
                    received.set(rows);
                    calls.incrementAndGet();
                }
        );

        service.sendAttack(attack(2));

        assertEquals(1, calls.get());
        assertEquals(2, received.get().getRowCount());
        assertEquals(BlockType.BOMB, received.get().getRows()[0][0]);
        assertEquals(0, service.getPendingRowCount());
    }

    @Test
    void emptyAttackIsNotSent() {
        AtomicInteger calls = new AtomicInteger();

        AttackService service = new AttackService(
                new Board(),
                new AttackQueue(),
                rows -> calls.incrementAndGet()
        );

        service.sendAttack(AttackRows.empty());

        assertEquals(0, calls.get());
    }

    @Test
    void sendingWithoutSenderDoesNotFail() {
        AttackService service = new AttackService(
                new Board(),
                new AttackQueue(),
                null
        );

        assertDoesNotThrow(() -> service.sendAttack(attack(2)));
    }

    @Test
    void localConnectionQueuesAttackOnlyForOpponent() {
        AttackQueue player1Queue = new AttackQueue();
        AttackQueue player2Queue = new AttackQueue();

        AttackService player2 = new AttackService(
                new Board(),
                player2Queue,
                null
        );

        AttackService player1 = new AttackService(
                new Board(),
                player1Queue,
                player2::receiveAttack
        );

        player1.sendAttack(attack(2));

        assertEquals(0, player1.getPendingRowCount());
        assertEquals(2, player2.getPendingRowCount());
        assertEquals(2, player2Queue.getRowCount());
    }

    @Test
    void receiveQueuesAttackUntilApplyIsCalled() {
        Board board = new Board();
        AttackService service = new AttackService(
                board,
                new AttackQueue(),
                null
        );

        service.receiveAttack(attack(2));

        assertEquals(2, service.getPendingRowCount());
        assertEquals(2, service.getPendingAttack().getRowCount());

        // 수신만으로 보드가 변경되지는 않음
        for (BlockType[] row : board.getCells()) {
            assertArrayEquals(new BlockType[Board.COLS], row);
        }

        assertFalse(service.applyAttack());

        assertEquals(
                BlockType.BOMB,
                board.getCells()[Board.ROWS - 2][0]
        );
        assertEquals(
                BlockType.BOMB,
                board.getCells()[Board.ROWS - 1][0]
        );
        assertEquals(0, service.getPendingRowCount());

        // 같은 공격을 다시 적용하지 않음
        BlockType[][] before = board.getCells();
        assertFalse(service.applyAttack());

        BlockType[][] after = board.getCells();
        for (int row = 0; row < Board.ROWS; row++) {
            assertArrayEquals(before[row], after[row]);
        }
    }

    @Test
    void applyReturnsTrueWhenFixedCellOverflowsTop() {
        Board board = new Board();
        AttackService service = new AttackService(
                board,
                new AttackQueue(),
                null
        );

        board.lockBlock(
                new Block(BlockType.CROSS, new Position(0, 3))
        );
        service.receiveAttack(attack(2));

        assertTrue(service.applyAttack());
        assertEquals(0, service.getPendingRowCount());
    }

    @Test
    void resetClearsPendingAttackButKeepsSender() {
        AtomicInteger calls = new AtomicInteger();

        AttackService service = new AttackService(
                new Board(),
                new AttackQueue(),
                rows -> calls.incrementAndGet()
        );

        service.receiveAttack(attack(2));
        service.reset();

        assertEquals(0, service.getPendingRowCount());
        assertEquals(0, service.getPendingAttack().getRowCount());

        service.sendAttack(attack(2));
        assertEquals(1, calls.get());
    }
}