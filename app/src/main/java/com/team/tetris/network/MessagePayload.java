package com.team.tetris.network;

import java.util.Arrays;
import java.util.Objects;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;
import com.team.tetris.domain.Position;

/**
 * 메시지에 실어 보내는 데이터.
 * 대전 도메인 객체를 그대로 직렬화하지 않고, 전송에 필요한 값만 담는다.
 * (도메인 클래스에 Serializable을 강제하지 않고, 받은 바이트로 임의 객체가 만들어지지 않도록 함)
 * 배열은 생성·조회 시 모두 복사하므로 만든 뒤에는 바뀌지 않는다.
 */
public sealed interface MessagePayload {

    /** 상대에게 넘길 수 있는 최대 공격 줄 수 */
    int MAX_ATTACK_ROWS = 10;

    /** CONFIG: 모드·난이도·제한시간. mode는 BattleMode 이름, difficulty는 아이템 모드에서 null 가능 */
    record Config(String mode, Difficulty difficulty, int timeLimitSeconds) implements MessagePayload {
        public Config {
            Objects.requireNonNull(mode, "mode must not be null");
            if (mode.isBlank()) {
                throw new IllegalArgumentException("mode must not be blank");
            }
            if (timeLimitSeconds < 0) {
                throw new IllegalArgumentException("timeLimitSeconds must not be negative");
            }
        }
    }

    /** READY: 자신의 준비 상태 */
    record Ready(boolean ready) implements MessagePayload {
    }

    /** START: 메시지를 받은 뒤 startDelayMillis 후에 양쪽이 함께 시작 */
    record Start(long startDelayMillis) implements MessagePayload {
        public Start {
            if (startDelayMillis < 0) {
                throw new IllegalArgumentException("startDelayMillis must not be negative");
            }
        }
    }

    /** ATTACK: 위→아래 순서의 공격 행 (1~10줄, 한 줄은 Board.COLS칸, 빈칸은 null) */
    record Attack(BlockType[][] rows) implements MessagePayload {
        public Attack {
            rows = copyRows(rows, 1, MAX_ATTACK_ROWS, "rows");
        }

        @Override
        public BlockType[][] rows() {
            return copyRows(rows, 0, MAX_ATTACK_ROWS, "rows");
        }

        public int rowCount() {
            return rows.length;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Attack other && Arrays.deepEquals(rows, other.rows);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(rows);
        }

        @Override
        public String toString() {
            return "Attack[rows=" + rows.length + "]";
        }
    }

    /** SNAPSHOT: 상대 화면 표시용 게임 상태 */
    record Snapshot(
            BlockType[][] board,
            boolean[][] attackCells,
            BlockState currentBlock,
            BlockState nextBlock,
            int score,
            int level,
            int remainingSkips,
            BlockType[][] pendingAttack,
            boolean gameOver
    ) implements MessagePayload {
        public Snapshot {
            board = copyBoard(board);
            attackCells = copyAttackCells(attackCells);
            pendingAttack = copyRows(pendingAttack, 0, MAX_ATTACK_ROWS, "pendingAttack");
        }

        @Override
        public BlockType[][] board() {
            return copyBoard(board);
        }

        @Override
        public boolean[][] attackCells() {
            return copyAttackCells(attackCells);
        }

        @Override
        public BlockType[][] pendingAttack() {
            return copyRows(pendingAttack, 0, MAX_ATTACK_ROWS, "pendingAttack");
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Snapshot other
                    && Arrays.deepEquals(board, other.board)
                    && Arrays.deepEquals(attackCells, other.attackCells)
                    && Objects.equals(currentBlock, other.currentBlock)
                    && Objects.equals(nextBlock, other.nextBlock)
                    && score == other.score
                    && level == other.level
                    && remainingSkips == other.remainingSkips
                    && Arrays.deepEquals(pendingAttack, other.pendingAttack)
                    && gameOver == other.gameOver;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.deepHashCode(board), Arrays.deepHashCode(attackCells),
                    currentBlock, nextBlock, score, level, remainingSkips,
                    Arrays.deepHashCode(pendingAttack), gameOver);
        }

