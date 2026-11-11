package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

//공격 데이터의 행 수, 생성 및 조회시 배열 복사 검증
class AttackRowsTest {

    @Test
    void emptyHasNoRows() {
        AttackRows attack = AttackRows.empty();

        assertEquals(0, attack.getRowCount());
        assertEquals(0, attack.getRows().length);
    }

    @Test
    void preservesCellsAndEmptySpaces() {
        BlockType[][] source = new BlockType[2][Board.COLS];
        source[0][0] = BlockType.BOMB;
        source[1][3] = BlockType.CROSS;

        AttackRows attack = new AttackRows(source);

        assertEquals(2, attack.getRowCount());
        assertArrayEquals(source[0], attack.getRows()[0]);
        assertArrayEquals(source[1], attack.getRows()[1]);
    }

    @Test
    void changingSourceArrayDoesNotChangeAttack() {
        BlockType[][] source = new BlockType[1][Board.COLS];
        source[0][0] = BlockType.BOMB;

        AttackRows attack = new AttackRows(source);

        source[0][0] = BlockType.CROSS;
        source[0] = new BlockType[Board.COLS];

        assertEquals(BlockType.BOMB, attack.getRows()[0][0]);
    }

    @Test
    void changingReturnedArrayDoesNotChangeAttack() {
        BlockType[][] source = new BlockType[1][Board.COLS];
        source[0][0] = BlockType.BOMB;

        AttackRows attack = new AttackRows(source);
        BlockType[][] returned = attack.getRows();

        returned[0][0] = BlockType.CROSS;
        returned[0] = new BlockType[Board.COLS];

        assertEquals(BlockType.BOMB, attack.getRows()[0][0]);
    }
}