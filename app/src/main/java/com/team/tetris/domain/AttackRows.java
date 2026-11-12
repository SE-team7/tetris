package com.team.tetris.domain;

import java.io.Serializable;
import java.util.Objects;

// 위→아래 순서로 공격 행 저장
public final class AttackRows implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BlockType[][] rows;

    public AttackRows(BlockType[][] rows) {
        Objects.requireNonNull(rows, "rows must not be null");

        this.rows = new BlockType[rows.length][Board.COLS];

        for (int row = 0; row < rows.length; row++) {
            if (rows[row] == null
                    || rows[row].length != Board.COLS) {
                throw new IllegalArgumentException(
                        "each attack row must contain "
                                + Board.COLS + " cells"
                );
            }

            System.arraycopy(
                    rows[row], 0,
                    this.rows[row], 0,
                    Board.COLS
            );
        }
    }

    public static AttackRows empty() {
        return new AttackRows(new BlockType[0][Board.COLS]);
    }

    public boolean isEmpty() {
        return rows.length == 0;
    }

    public int getRowCount() {
        return rows.length;
    }

    // 내부 배열을 직접 노출하지 않고 복사본 반환
    public BlockType[][] getRows() {
        BlockType[][] copy = new BlockType[rows.length][Board.COLS];

        for (int row = 0; row < rows.length; row++) {
            System.arraycopy(
                    rows[row], 0,
                    copy[row], 0,
                    Board.COLS
            );
        }

        return copy;
    }
}