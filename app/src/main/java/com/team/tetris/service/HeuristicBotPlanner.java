package com.team.tetris.service;

import java.util.ArrayList;
import java.util.List;

import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Position;
import com.team.tetris.domain.Settings.KeyAction;

/**
 * BCTS 8 특징 휴리스틱 플래너 + 엔진 정합 시뮬.
 * 엔진의 회전(wall kick, [0,-1,+1,-2,+2] 순차 시도)과 이동·하드드롭을 그대로 복제해,
 * 봇의 액션이 실행됐을 때 실제로 어디에 착지하는지 정확히 예측한다.
 * 2수 lookahead: current 배치 → next 배치까지 평가해 최종 점수가 가장 높은 현재 (rotation, delta)를 선택.
 */
public class HeuristicBotPlanner {

    private static final double W_LANDING_HEIGHT = -12.63;
    private static final double W_ERODED_CELLS = 6.60;
    private static final double W_ROW_TRANSITIONS = -9.22;
    private static final double W_COL_TRANSITIONS = -19.77;
    private static final double W_HOLES = -13.08;
    private static final double W_WELLS = -10.49;
    private static final double W_HOLE_DEPTH = -1.61;
    private static final double W_ROWS_WITH_HOLES = -24.04;

    private static final int ROWS = Board.ROWS;
    private static final int COLS = Board.COLS;

    private static final int[] KICK_DELTAS = {0, -1, 1, -2, 2};

    public List<KeyAction> plan(BlockType[][] board, Block current, Block next) {
        if (current == null) return List.of();
        BlockType[][] workBoard = copy(board);

        Placement best = null;
        int rotations = current.canRotate() ? 4 : 1;

        for (int r = 0; r < rotations; r++) {
            Block afterRot = applyRotationsWithKick(current.copy(), workBoard, r);
            if (afterRot == null) continue;

            int startLeftmost = minCol(afterRot.getCells());
            for (int targetLeftmost = -3; targetLeftmost < COLS + 3; targetLeftmost++) {
                int delta = targetLeftmost - startLeftmost;
                Block moved = tryHorizontalMoves(afterRot.copy(), workBoard, delta);
                if (moved == null) continue;

                Placement placed = simulateDrop(workBoard, moved);
                if (placed == null) continue;

                double score = evaluate(placed);
                if (next != null) {
                    score += bestNextScore(placed.boardAfter, next);
                }

                if (best == null || score > best.score) {
                    best = placed;
                    best.score = score;
                    best.rotations = r;
                    best.horizontalDelta = delta;
                }
            }
        }

        if (best == null) return List.of(KeyAction.HARD_DROP);
        return buildActions(best.rotations, best.horizontalDelta);
    }

    // 2수: current 배치 후 보드에서 next의 모든 (회전×열)을 돌려 최고 점수를 돌려준다.
    // 유효 placement가 없으면 0 반환 (현재 점수에 영향 없음).
    private double bestNextScore(BlockType[][] boardAfter, Block next) {
        double best = Double.NEGATIVE_INFINITY;
        int rotations = next.canRotate() ? 4 : 1;
        for (int r = 0; r < rotations; r++) {
            Block afterRot = applyRotationsWithKick(next.copy(), boardAfter, r);
            if (afterRot == null) continue;

            int startLeftmost = minCol(afterRot.getCells());
            for (int targetLeftmost = -3; targetLeftmost < COLS + 3; targetLeftmost++) {
                int delta = targetLeftmost - startLeftmost;
                Block moved = tryHorizontalMoves(afterRot.copy(), boardAfter, delta);
                if (moved == null) continue;

                Placement p = simulateDrop(boardAfter, moved);
                if (p == null) continue;

                double s = evaluate(p);
                if (s > best) best = s;
            }
        }
        return best == Double.NEGATIVE_INFINITY ? 0.0 : best;
    }

