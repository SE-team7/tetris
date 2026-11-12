package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Block;
import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.ClearResult;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.Position;

/**
 * [대전 모드] 엔진의 공격 생성·전송·적용 순서 테스트.
 * 블럭 고정 → 삭제 전 공격 생성 → 행 삭제 → 공격 전송 → 받은 공격 적용 → 다음 블럭 생성
 */
class GameEngineAttackTest {

    // 항상 O 블럭만 주는 팩토리
    private static class OBlockFactory extends BlockFactory {
        @Override
        public Block createRandomBlock(Difficulty difficulty) {
            return createBlock(BlockType.O);
        }
    }

    // createAttackRows 호출 시점을 기록하는 보드
    private static class RecordingBoard extends Board {
        final AttackRows marker = new AttackRows(new BlockType[][] {new BlockType[COLS]});
        int createCount = 0;
        boolean hadFullLinesOnCreate = false;
        Position[] lastLockedCells;

        @Override
        public AttackRows createAttackRows(ClearResult result, Position[] lastLockedCells) {
            createCount++;
            hadFullLinesOnCreate = !findFullLines().isEmpty();
            this.lastLockedCells = lastLockedCells;
            return marker;
        }
    }

    // sendAttack/applyAttack 호출을 기록하는 서비스
    private static class RecordingAttackService extends AttackService {
        private final Board board;
        final List<AttackRows> sent = new ArrayList<>();
        boolean hadFullLinesOnSend = false;
        int applyCount = 0;
        boolean applyResult = false;
        boolean hadFullLinesOnApply = false;

        RecordingAttackService(Board board) {
            super(board, new AttackQueue(), attack -> { });
            this.board = board;
        }

        @Override
        public void sendAttack(AttackRows attackRows) {
            sent.add(attackRows);
            hadFullLinesOnSend = !board.findFullLines().isEmpty();
        }

        @Override
        public boolean applyAttack() {
            applyCount++;
            hadFullLinesOnApply = !board.findFullLines().isEmpty();
            return applyResult;
        }
    }

    private RecordingBoard board;
    private RecordingAttackService attackService;
    private GameEngineService engine;

    @BeforeEach
    void setUp() {
        board = new RecordingBoard();
        attackService = new RecordingAttackService(board);
        engine = new GameEngineService(
                board,
                new OBlockFactory(),
                new ScoreService(),
                new LevelService(),
                attackService
        );
        engine.startGame(Difficulty.EASY, false);
    }

    // 지정한 줄들을 현재 블럭이 떨어질 열만 비우고 채운다
    private void fillRowsExceptCurrentBlockColumns(int... rows) {
        Block current = engine.getCurrentBlock();
        assertNotNull(current);
        int[] blockCols = Arrays.stream(current.getCells())
                .mapToInt(Position::getCol).distinct().sorted().toArray();

        for (int row : rows) {
            for (int col = 0; col < Board.COLS; col++) {
                if (Arrays.binarySearch(blockCols, col) < 0) {
                    board.lockBlock(new Block(BlockType.BOMB, new Position(row, col)));
                }
            }
        }
    }

    // 첫 블럭 생성 → 바닥 2줄 채움 → 하드드랍 → 고정 처리
    private Position[] dropBlockOnTwoAlmostFullLines() {
        engine.tick();
        fillRowsExceptCurrentBlockColumns(Board.ROWS - 2, Board.ROWS - 1);
        engine.hardDrop();
        Position[] lockedCells = engine.getCurrentBlock().getCells();
        engine.tick();
        return lockedCells;
    }

    // Position은 equals가 없어서 [행, 열] 값으로 비교
    private int[][] toRowCol(Position[] cells) {
        return Arrays.stream(cells)
                .map(c -> new int[] {c.getRow(), c.getCol()})
                .toArray(int[][]::new);
    }

    @Test
    void attackIsCreatedBeforeLinesAreCleared() {
        Position[] lockedCells = dropBlockOnTwoAlmostFullLines();

        assertEquals(1, board.createCount);
        assertTrue(board.hadFullLinesOnCreate, "행 삭제 전에 공격을 생성해야 함");
        assertArrayEquals(toRowCol(lockedCells), toRowCol(board.lastLockedCells),
                "마지막 고정 블럭 칸을 넘겨야 함");
        assertTrue(attackService.sent.isEmpty(), "행 삭제 전에는 전송하지 않음");
    }

    @Test
    void attackIsSentAfterLinesAreCleared() {
        dropBlockOnTwoAlmostFullLines();

        engine.tick(); // 미뤄둔 줄 삭제

        assertEquals(1, attackService.sent.size());
        assertSame(board.marker, attackService.sent.get(0));
        assertFalse(attackService.hadFullLinesOnSend, "행 삭제 후에 전송해야 함");
    }

    @Test
    void attackIsSentWhenClearIsAppliedByAnimationCallback() {
        dropBlockOnTwoAlmostFullLines();

        assertTrue(engine.applyPendingLineClear());

        assertEquals(1, attackService.sent.size());
        assertFalse(attackService.hadFullLinesOnSend);
    }

    @Test
    void receivedAttackIsAppliedAfterClearAndBeforeNextBlock() {
        engine.tick();
        assertEquals(1, attackService.applyCount, "첫 블럭 생성 전에 한 번 적용");

        fillRowsExceptCurrentBlockColumns(Board.ROWS - 2, Board.ROWS - 1);
        engine.hardDrop();
        engine.tick(); // 고정 처리
        engine.tick(); // 미뤄둔 줄 삭제
        assertEquals(1, attackService.applyCount, "줄 삭제 틱에는 적용하지 않음");

        engine.tick(); // 다음 블럭 생성
        assertEquals(2, attackService.applyCount);
        assertFalse(attackService.hadFullLinesOnApply, "공격은 줄 삭제가 끝난 뒤 적용");
        assertNotNull(engine.getCurrentBlock());
    }

    @Test
    void overflowWhileApplyingAttackEndsGame() {
        attackService.applyResult = true;

        engine.tick();

        assertTrue(engine.isGameOver());
        assertNull(engine.getCurrentBlock(), "게임오버 시 다음 블럭을 꺼내지 않음");
    }

    @Test
    void singlePlayerEngineKeepsWorkingWithDefaultAttackService() {
        GameEngineService single = new GameEngineService();
        single.startGame(Difficulty.EASY, false);
        single.tick();

        assertFalse(single.isGameOver());
        assertNotNull(single.getCurrentBlock());
    }
}
