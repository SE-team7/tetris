package com.team.tetris.domain;

import org.junit.jupiter.api.Test;

public class BlockRotateTest {
    @Test
    void showRotationWithPosition() {
        for (BlockType type : BlockType.values()) {
            Block block = new Block(type, new Position(10, 5));

            System.out.println("\n================ " + type + " ================");

            for (int i = 0; i < 4; i++) {
                System.out.println("\nrotation = " + block.getRotation() * 90 + "도");
                System.out.println(
                        "position = (" +
                        block.getPosition().getRow() + ", " +
                        block.getPosition().getCol() + ")"
                );

                printBoardArea(block);

                block.rotate();
            }
        }
    }

    private void printBoardArea(Block block) {
        Position[] cells = block.getCells();

        int centerRow = block.getPosition().getRow();
        int centerCol = block.getPosition().getCol();

        for (int row = centerRow - 4; row <= centerRow + 2; row++) {
            for (int col = centerCol - 4; col <= centerCol + 4; col++) {

                boolean filled = false;

                for (Position cell : cells) {
                    if (cell.getRow() == row && cell.getCol() == col) {
                        filled = true;
                        break;
                    }
                }

                if (row == centerRow && col == centerCol) {
                    System.out.print("P ");
                } else if (filled) {
                    System.out.print("x ");
                } else {
                    System.out.print(". ");
                }
            }

            System.out.println();
        }
    }
}
