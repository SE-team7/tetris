package com.team.tetris.domain;

/**
 * 받은 공격 대기열 (최대 10줄).
 *
 * TODO(1번): 임시 파일. 1번 구현으로 교체.
 * 메서드 이름·모양만 명세에 맞춘 빈 껍데기이며 실제로 저장하지 않는다.
 * (실제 구현은 여러 엔진·통신 스레드에서 동시에 불리므로 동기화 필요)
 */
public class AttackQueue {

    // 기존 대기 행 아래에 새 행 추가 (최대 10줄, 넘치면 아래쪽 제거, 이미 10줄이면 무시)
    public void add(AttackRows attackRows) {
        // TODO(1번)
    }

    // 대기열 변경 없이 표시용 복사본 반환
    public AttackRows snapshot() {
        // TODO(1번)
        return AttackRows.empty();
    }

    // 현재 대기 행 전체 반환 후 대기열 비우기
    public AttackRows drain() {
        // TODO(1번)
        return AttackRows.empty();
    }

    public int getRowCount() {
        // TODO(1번)
        return 0;
    }

    public boolean isEmpty() {
        // TODO(1번)
        return true;
    }

    public void clear() {
        // TODO(1번)
    }
}
