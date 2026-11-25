package com.team.tetris.service;

import java.util.List;

import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.Settings.KeyAction;

/**
 * BattleController가 틱마다 호출하는 자동 플레이 입력원.
 * 봇은 자신의 플레이어 스냅샷만 보고 그 틱에 실행할 액션 목록을 돌려준다.
 * BattleService는 건드리지 않으며 반환된 액션은 사람의 키 입력과 동일한 경로로 처리된다.
 */
public interface PlayerAgent {

    /** 이번 틱에 수행할 액션들. 없으면 빈 리스트 */
    List<KeyAction> decide(GameSnapshot snapshot);

    /** 새 대전 시작 시 호출 */
    default void reset() {
    }
}