        @Override
        public String toString() {
            return "Snapshot[score=" + score + ", level=" + level
                    + ", pending=" + pendingAttack.length + ", gameOver=" + gameOver + "]";
        }
    }

    /** GAME_OVER: 판정 담당이 동시 게임오버 여부를 판단할 수 있도록 진행 시점 정보를 함께 보냄 */
    record GameOver(long elapsedMillis, int score) implements MessagePayload {
        public GameOver {
            if (elapsedMillis < 0) {
                throw new IllegalArgumentException("elapsedMillis must not be negative");
            }
        }
    }

    /** RESULT: 판정 담당이 확정한 결과. winner는 PlayerId 이름(승자 없으면 null), reason은 BattleEndReason 이름 */
    record Result(String winner, boolean draw, String reason) implements MessagePayload {
        public Result {
            Objects.requireNonNull(reason, "reason must not be null");
            if (draw && winner != null) {
                throw new IllegalArgumentException("draw result must not have a winner");
            }
        }
    }

    /** REMATCH_RESPONSE: 재대전 수락·거절 */
    record RematchResponse(boolean accepted) implements MessagePayload {
    }

    /** PING·PONG: 보낸 쪽의 System.nanoTime(). PONG은 받은 PING 값을 그대로 돌려줌 */
    record Ping(long timestampNanos) implements MessagePayload {
    }

    /** 블록 상태. Block은 회전값을 직접 설정할 수 없으므로 toBlock()에서 rotate()를 반복해 복원 */
    record BlockState(BlockType type, int row, int col, int rotation, ItemType itemType, int itemCellIndex) {
        public BlockState {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(itemType, "itemType must not be null");
            if (rotation < 0 || rotation > 3) {
                throw new IllegalArgumentException("rotation must be between 0 and 3");
            }
        }

        public static BlockState from(Block block) {
            if (block == null) {
                return null;
            }
            Position position = block.getPosition();
            int cellIndex = block.getItemType() == ItemType.LINE_CLEAR ? findItemCellIndex(block) : -1;
            return new BlockState(block.getType(), position.getRow(), position.getCol(),
                    block.getRotation(), block.getItemType(), cellIndex);
        }

        public Block toBlock() {
            Block block = new Block(type, new Position(row, col), itemType, itemCellIndex);
            for (int i = 0; i < rotation; i++) {
                block.rotate();
            }
            return block;
        }

        // Block은 LINE_CLEAR 셀 인덱스를 노출하지 않으므로 아이템 좌표와 같은 셀을 찾는다
        private static int findItemCellIndex(Block block) {
            Position item = block.getItemPosition();
            Position[] cells = block.getCells();
            for (int i = 0; i < cells.length; i++) {
                if (cells[i].getRow() == item.getRow() && cells[i].getCol() == item.getCol()) {
                    return i;
                }
            }
            throw new IllegalStateException("item cell not found");
        }
    }

    private static BlockType[][] copyRows(BlockType[][] rows, int minRows, int maxRows, String name) {
        Objects.requireNonNull(rows, name + " must not be null");
        if (rows.length < minRows || rows.length > maxRows) {
            throw new IllegalArgumentException(
                    name + " must contain " + minRows + " to " + maxRows + " rows");
        }
        BlockType[][] copy = new BlockType[rows.length][];
        for (int row = 0; row < rows.length; row++) {
            if (rows[row] == null || rows[row].length != Board.COLS) {
                throw new IllegalArgumentException(name + " row must contain " + Board.COLS + " cells");
            }
            copy[row] = rows[row].clone();
        }
        return copy;
    }

    private static BlockType[][] copyBoard(BlockType[][] board) {
        Objects.requireNonNull(board, "board must not be null");
        if (board.length != Board.ROWS) {
            throw new IllegalArgumentException("board must contain " + Board.ROWS + " rows");
        }
        return copyRows(board, Board.ROWS, Board.ROWS, "board");
    }

    private static boolean[][] copyAttackCells(boolean[][] cells) {
        Objects.requireNonNull(cells, "attackCells must not be null");
        if (cells.length != Board.ROWS) {
            throw new IllegalArgumentException("attackCells must contain " + Board.ROWS + " rows");
        }
        boolean[][] copy = new boolean[Board.ROWS][];
        for (int row = 0; row < Board.ROWS; row++) {
            if (cells[row] == null || cells[row].length != Board.COLS) {
                throw new IllegalArgumentException("attackCells row must contain " + Board.COLS + " cells");
            }
            copy[row] = cells[row].clone();
        }
        return copy;
    }
}
