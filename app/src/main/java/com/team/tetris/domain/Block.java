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

    public void move(int deltaRow, int deltaCol) {
        position = new Position(position.getRow() + deltaRow, position.getCol() + deltaCol);
    }

    public void rotate() {
        rotation = normalizeRotation(rotation + 1);
    }

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

    private Position[] getCells(Position basePosition, int targetRotation) {
        int[][] offsets = getOffsets(type, normalizeRotation(targetRotation));
        Position[] cells = new Position[offsets.length];

        for (int i = 0; i < offsets.length; i++) {
            cells[i] = new Position(
                    basePosition.getRow() + offsets[i][0],
                    basePosition.getCol() + offsets[i][1]
            );
        }

        return cells;
    }

    private int normalizeRotation(int value) {
        return Math.floorMod(value, 4);
    }

    private int[][] getOffsets(BlockType blockType, int targetRotation) {
        return switch (blockType) {
            case I -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {0, 2}};
                case 1, 3 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {2, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
            case O -> new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}};
            case T -> switch (targetRotation) {
                case 0 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {1, 0}};
                case 1 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {0, -1}};
                case 2 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {-1, 0}};
                case 3 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {0, 1}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
            case S -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{0, 0}, {0, 1}, {1, -1}, {1, 0}};
                case 1, 3 -> new int[][]{{-1, 0}, {0, 0}, {0, 1}, {1, 1}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
            case Z -> switch (targetRotation) {
                case 0, 2 -> new int[][]{{0, -1}, {0, 0}, {1, 0}, {1, 1}};
                case 1, 3 -> new int[][]{{-1, 1}, {0, 0}, {0, 1}, {1, 0}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
            case J -> switch (targetRotation) {
                case 0 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {1, -1}};
                case 1 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, -1}};
                case 2 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {-1, 1}};
                case 3 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {1, 1}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
            case L -> switch (targetRotation) {
                case 0 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {1, 1}};
                case 1 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {1, -1}};
                case 2 -> new int[][]{{0, -1}, {0, 0}, {0, 1}, {-1, -1}};
                case 3 -> new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}};
                default -> throw new IllegalStateException("Unexpected rotation: " + targetRotation);
            };
        };
    }
}