    // 엔진의 rotate(): [0,-1,+1,-2,+2] 중 첫 번째로 들어맞는 delta를 적용. 모두 막히면 null.
    private Block applyRotationsWithKick(Block block, BlockType[][] board, int times) {
        if (times == 0) return block;
        if (!block.canRotate()) return times == 0 ? block : null;
        for (int i = 0; i < times; i++) {
            Integer kick = findKickDelta(block, board);
            if (kick == null) return null;
            block.move(0, kick);
            block.rotate();
        }
        return block;
    }

    private Integer findKickDelta(Block block, BlockType[][] board) {
        for (int delta : KICK_DELTAS) {
            Position[] cells = block.getRotatedCells(0, delta);
            if (fits(board, cells)) return delta;
        }
        return null;
    }

    // 엔진의 moveLeft/Right: 한 칸씩 시도, 막히면 그 자리에서 멈춤. 봇은 "움직였어야 할 거리" 전부가
    // 가능해야만 그 placement를 유효로 간주한다 (하나라도 막히면 다른 col로 평가).
    private Block tryHorizontalMoves(Block block, BlockType[][] board, int delta) {
        int direction = delta >= 0 ? 1 : -1;
        for (int i = 0; i < Math.abs(delta); i++) {
            Position[] cells = block.getMovedCells(0, direction);
            if (!fits(board, cells)) return null;
            block.move(0, direction);
        }
        return block;
    }

    // 엔진의 hardDrop(): canMove(1, 0)이 가능한 만큼 내려 확정.
    private Placement simulateDrop(BlockType[][] board, Block positioned) {
        int drop = 0;
        while (true) {
            Position[] cells = positioned.getMovedCells(drop + 1, 0);
            if (!fits(board, cells)) break;
            drop++;
        }
        Position[] finalCells = positioned.getMovedCells(drop, 0);
        if (!fits(board, finalCells)) return null;

        BlockType[][] next = copy(board);
        int topRow = Integer.MAX_VALUE;
        int bottomRow = Integer.MIN_VALUE;
        for (Position cell : finalCells) {
            next[cell.getRow()][cell.getCol()] = positioned.getType();
            if (cell.getRow() < topRow) topRow = cell.getRow();
            if (cell.getRow() > bottomRow) bottomRow = cell.getRow();
        }

        int cleared = 0;
        int eroded = 0;
        for (int r = topRow; r <= bottomRow; r++) {
            if (isFull(next[r])) {
                cleared++;
                for (Position cell : finalCells) {
                    if (cell.getRow() == r) eroded++;
                }
            }
        }
        if (cleared > 0) clearLines(next);

        Placement p = new Placement();
        p.boardAfter = next;
        p.linesCleared = cleared;
        p.erodedPieceCells = cleared * eroded;
        p.landingHeight = (ROWS - 1) - (topRow + bottomRow) / 2.0;
        return p;
    }

    private List<KeyAction> buildActions(int rotations, int horizontalDelta) {
        List<KeyAction> actions = new ArrayList<>(rotations + Math.abs(horizontalDelta) + 1);
        for (int i = 0; i < rotations; i++) actions.add(KeyAction.ROTATE);
        KeyAction dir = horizontalDelta >= 0 ? KeyAction.MOVE_RIGHT : KeyAction.MOVE_LEFT;
        for (int i = 0; i < Math.abs(horizontalDelta); i++) actions.add(dir);
        actions.add(KeyAction.HARD_DROP);
        return actions;
    }

    // ---- 평가 (BCTS 8 특징) ----

    private double evaluate(Placement p) {
        BlockType[][] board = p.boardAfter;
        return W_LANDING_HEIGHT * p.landingHeight
                + W_ERODED_CELLS * p.erodedPieceCells
                + W_ROW_TRANSITIONS * rowTransitions(board)
                + W_COL_TRANSITIONS * columnTransitions(board)
                + W_HOLES * countHoles(board)
                + W_WELLS * wellSums(board)
                + W_HOLE_DEPTH * holeDepth(board)
                + W_ROWS_WITH_HOLES * rowsWithHoles(board);
    }

