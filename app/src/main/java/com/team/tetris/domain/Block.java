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

    // 초기 모양에서 고정된 회전축을 기준으로 시계방향 회전
    private int[][] getOffsets(BlockType blockType, int targetRotation) {
        if (targetRotation < 0 || targetRotation > 3) {
            throw new IllegalStateException(
                    "Unexpected rotation: " + targetRotation
            );
        }

        int[][] offsets = switch (blockType) {
            case I -> new int[][]{
                    {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}
            };
            case O -> new int[][]{
                    {-1, 0}, {-1, 1}, {0, 0}, {0, 1}
            };
            case J -> new int[][]{
                    {-2, 1}, {-1, 1}, {0, 0}, {0, 1}
            };
            case L -> new int[][]{
                    {-2, 0}, {-1, 0}, {0, 0}, {0, 1}
            };
            case S -> new int[][]{
                    {-1, 1}, {-1, 2}, {0, 0}, {0, 1}
            };
            case Z -> new int[][]{
                    {-1, -1}, {-1, 0}, {0, 0}, {0, 1}
            };
            case T -> new int[][]{
                    {-1, -1}, {-1, 0}, {-1, 1}, {0, 0}
            };
        };

        // O는 회전해도 차지하는 셀이 동일
        if (blockType == BlockType.O) {
            return offsets;
        }

        
        int[] pivot2 = switch (blockType) {
            case I -> new int[]{-3, 1};
            case J, S -> new int[]{-2, 2};
            case L, Z, T -> new int[]{-2, 0};
            case O -> throw new IllegalStateException(
                    "O block does not require rotation"
            );
        };

        for (int turn = 0; turn < targetRotation; turn++) {
            for (int[] offset : offsets) {
                int relativeRow2 = offset[0] * 2 - pivot2[0];
                int relativeCol2 = offset[1] * 2 - pivot2[1];

                // row는 아래로, col은 오른쪽으로 증가
                // 시계방향 90도: (row, col) → (col, -row)
                offset[0] = (pivot2[0] + relativeCol2) / 2;
                offset[1] = (pivot2[1] - relativeRow2) / 2;
            }
        }

        return offsets;
    }
}