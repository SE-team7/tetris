package com.team.tetris.network;

import java.util.Objects;
import java.util.function.Consumer;

import com.team.tetris.domain.PlayerId;
import com.team.tetris.service.BattleService;

/**
 * BattleGateway → 2번 BattleService 연결 어댑터.
 * NetworkController가 주고받는 전송용 데이터를 BattleService 도메인 객체로 변환해 그대로 넘긴다.
 * 서버는 PLAYER1(판정 담당), 클라이언트는 PLAYER2.
 */
public class BattleServiceGateway implements BattleGateway {

    private final BattleService battle;
    private volatile PlayerId localPlayer = PlayerId.PLAYER1;

    public BattleServiceGateway(BattleService battle) {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
    }

    public BattleService getBattleService() {
        return battle;
    }

    /** 이 PC의 플레이어 (마지막으로 시작한 대전 기준) */
    public PlayerId getLocalPlayer() {
        return localPlayer;
    }

    @Override
    public void startNetworkBattle(MessagePayload.Config config, boolean localIsHost) {
        PlayerId player = localIsHost ? PlayerId.PLAYER1 : PlayerId.PLAYER2;
        localPlayer = player;   // 시작 직후 엔진 스레드가 상태를 조회할 수 있으므로 먼저 설정
        battle.startNetworkBattle(PayloadMapper.toBattleConfig(config), player);
    }

    @Override
    public void receiveRemoteAttack(MessagePayload.Attack attack) {
        battle.receiveRemoteAttack(PayloadMapper.toAttackRows(attack));
    }

    @Override
    public void updateRemoteSnapshot(MessagePayload.Snapshot snapshot) {
        battle.updateRemoteSnapshot(PayloadMapper.toGameSnapshot(snapshot));
    }

    @Override
    public MessagePayload.Snapshot getLocalSnapshot() {
        return PayloadMapper.toPayload(battle.getPlayerSnapshot(localPlayer));
    }

    @Override
    public void notifyRemoteGameOver() {
        battle.notifyRemoteGameOver();
    }

    @Override
    public void applyRemoteResult(MessagePayload.Result result) {
        battle.applyRemoteResult(PayloadMapper.toBattleResult(result));
    }

    @Override
    public void notifyRemoteLeft() {
        battle.notifyRemoteLeft();
    }

    @Override
    public void notifyConnectionLost() {
        battle.notifyConnectionLost();
    }

    @Override
    public void pause() {
        battle.pause();
    }

    @Override
    public void resume() {
        battle.resume();
    }

    @Override
    public void stop() {
        battle.stop();
    }

    @Override
    public MessagePayload.Result getResult() {
        return PayloadMapper.toPayload(battle.getResult());
    }

    @Override
    public void setOutgoingAttackListener(Consumer<MessagePayload.Attack> listener) {
        if (listener == null) {
            battle.setOutgoingAttackListener(null);
            return;
        }
        battle.setOutgoingAttackListener(rows -> {
            MessagePayload.Attack attack = PayloadMapper.toPayload(rows);
            if (attack != null) {
                listener.accept(attack);
            }
        });
    }

    @Override
    public void setLocalGameOverListener(Runnable listener) {
        battle.setLocalGameOverListener(listener);
    }
}
