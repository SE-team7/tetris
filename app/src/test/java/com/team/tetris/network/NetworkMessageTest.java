package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** NetworkMessage·MessageType: 종류별 데이터 검사, 순서 번호, 이벤트 구분 */
class NetworkMessageTest {

    @Test
    void of_startsWithoutSequence() {
        NetworkMessage message = NetworkMessage.of(MessageType.READY, 3, new MessagePayload.Ready(true));

        assertEquals(MessageType.READY, message.getType());
        assertEquals(0, message.getSequence());
        assertEquals(3, message.getBattleId());
        assertEquals(new MessagePayload.Ready(true), message.getPayload());
    }

    @Test
    void withSequence_returnsNumberedCopy() {
        NetworkMessage message = NetworkMessage.of(MessageType.LEAVE, 1);

        NetworkMessage numbered = message.withSequence(7);

        assertEquals(7, numbered.getSequence());
        assertEquals(0, message.getSequence());
        assertEquals(message.getType(), numbered.getType());
    }

    @Test
    void of_rejectsMissingPayload() {
        assertThrows(IllegalArgumentException.class, () -> NetworkMessage.of(MessageType.ATTACK, 1));
        assertThrows(IllegalArgumentException.class, () -> NetworkMessage.of(MessageType.CONFIG, 1));
    }

    @Test
    void of_rejectsWrongPayloadType() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.of(MessageType.READY, 1, new MessagePayload.RematchResponse(true)));
    }

    @Test
    void of_rejectsPayloadOnMessagesWithoutData() {
        for (MessageType type : new MessageType[] {
                MessageType.PAUSE, MessageType.RESUME, MessageType.REMATCH_REQUEST, MessageType.LEAVE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> NetworkMessage.of(type, 1, new MessagePayload.Ready(true)), type.name());
            assertNull(NetworkMessage.of(type, 1).getPayload());
        }
    }

    @Test
    void of_rejectsNullTypeAndNegativeIds() {
        assertThrows(NullPointerException.class, () -> NetworkMessage.of(null, 1));
        assertThrows(IllegalArgumentException.class, () -> NetworkMessage.of(MessageType.LEAVE, -1));
        assertThrows(IllegalArgumentException.class, () -> NetworkMessage.of(MessageType.LEAVE, 1).withSequence(-1));
    }

    @Test
    void everyTypeHasPayloadRule() {
        for (MessageType type : MessageType.values()) {
            // switch가 모든 종류를 다루는지 (새 종류 추가 시 누락 방지)
            assertDoesNotThrow(() -> NetworkMessage.payloadTypeOf(type), type.name());
        }
    }

    @Test
    void getPayload_castsToRequestedType() {
        NetworkMessage message = NetworkMessage.of(MessageType.START, 1, new MessagePayload.Start(100));

        assertEquals(100, message.getPayload(MessagePayload.Start.class).startDelayMillis());
        assertThrows(IllegalStateException.class, () -> message.getPayload(MessagePayload.Ready.class));
    }

    @Test
    void equalsAndHashCode_compareAllFields() {
        NetworkMessage a = NetworkTestData.attack(1, 2);
        NetworkMessage same = NetworkTestData.attack(1, 2);

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertNotEquals(a, NetworkTestData.attack(2, 2));
        assertNotEquals(a, NetworkTestData.attack(1, 3));
        assertNotEquals(a, a.withSequence(1));
        assertNotEquals(a, "ATTACK");
    }

    @Test
    void toString_containsTypeAndIds() {
        String text = NetworkMessage.of(MessageType.PAUSE, 5).withSequence(9).toString();

        assertTrue(text.contains("PAUSE"));
        assertTrue(text.contains("seq=9"));
        assertTrue(text.contains("battle=5"));
    }

    @Test
    void messageType_eventsAreAllButSnapshotAndHeartbeat() {
        for (MessageType type : MessageType.values()) {
            boolean expected = type != MessageType.SNAPSHOT && type != MessageType.PING && type != MessageType.PONG;
            assertEquals(expected, type.isEvent(), type.name());
        }
    }

    @Test
    void messageType_onlyPingAndPongAreControl() {
        for (MessageType type : MessageType.values()) {
            assertEquals(type == MessageType.PING || type == MessageType.PONG, type.isControl(), type.name());
        }
    }

    @Test
    void messageType_containsAllSpecifiedTypesInOrder() {
        String[] specified = {"CONFIG", "READY", "START", "ATTACK", "SNAPSHOT", "GAME_OVER", "RESULT",
                "PAUSE", "RESUME", "REMATCH_REQUEST", "REMATCH_RESPONSE", "LEAVE"};
        for (int i = 0; i < specified.length; i++) {
            assertEquals(specified[i], MessageType.values()[i].name());
        }
    }
}
