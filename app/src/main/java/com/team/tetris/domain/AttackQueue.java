package com.team.tetris.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;


// 플레이어별 공격 수신 대기열
public final class AttackQueue {
    private static final int MAX_ROWS = 10;

    // 앞쪽이 위 행, 뒤쪽이 아래 행
    private final Deque<BlockType[]> rows = new ArrayDeque<>();

    /*
     * 기존 대기 행 아래에 새 행 추가.
     * 최대 10줄이며 아래쪽 초과분은 버림.
     */
    public synchronized void add(AttackRows attackRows) {
        Objects.requireNonNull(
                attackRows,
                "attackRows must not be null"
        );

        if (rows.size() == MAX_ROWS) {
            return;
        }

        // getRows()가 복사본을 반환하므로 외부 배열과 분리됨
        for (BlockType[] row : attackRows.getRows()) {
            if (rows.size() == MAX_ROWS) {
                break;
            }

            rows.addLast(row);
        }
    }

    // 대기열을 변경하지 않고 표시용 복사본 반환
    public synchronized AttackRows snapshot() {
        BlockType[][] copy = rows.toArray(
                new BlockType[rows.size()][Board.COLS]
        );

        return new AttackRows(copy);
    }

    // 현재 대기 행 반환과 초기화를 하나의 동작으로 처리
    public synchronized AttackRows drain() {
        AttackRows result = snapshot();
        rows.clear();
        return result;
    }

    public synchronized int getRowCount() {
        return rows.size();
    }

    public synchronized boolean isEmpty() {
        return rows.isEmpty();
    }

    public synchronized void clear() {
        rows.clear();
    }
}
