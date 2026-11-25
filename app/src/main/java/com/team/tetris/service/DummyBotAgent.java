package com.team.tetris.service;

import java.util.List;

import com.team.tetris.domain.GameSnapshot;
import com.team.tetris.domain.Settings.KeyAction;

/**
 * 플러그 가능 구조를 검증하기 위한 자리표시용 봇.
 * 아무 액션도 내지 않아 블록이 중력으로만 떨어진다. 진짜 전략은 별도 작업으로 교체.
 */
public class DummyBotAgent implements PlayerAgent {

    @Override
    public List<KeyAction> decide(GameSnapshot snapshot) {
        return List.of();
    }
}
