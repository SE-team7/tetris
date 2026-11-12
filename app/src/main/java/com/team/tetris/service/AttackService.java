package com.team.tetris.service;

import java.util.function.Consumer;

import com.team.tetris.domain.AttackQueue;
import com.team.tetris.domain.AttackRows;
import com.team.tetris.domain.Board;

/**
 * 대전 모드 공격 보내기/적용 서비스.
 *
 * TODO(1번): 임시 파일. 1번 구현으로 교체.
 * 엔진(2번)은 sendAttack(), applyAttack() 두 개만 사용한다.
 * 지금은 1인 모드 동작이 바뀌지 않도록 아무 일도 하지 않는다.
 */
public class AttackService {

    private final Board board;
    private final AttackQueue incomingAttackQueue;
    private final Consumer<AttackRows> attackSender;

    // board: 자신의 보드 / incomingAttackQueue: 받은 공격 보관 / attackSender: 상대에게 전달하는 동작
    public AttackService(Board board,
                         AttackQueue incomingAttackQueue,
                         Consumer<AttackRows> attackSender) {
        this.board = board;
        this.incomingAttackQueue = incomingAttackQueue;
        this.attackSender = attackSender;
    }

    // 생성된 공격 행을 상대에게 전달. 빈 공격이면 아무 작업도 하지 않음
    public void sendAttack(AttackRows attackRows) {
        // TODO(1번)
    }

    // 받은 공격 중 아직 보드에 들어가지 않은 것 (화면 표시용, 대기열 변경 없음)
    public AttackRows getPendingAttack() {
        // TODO(1번): incomingAttackQueue.snapshot()
        return AttackRows.empty();
    }

    // 받은 공격 전체를 보드에 적용. 위로 넘친 셀이 있으면 true, 없거나 대기 공격이 없으면 false
    public boolean applyAttack() {
        // TODO(1번)
        return false;
    }
}
