package com.team.tetris.domain;

import java.io.Serializable;

public final class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
