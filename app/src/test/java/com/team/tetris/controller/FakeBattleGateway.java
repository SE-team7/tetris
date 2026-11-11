package com.team.tetris.controller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.team.tetris.network.BattleGateway;
import com.team.tetris.network.MessagePayload;

/** BattleService 대신 호출 내용을 기록하는 테스트용 대전 (여러 스레드에서 호출됨) */
class FakeBattleGateway implements BattleGateway {

    final List<String> calls = new CopyOnWriteArrayList<>();
    final List<MessagePayload.Attack> receivedAttacks = new CopyOnWriteArrayList<>();
    final List<MessagePayload.Snapshot> remoteSnapshots = new CopyOnWriteArrayList<>();
    final List<MessagePayload.Result> appliedResults = new CopyOnWriteArrayList<>();

    volatile MessagePayload.Config startedConfig;
    volatile Boolean startedAsHost;
    volatile MessagePayload.Snapshot localSnapshot;
    volatile MessagePayload.Result result;
    volatile Consumer<MessagePayload.Attack> attackListener;
    volatile Runnable gameOverListener;

    /** 로컬 엔진이 공격을 만든 상황 */
    void engineCreatesAttack(MessagePayload.Attack attack) {
        attackListener.accept(attack);
    }

    /** 로컬 엔진이 게임오버된 상황 */
    void engineGameOver() {
        gameOverListener.run();
    }

    int count(String call) {
        return (int) calls.stream().filter(call::equals).count();
    }

    @Override
    public void startNetworkBattle(MessagePayload.Config config, boolean localIsHost) {
        startedConfig = config;
        startedAsHost = localIsHost;
        calls.add("start");
    }

    @Override
    public void receiveRemoteAttack(MessagePayload.Attack attack) {
        receivedAttacks.add(attack);
        calls.add("receiveRemoteAttack");
    }

    @Override
    public void updateRemoteSnapshot(MessagePayload.Snapshot snapshot) {
        remoteSnapshots.add(snapshot);
    }

    @Override
    public MessagePayload.Snapshot getLocalSnapshot() {
        return localSnapshot;
    }

    @Override
    public void notifyRemoteGameOver() {
        calls.add("notifyRemoteGameOver");
    }

    @Override
    public void applyRemoteResult(MessagePayload.Result applied) {
        appliedResults.add(applied);
        calls.add("applyRemoteResult");
    }

    /** notifyRemoteLeft 후 BattleService가 확정할 결과 */
    volatile MessagePayload.Result resultAfterRemoteLeft;

    @Override
    public void notifyRemoteLeft() {
        calls.add("notifyRemoteLeft");
        if (resultAfterRemoteLeft != null) {
            result = resultAfterRemoteLeft;
        }
    }

    @Override
    public void notifyConnectionLost() {
        calls.add("notifyConnectionLost");
    }

    @Override
    public void pause() {
        calls.add("pause");
    }

    @Override
    public void resume() {
        calls.add("resume");
    }

    @Override
    public void stop() {
        calls.add("stop");
    }

    @Override
    public MessagePayload.Result getResult() {
        return result;
    }

    @Override
    public void setOutgoingAttackListener(Consumer<MessagePayload.Attack> listener) {
        attackListener = listener;
    }

    @Override
    public void setLocalGameOverListener(Runnable listener) {
        gameOverListener = listener;
    }
}
