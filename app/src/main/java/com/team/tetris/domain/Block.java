package com.team.tetris.domain;

public class Block {
    private final BlockType type;
    private Position position;
    private int rotation;

    public Block(BlockType type, Position position) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }

        this.type = type;
        this.position = position;
        this.rotation = 0;
    }
    //실제블록이동
    public void move(int deltaRow, int deltaCol) {
        position = new Position(position.getRow() + deltaRow, position.getCol() + deltaCol);
    }

   // 이동했을 때의 위치를 미리 계산
    // 실제 position은 변경은 x
    public Position[] getMovedCells(int deltaRow, int deltaCol) {
        Position movedPosition = new Position(
                position.getRow() + deltaRow,
                position.getCol() + deltaCol
        );

        return getCells(movedPosition, rotation);
    }

    // 실제 회전
    public void rotate() {
        if (type == BlockType.O) {
            return;
        }
        rotation = (rotation+1)%4;
    }

    // 회전했을 때의 셀 위치를 미리 계산
    // 실제 rotation은 변경하지 않음
    public Position[] getRotatedCells() {
        if (type == BlockType.O) {
            return getCells();
        }

        int nextRotation = (rotation+1)%4;

        return getCells(position, nextRotation);
    }

    //현재 블록이 있는 칸
    public Position[] getCells() {
        return getCells(position, rotation);
    }

    public BlockType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public int getRotation() {
        return rotation;
    }


    // 기준 위치 + offset으로 실제 셀 좌표 계산
    private Position[] getCells(Position basePosition, int targetRotation) {
        int[][] offsets = getOffsets(type, targetRotation);
        Position[] cells = new Position[offsets.length];

        for (int i = 0; i < offsets.length; i++) {
            cells[i] = new Position(
                    basePosition.getRow() + offsets[i][0],
                    basePosition.getCol() + offsets[i][1]
            );
        }

        return cells;
    }

    //블록 타입 회전 시 상대좌표
    private int[][] getOffsets(BlockType blockType, int targetRotation) {
        return switch (blockType) {
            case I -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{-3, 0}, {-2, 0}, {-1, 0}, {0, 0}};
                case 1, 3 -> new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };

            case O -> new int[][]{{-1, 0}, {-1, 1}, {0, 0}, {0, 1}};

            case J -> switch (targetRotation) {
                case 0 -> new int[][]{{-2, 1}, {-1, 1}, {0, 0}, {0, 1}};
                case 1 -> new int[][]{{-1, 0}, {0, 0}, {0, 1}, {0, 2}};
                case 2 -> new int[][]{{-2, 0}, {-2, 1}, {-1, 0}, {0, 0}};
                case 3 -> new int[][]{{-1, -2}, {-1, -1}, {-1, 0}, {0, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };

            case L -> switch (targetRotation) {
                case 0 -> new int[][]{{-2, 0}, {-1, 0}, {0, 0}, {0, 1}};
                case 1 -> new int[][]{{-1, 0}, {-1, 1}, {-1, 2}, {0, 0}};
                case 2 -> new int[][]{{-2, -1}, {-2, 0}, {-1, 0}, {0, 0}};
                case 3 -> new int[][]{{-1, 2}, {0, 0}, {0, 1}, {0, 2}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };

            case S -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{-1, 1}, {-1, 2}, {0, 0}, {0, 1}};
                case 1, 3 -> new int[][]{{-2, -1}, {-1, -1}, {-1, 0}, {0, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };

            case Z -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{-1, -1}, {-1, 0}, {0, 0}, {0, 1}};
                case 1, 3 -> new int[][]{{-2, 1}, {-1, 0}, {-1, 1}, {0, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };

            case T -> switch (targetRotation) {
                case 0 -> new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, 0}};
                case 1 -> new int[][]{{-2, 0}, {-1, -1}, {-1, 0}, {0, 0}};
                case 2 -> new int[][]{{-1, 1}, {0, 0}, {0, 1}, {0, 2}};
                case 3 -> new int[][]{{-2, 0}, {-1, 0}, {-1, 1}, {0, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
        
        };
    }
}