    private int rowTransitions(BlockType[][] board) {
        int transitions = 0;
        for (int r = 0; r < ROWS; r++) {
            boolean prev = true;
            for (int c = 0; c < COLS; c++) {
                boolean curr = board[r][c] != null;
                if (curr != prev) transitions++;
                prev = curr;
            }
            if (!prev) transitions++;
        }
        return transitions;
    }

    private int columnTransitions(BlockType[][] board) {
        int transitions = 0;
        for (int c = 0; c < COLS; c++) {
            boolean prev = false;
            for (int r = 0; r < ROWS; r++) {
                boolean curr = board[r][c] != null;
                if (curr != prev) transitions++;
                prev = curr;
            }
            if (!prev) transitions++;
        }
        return transitions;
    }

    private int countHoles(BlockType[][] board) {
        int holes = 0;
        for (int c = 0; c < COLS; c++) {
            boolean roofSeen = false;
            for (int r = 0; r < ROWS; r++) {
                if (board[r][c] != null) {
                    roofSeen = true;
                } else if (roofSeen) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private int wellSums(BlockType[][] board) {
        int sum = 0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (board[r][c] != null) continue;
                boolean leftFilled = (c == 0) || board[r][c - 1] != null;
                boolean rightFilled = (c == COLS - 1) || board[r][c + 1] != null;
                if (!(leftFilled && rightFilled)) continue;

                int depth = 0;
                for (int rr = r; rr < ROWS; rr++) {
                    if (board[rr][c] != null) break;
                    depth++;
                }
                sum += depth * (depth + 1) / 2;
                break;
            }
        }
        return sum;
    }

    private int holeDepth(BlockType[][] board) {
        int sum = 0;
        for (int c = 0; c < COLS; c++) {
            int filledAbove = 0;
            for (int r = 0; r < ROWS; r++) {
                if (board[r][c] != null) {
                    filledAbove++;
                } else if (filledAbove > 0) {
                    sum += filledAbove;
                }
            }
        }
        return sum;
    }

    private int rowsWithHoles(BlockType[][] board) {
        boolean[] hasHole = new boolean[ROWS];
        for (int c = 0; c < COLS; c++) {
            boolean roofSeen = false;
            for (int r = 0; r < ROWS; r++) {
                if (board[r][c] != null) {
                    roofSeen = true;
                } else if (roofSeen) {
                    hasHole[r] = true;
                }
            }
        }
        int count = 0;
        for (boolean h : hasHole) if (h) count++;
        return count;
    }

    private void clearLines(BlockType[][] board) {
        int write = ROWS - 1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (isFull(board[r])) continue;
            if (write != r) System.arraycopy(board[r], 0, board[write], 0, COLS);
            write--;
        }
        for (int r = write; r >= 0; r--) {
            for (int c = 0; c < COLS; c++) board[r][c] = null;
        }
    }

    private boolean isFull(BlockType[] row) {
        for (BlockType cell : row) if (cell == null) return false;
        return true;
    }

    private boolean fits(BlockType[][] board, Position[] cells) {
        for (Position cell : cells) {
            int r = cell.getRow();
            int c = cell.getCol();
            if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
            if (board[r][c] != null) return false;
        }
        return true;
    }

    private int minCol(Position[] cells) {
        int min = Integer.MAX_VALUE;
        for (Position cell : cells) if (cell.getCol() < min) min = cell.getCol();
        return min;
    }

    private BlockType[][] copy(BlockType[][] board) {
        BlockType[][] copy = new BlockType[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, COLS);
        }
        return copy;
    }

    private static class Placement {
        BlockType[][] boardAfter;
        int linesCleared;
        int erodedPieceCells;
        double landingHeight;
        double score;
        int rotations;
        int horizontalDelta;
    }
}
