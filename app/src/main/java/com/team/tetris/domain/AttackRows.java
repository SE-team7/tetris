package com.team.tetris.domain;

import java.io.Serializable;

/**
 * 공격 행 묶음.
 *
 * TODO(1번): 임시 파일. 1번 구현으로 교체.
 */
public class AttackRows implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final AttackRows EMPTY = new AttackRows(new BlockType[0][]);

    private final BlockType[][] rows;

    public AttackRows(BlockType[][] rows) {
        this.rows = rows;
    }

    public static AttackRows empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return rows.length == 0;
    }

    public int getRowCount() {
        return rows.length;
    }

    public BlockType[][] getRows() {
        return rows;
    }
}
