package com.team.tetris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// [대전 모드] 대전 결과(승리·무승부·연결 끊김) 구분 테스트
class BattleResultTest {

    @Test
    void winHasWinnerAndIsNotDraw() {
        BattleResult result = BattleResult.win(PlayerId.PLAYER2, BattleEndReason.GAME_OVER);

        assertEquals(PlayerId.PLAYER2, result.getWinner());
        assertFalse(result.isDraw());
        assertEquals(BattleEndReason.GAME_OVER, result.getReason());
    }

    @Test
    void drawHasNoWinner() {
        BattleResult result = BattleResult.draw(BattleEndReason.TIME_UP);

        assertNull(result.getWinner());
        assertTrue(result.isDraw());
    }

    @Test
    void connectionLostIsNeitherWinNorDraw() {
        BattleResult result = BattleResult.connectionLost();

        assertNull(result.getWinner());
        assertFalse(result.isDraw());
        assertEquals(BattleEndReason.CONNECTION_LOST, result.getReason());
    }

    @Test
    void invalidCombinationsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> BattleResult.win(null, BattleEndReason.GAME_OVER));
        assertThrows(IllegalArgumentException.class,
                () -> BattleResult.win(PlayerId.PLAYER1, BattleEndReason.CONNECTION_LOST));
        assertThrows(IllegalArgumentException.class,
                () -> BattleResult.draw(BattleEndReason.PLAYER_LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> BattleResult.draw(BattleEndReason.CONNECTION_LOST));
    }

    @Test
    void sameResultsAreEqual() {
        assertEquals(BattleResult.win(PlayerId.PLAYER1, BattleEndReason.TIME_UP),
                BattleResult.win(PlayerId.PLAYER1, BattleEndReason.TIME_UP));
        assertEquals(BattleResult.connectionLost().hashCode(),
                BattleResult.connectionLost().hashCode());
        assertNotEquals(BattleResult.win(PlayerId.PLAYER1, BattleEndReason.TIME_UP),
                BattleResult.win(PlayerId.PLAYER2, BattleEndReason.TIME_UP));
        assertNotEquals(BattleResult.draw(BattleEndReason.GAME_OVER), null);
    }

    @Test
    void opponentOfPlayerIsTheOtherPlayer() {
        assertEquals(PlayerId.PLAYER2, PlayerId.PLAYER1.opponent());
        assertEquals(PlayerId.PLAYER1, PlayerId.PLAYER2.opponent());
    }
}
