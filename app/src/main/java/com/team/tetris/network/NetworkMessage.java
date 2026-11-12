package com.team.tetris.network;

import java.util.Objects;

/**
 * 두 PC 사이에 주고받는 메시지 한 개.
 * sequence는 NetworkService가 실제로 보낼 때 연결마다 1부터 붙인다. (보내기 전에는 0)
 * battleId는 재대전 전 대전의 늦게 도착한 메시지를 걸러내는 데 쓴다.
 */
public final class NetworkMessage {

    private final MessageType type;
    private final long sequence;
    private final long battleId;
    private final MessagePayload payload;

    private NetworkMessage(MessageType type, long sequence, long battleId, MessagePayload payload) {
        Objects.requireNonNull(type, "type must not be null");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        if (battleId < 0) {
            throw new IllegalArgumentException("battleId must not be negative");
        }
        Class<? extends MessagePayload> expected = payloadTypeOf(type);
        if (expected == null ? payload != null : !expected.isInstance(payload)) {
            throw new IllegalArgumentException(type + " requires payload "
                    + (expected == null ? "none" : expected.getSimpleName()));
        }
        this.type = type;
        this.sequence = sequence;
        this.battleId = battleId;
        this.payload = payload;
    }

    /** 보낼 메시지 생성. payload 종류가 type과 맞지 않으면 예외 */
    public static NetworkMessage of(MessageType type, long battleId, MessagePayload payload) {
        return new NetworkMessage(type, 0, battleId, payload);
    }

    /** 데이터가 없는 메시지(PAUSE·RESUME·REMATCH_REQUEST·LEAVE) 생성 */
    public static NetworkMessage of(MessageType type, long battleId) {
        return of(type, battleId, null);
    }

    /** 수신·전송 시 순서 번호를 붙인 복사본 */
    NetworkMessage withSequence(long newSequence) {
        return new NetworkMessage(type, newSequence, battleId, payload);
    }

    // decode 전용: 순서 번호까지 그대로 복원
    static NetworkMessage restore(MessageType type, long sequence, long battleId, MessagePayload payload) {
        return new NetworkMessage(type, sequence, battleId, payload);
    }

    /** 메시지 종류별로 실어야 하는 데이터 종류. 데이터가 없으면 null */
    static Class<? extends MessagePayload> payloadTypeOf(MessageType type) {
        return switch (type) {
            case CONFIG -> MessagePayload.Config.class;
            case READY -> MessagePayload.Ready.class;
            case START -> MessagePayload.Start.class;
            case ATTACK -> MessagePayload.Attack.class;
            case SNAPSHOT -> MessagePayload.Snapshot.class;
            case GAME_OVER -> MessagePayload.GameOver.class;
            case RESULT -> MessagePayload.Result.class;
            case REMATCH_RESPONSE -> MessagePayload.RematchResponse.class;
            case PING, PONG -> MessagePayload.Ping.class;
            case PAUSE, RESUME, REMATCH_REQUEST, LEAVE -> null;
        };
    }

    public MessageType getType() {
        return type;
    }

    public long getSequence() {
        return sequence;
    }

    public long getBattleId() {
        return battleId;
    }

    /** 실린 데이터. 데이터가 없는 메시지는 null */
    public MessagePayload getPayload() {
        return payload;
    }

    /** 실린 데이터를 원하는 종류로 꺼낸다. 종류가 다르면 예외 */
    public <T extends MessagePayload> T getPayload(Class<T> payloadType) {
        if (!payloadType.isInstance(payload)) {
            throw new IllegalStateException(type + " does not carry " + payloadType.getSimpleName());
        }
        return payloadType.cast(payload);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NetworkMessage other
                && type == other.type
                && sequence == other.sequence
                && battleId == other.battleId
                && Objects.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sequence, battleId, payload);
    }

    @Override
    public String toString() {
        return "NetworkMessage[" + type + ", seq=" + sequence + ", battle=" + battleId
                + (payload == null ? "" : ", " + payload) + "]";
    }
}
