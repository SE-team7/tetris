package com.team.tetris.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.GameSnapshot;

// [대전 모드] 엔진이 만든 스냅샷이 현재 상태와 일치하고 원본과 분리되는지 검증
class GameEngineSnapshotTest {

    @Test
    void snapshotMatchesEngineState() {
        GameEngineService engine = new GameEngineService();
        engine.startGame(Difficulty.NORMAL, false);
        engine.tick();

        GameSnapshot snap = engine.getSnapshot();

        assertEquals(engine.getScore(), snap.getScore());
        assertEquals(engine.getLevel(), snap.getLevel());
        assertEquals(engine.getSkipsRemaining(), snap.getRemainingSkips());
        assertEquals(engine.isGameOver(), snap.isGameOver());
        assertEquals(engine.getCurrentBlock().getType(), snap.getCurrentBlock().getType());
        assertEquals(engine.getNextBlock().getType(), snap.getNextBlock().getType());
        assertTrue(snap.getPendingAttack().isEmpty());
    }

    @Test
    void snapshotDoesNotChangeWhenEngineMoves() {
        GameEngineService engine = new GameEngineService();
        engine.startGame(Difficulty.EASY, false);
        engine.tick();

        GameSnapshot snap = engine.getSnapshot();
        int rowBefore = snap.getCurrentBlock().getPosition().getRow();
        engine.moveDown();

        assertEquals(rowBefore, snap.getCurrentBlock().getPosition().getRow());
        assertNotSame(engine.getCurrentBlock(), snap.getCurrentBlock());
    }

    @Test
    void attackCellsHaveBoardSize() {
        GameEngineService engine = new GameEngineService();
        engine.startGame(Difficulty.EASY, false);

        GameSnapshot snap = engine.getSnapshot();

        assertNotNull(snap.getAttackCells());
        assertEquals(snap.getBoard().length, snap.getAttackCells().length);
        assertFalse(snap.getAttackCells()[0][0]);
    }
}
