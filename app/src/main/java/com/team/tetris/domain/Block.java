package com.team.tetris.domain;

public class Block {
    private final BlockType type;
    private Position position;
    private int rotation;
    private final ItemType itemType;
    private final int itemCellIndex; //LineClear 가 붙은 셀 인덱스

    //일반블록이랑 단독 아이템 생성자
    public Block(BlockType type, Position position) {
        this(type, position, defaultItemType(type), -1);
    }
    
    //lineClear도 처리
    public Block(
            BlockType type,
            Position position,
            ItemType itemType,
            int itemCellIndex
    ) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (itemType == null) {
            throw new IllegalArgumentException("itemType must not be null");
        }

        ItemType expectedItemType = defaultItemType(type);

        if (itemType == ItemType.LINE_CLEAR) {
            if (expectedItemType != ItemType.NONE) {
                throw new IllegalArgumentException(
                        "LINE_CLEAR must be attached to a normal block"
                );
            }
            if (itemCellIndex < 0 || itemCellIndex >= 4) {
                throw new IllegalArgumentException(
                        "itemCellIndex must be between 0 and 3"
                );
            }
        } else {
            if (itemType != expectedItemType) {
                throw new IllegalArgumentException(
                        "block type and item type do not match"
                );
            }
            if (itemCellIndex != -1) {
                throw new IllegalArgumentException(
                        "itemCellIndex must be -1 unless LINE_CLEAR"
                );
            }
        }

        this.type = type;
        this.position = position;
        this.rotation = 0;
        this.itemType = itemType;
        this.itemCellIndex = itemCellIndex;
    }

    //블록 종류에 따른 기본 아이템 종류 결정
    private static ItemType defaultItemType(BlockType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        return switch (type) {
            case WEIGHT -> ItemType.WEIGHT;
            case BOMB -> ItemType.BOMB;
            case CROSS -> ItemType.CROSS;
            case GAP_DESTROY -> ItemType.GAP_DESTROY;
            default -> ItemType.NONE;
        };
    }
    
    //아이템 종류 반환, 일반 블록은 NONE
    public ItemType getItemType() {
        return itemType;
    }

    //아이템 포함여부 반환
    public boolean hasItem() {
        return itemType != ItemType.NONE;
    }

    //아이템 기준칸의 실제 좌표, 일반블록은 null 반환
    public Position getItemPosition() {
        if (!hasItem()) {
            return null;
        }

        Position[] currentCells = getCells();

        if (itemType == ItemType.LINE_CLEAR) {
            return currentCells[itemCellIndex];
        }

        //W와 D는 왼쪽 칸, B와 CROSS는 유일한 칸을 기준으로 사용
        return currentCells[0];
    }

    //돌려도 똑같은 블럭, 회전 금지 블럭은 회전 불가
    public boolean canRotate() {
        return switch (type) {
            case O, WEIGHT, BOMB, CROSS, GAP_DESTROY -> false;
            default -> true;
        };
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
        if (!canRotate()) {
            return;
        }
        rotation = (rotation+1)%4;
    }

    // 회전했을 때의 셀 위치를 미리 계산
    // 실제 rotation은 변경하지 않음
    public Position[] getRotatedCells() {
        if (!canRotate()) {
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
            //아이템 블록
            case WEIGHT -> new int[][]{
                    {0, -1}, {0, 0}, {0, 1}, {0, 2}, {-1,0}, {-1,1}
            };
            case BOMB, CROSS -> new int[][]{
                    {0, 0}
            };
            case GAP_DESTROY -> new int[][]{
                    {0, 0}, {0, 1}
            };
        };

        //회전해도 똑같은 애들 
        if (blockType == BlockType.O
            || blockType == BlockType.WEIGHT
            || blockType == BlockType.BOMB
            || blockType == BlockType.CROSS
            || blockType == BlockType.GAP_DESTROY) {
        return offsets;
        }

        
        int[] pivot2 = switch (blockType) {
            case I -> new int[]{-3, 1};
            case J, S -> new int[]{-2, 2};
            case L, Z, T -> new int[]{-2, 0};
            case O, WEIGHT, BOMB, CROSS, GAP_DESTROY -> throw new IllegalStateException(
                    "Block does not require rotation"
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