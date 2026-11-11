package com.team.tetris.network;

import java.util.function.Consumer;

/**
 * NetworkController가 사용하는 대전 기능 (2번 BattleService 연결 지점).
 * 대전 도메인 타입 대신 전송용 데이터(MessagePayload)로 주고받는다.
 * BattleService와의 타입 변환은 어댑터에서 한 번만 처리한다.
 *
 * 명세의 4번 → 2번 호출 목록과 1:1 대응:
 *   startNetworkBattle · receiveRemoteAttack · updateRemoteSnapshot · getPlayerSnapshot(localPlayer)
 *   notifyRemoteGameOver · applyRemoteResult · notifyRemoteLeft · notifyConnectionLost
 *   pause · resume · stop · getResult · setOutgoingAttackListener · setLocalGameOverListener
 */
public interface BattleGateway {

    /** 양쪽 준비 완료 후 대전 시작. 서버가 PLAYER1, 클라이언트가 PLAYER2 */
    void startNetworkBattle(MessagePayload.Config config, boolean localIsHost);

    /** 상대에게서 받은 공격을 로컬 엔진 대기열에 추가 */
    void receiveRemoteAttack(MessagePayload.Attack attack);

    /** 상대 화면 표시용 상태 갱신 */
    void updateRemoteSnapshot(MessagePayload.Snapshot snapshot);

    /** 상대에게 보낼 로컬 플레이어 상태. 대전 중이 아니면 null 가능 */
    MessagePayload.Snapshot getLocalSnapshot();

    /** 상대 게임오버 통지 */
    void notifyRemoteGameOver();

    /** 판정 담당(서버)이 확정한 결과 적용 */
    void applyRemoteResult(MessagePayload.Result result);

    /** 상대 나가기 → 로컬 승리 */
    void notifyRemoteLeft();

    /** 연결 끊김 → 승자 없이 종료 */
    void notifyConnectionLost();

    void pause();

    void resume();

    /** 대전 스레드·시간 관리 정리 */
    void stop();

    /** 확정된 결과. 종료 전이면 null */
    MessagePayload.Result getResult();

    /** 로컬 엔진이 만든 공격을 받을 콜백 등록 (null이면 해제) */
    void setOutgoingAttackListener(Consumer<MessagePayload.Attack> listener);

    /** 로컬 게임오버 콜백 등록 (null이면 해제) */
    void setLocalGameOverListener(Runnable listener);
}
