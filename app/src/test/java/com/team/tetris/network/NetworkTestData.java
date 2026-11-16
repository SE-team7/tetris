package com.team.tetris.network;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ItemType;

/** network 테스트에서 함께 쓰는 메시지 데이터 */
final class NetworkTestData {

    private NetworkTestData() {
    }

    /** col 칸만 비어 있는 공격 행 count줄 */
    static BlockType[][] attackRows(int count, int emptyCol) {
        BlockType[][] rows = new BlockType[count][Board.COLS];
        for (int row = 0; row < count; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                rows[row][col] = col == emptyCol ? null : BlockType.values()[(row + col) % 7];
            }
        }
        return rows;
    }

    /** 아래 3줄이 차 있고 맨 아래 줄은 공격 블록인 보드 상태 */
    static MessagePayload.Snapshot snapshot(int score) {
        BlockType[][] board = new BlockType[Board.ROWS][Board.COLS];
        boolean[][] attackCells = new boolean[Board.ROWS][Board.COLS];
        for (int row = Board.ROWS - 3; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS - 1; col++) {
                board[row][col] = BlockType.T;
                attackCells[row][col] = row == Board.ROWS - 1;
            }
        }
        return new MessagePayload.Snapshot(
                board,
                attackCells,
                new MessagePayload.BlockState(BlockType.L, 5, 4, 2, ItemType.NONE, -1),
                new MessagePayload.BlockState(BlockType.I, 1, 4, 0, ItemType.LINE_CLEAR, 2),
                score,
                3,
                6,
                attackRows(2, 0),
                false);
    }

    static NetworkMessage attack(long battleId, int rows) {
        return NetworkMessage.of(MessageType.ATTACK, battleId, new MessagePayload.Attack(attackRows(rows, 1)));
    }

    static NetworkMessage snapshotMessage(long battleId, int score) {
        return NetworkMessage.of(MessageType.SNAPSHOT, battleId, snapshot(score));
    }
}
