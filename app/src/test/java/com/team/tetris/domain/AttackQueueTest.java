package com.team.tetris.domain;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

//10줄 제한, 추가 순서, 아래쪽 초과분 제거, 꺼내기, 동시 수신 검증
class AttackQueueTest {

    private AttackRows attack(int count, BlockType type) {
        BlockType[][] rows = new BlockType[count][Board.COLS];

        for (int i = 0; i < count; i++) {
            rows[i][0] = type;
        }

        return new AttackRows(rows);
    }

    @Test
    void newQueueIsEmpty() {
        AttackQueue queue = new AttackQueue();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.getRowCount());
        assertEquals(0, queue.snapshot().getRowCount());
    }

    @Test
    void addsNewRowsBelowExistingRows() {
        AttackQueue queue = new AttackQueue();

        queue.add(attack(2, BlockType.BOMB));
        queue.add(attack(3, BlockType.CROSS));

        BlockType[][] rows = queue.snapshot().getRows();

        assertEquals(5, rows.length);
        assertEquals(BlockType.BOMB, rows[0][0]);
        assertEquals(BlockType.BOMB, rows[1][0]);

        for (int i = 2; i < 5; i++) {
            assertEquals(BlockType.CROSS, rows[i][0]);
        }
    }

    @Test
    void discardsBottomRowsWhenTotalExceedsTen() {
        AttackQueue queue = new AttackQueue();
        queue.add(attack(7, BlockType.BOMB));

        BlockType[][] incoming = new BlockType[4][Board.COLS];
        incoming[0][0] = BlockType.I;
        incoming[1][0] = BlockType.O;
        incoming[2][0] = BlockType.T;
        incoming[3][0] = BlockType.Z;

        queue.add(new AttackRows(incoming));

        BlockType[][] rows = queue.snapshot().getRows();

        assertEquals(10, rows.length);

        for (int i = 0; i < 7; i++) {
            assertEquals(BlockType.BOMB, rows[i][0]);
        }

        assertEquals(BlockType.I, rows[7][0]);
        assertEquals(BlockType.O, rows[8][0]);
        assertEquals(BlockType.T, rows[9][0]);
        //가장 아래쪽 Z 행은 추가되지 않음
    }

    @Test
    void ignoresNewAttackWhenAlreadyFull() {
        AttackQueue queue = new AttackQueue();
        queue.add(attack(10, BlockType.BOMB));

        queue.add(attack(2, BlockType.CROSS));

        assertEquals(10, queue.getRowCount());

        for (BlockType[] row : queue.snapshot().getRows()) {
            assertEquals(BlockType.BOMB, row[0]);
        }
    }

    @Test
    void snapshotDoesNotConsumeOrExposePendingRows() {
        AttackQueue queue = new AttackQueue();
        queue.add(attack(2, BlockType.BOMB));

        AttackRows snapshot = queue.snapshot();
        snapshot.getRows()[0][0] = BlockType.CROSS;

        assertEquals(2, queue.getRowCount());
        assertEquals(
                BlockType.BOMB,
                queue.snapshot().getRows()[0][0]
        );
    }

    @Test
    void drainReturnsPendingRowsAndEmptiesQueue() {
        AttackQueue queue = new AttackQueue();
        queue.add(attack(3, BlockType.BOMB));

        AttackRows drained = queue.drain();

        assertEquals(3, drained.getRowCount());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.drain().getRowCount());

        // 꺼낸 후에도 새 공격을 정상적으로 받을 수 있음
        queue.add(attack(2, BlockType.CROSS));
        assertEquals(2, queue.getRowCount());
    }

    @Test
    void clearRemovesAllPendingRows() {
        AttackQueue queue = new AttackQueue();
        queue.add(attack(3, BlockType.BOMB));

        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.snapshot().getRowCount());
    }

    @Test
    void nullAttackIsRejected() {
        AttackQueue queue = new AttackQueue();

        assertThrows(NullPointerException.class, () -> queue.add(null));
        assertTrue(queue.isEmpty());
    }

    @Test
    void concurrentReceivesDoNotExceedTenRows() throws Exception {
        AttackQueue queue = new AttackQueue();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> {
                start.await();
                queue.add(attack(8, BlockType.BOMB));
                return null;
            });

            Future<?> second = executor.submit(() -> {
                start.await();
                queue.add(attack(8, BlockType.CROSS));
                return null;
            });

            start.countDown();
            first.get();
            second.get();
        }

        AttackRows result = queue.drain();

        assertEquals(10, result.getRowCount());

        // 먼저 들어온 공격 8줄 다음에 나머지 공격 2줄
        BlockType[][] rows = result.getRows();
        BlockType firstType = rows[0][0];
        BlockType secondType = firstType == BlockType.BOMB
                ? BlockType.CROSS : BlockType.BOMB;

        for (int i = 0; i < 8; i++) {
            assertEquals(firstType, rows[i][0]);
        }

        for (int i = 8; i < 10; i++) {
            assertEquals(secondType, rows[i][0]);
        }
    }
}