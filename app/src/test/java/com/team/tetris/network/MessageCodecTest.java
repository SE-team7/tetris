package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;

/** MessageCodec: 모든 메시지 종류의 인코딩·디코딩과 잘못된 데이터 거부 */
class MessageCodecTest {

    private final MessageCodec codec = new MessageCodec();

    private NetworkMessage roundTrip(NetworkMessage message) {
        return codec.decode(codec.encode(message));
    }

    /** 메시지 종류마다 하나씩 */
    static List<NetworkMessage> oneOfEachType() {
        return List.of(
                NetworkMessage.of(MessageType.CONFIG, 1, new MessagePayload.Config("TIME_LIMIT", Difficulty.HARD, 300)),
                NetworkMessage.of(MessageType.READY, 1, new MessagePayload.Ready(true)),
                NetworkMessage.of(MessageType.START, 1, new MessagePayload.Start(1500)),
                NetworkTestData.attack(1, 3),
                NetworkTestData.snapshotMessage(1, 1234),
                NetworkMessage.of(MessageType.GAME_OVER, 1, new MessagePayload.GameOver(90_000, 777)),
                NetworkMessage.of(MessageType.RESULT, 1, new MessagePayload.Result("PLAYER1", false, "GAME_OVER")),
                NetworkMessage.of(MessageType.PAUSE, 1),
                NetworkMessage.of(MessageType.RESUME, 1),
                NetworkMessage.of(MessageType.REMATCH_REQUEST, 1),
                NetworkMessage.of(MessageType.REMATCH_RESPONSE, 1, new MessagePayload.RematchResponse(true)),
                NetworkMessage.of(MessageType.LEAVE, 1),
                NetworkMessage.of(MessageType.PING, 0, new MessagePayload.Ping(123456789L)),
                NetworkMessage.of(MessageType.PONG, 0, new MessagePayload.Ping(-5L)));
    }

    @Test
    void roundTrip_everyMessageTypeIsCovered() {
        List<MessageType> covered = oneOfEachType().stream().map(NetworkMessage::getType).toList();
        assertEquals(List.of(MessageType.values()), covered);
    }

    @Test
    void roundTrip_preservesEveryMessageType() {
        for (NetworkMessage message : oneOfEachType()) {
            assertEquals(message, roundTrip(message), message.getType().name());
        }
    }

    @Test
    void roundTrip_preservesSequenceAndBattleId() {
        NetworkMessage message = NetworkMessage.of(MessageType.LEAVE, 42).withSequence(Long.MAX_VALUE);

        NetworkMessage decoded = roundTrip(message);

        assertEquals(Long.MAX_VALUE, decoded.getSequence());
        assertEquals(42, decoded.getBattleId());
    }

    @Test
    void roundTrip_attackKeepsEmptyCellsAndOrder() {
        NetworkMessage message = NetworkTestData.attack(3, 10);

        MessagePayload.Attack decoded = roundTrip(message).getPayload(MessagePayload.Attack.class);

        assertEquals(10, decoded.rowCount());
        assertTrue(Arrays.deepEquals(NetworkTestData.attackRows(10, 1), decoded.rows()));
        assertNull(decoded.rows()[0][1]);
    }

    @Test
    void roundTrip_snapshotKeepsBoardAttackCellsBlocksAndPending() {
        MessagePayload.Snapshot original = NetworkTestData.snapshot(500);

        MessagePayload.Snapshot decoded = roundTrip(NetworkMessage.of(MessageType.SNAPSHOT, 1, original))
                .getPayload(MessagePayload.Snapshot.class);

        assertTrue(Arrays.deepEquals(original.board(), decoded.board()));
        assertTrue(decoded.attackCells()[Board.ROWS - 1][0]);
        assertFalse(decoded.attackCells()[Board.ROWS - 2][0]);
        assertEquals(original.currentBlock(), decoded.currentBlock());
        assertEquals(original.nextBlock(), decoded.nextBlock());
        assertEquals(2, decoded.pendingAttack().length);
        assertEquals(500, decoded.score());
    }

    @Test
    void roundTrip_snapshotWithoutBlocksAndGameOver() {
        MessagePayload.Snapshot base = NetworkTestData.snapshot(0);
        MessagePayload.Snapshot over = new MessagePayload.Snapshot(base.board(), base.attackCells(),
                null, null, 10, 1, 0, new com.team.tetris.domain.BlockType[0][Board.COLS], true);

        MessagePayload.Snapshot decoded = roundTrip(NetworkMessage.of(MessageType.SNAPSHOT, 1, over))
                .getPayload(MessagePayload.Snapshot.class);

        assertNull(decoded.currentBlock());
        assertNull(decoded.nextBlock());
        assertTrue(decoded.gameOver());
        assertEquals(0, decoded.pendingAttack().length);
    }

    @Test
    void roundTrip_configWithoutDifficulty() {
        NetworkMessage message = NetworkMessage.of(MessageType.CONFIG, 1, new MessagePayload.Config("ITEM", null, 0));

        assertNull(roundTrip(message).getPayload(MessagePayload.Config.class).difficulty());
    }

    @Test
    void roundTrip_resultWithoutWinner() {
        NetworkMessage draw = NetworkMessage.of(MessageType.RESULT, 1, new MessagePayload.Result(null, true, "TIME_UP"));
        NetworkMessage lost = NetworkMessage.of(MessageType.RESULT, 1,
                new MessagePayload.Result(null, false, "CONNECTION_LOST"));

        assertEquals(draw, roundTrip(draw));
        assertEquals(lost, roundTrip(lost));
    }

    @Test
    void snapshot_isSmallEnoughForLowLatency() {
        // 스냅샷은 초당 여러 번 보내므로 1KB 이하로 유지
        assertTrue(codec.encode(NetworkTestData.snapshotMessage(1, 1)).length < 1024);
    }

    @Test
    void encode_rejectsNull() {
        assertThrows(NullPointerException.class, () -> codec.encode(null));
    }

    @Test
    void decode_rejectsNull() {
        assertThrows(NullPointerException.class, () -> codec.decode(null));
    }

    @Test
    void decode_rejectsEmptyData() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode(new byte[0]));
    }

    @Test
    void decode_rejectsOtherProtocolVersion() {
        byte[] data = codec.encode(NetworkMessage.of(MessageType.LEAVE, 1));
        data[0] = MessageCodec.VERSION + 1;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> codec.decode(data));
        assertTrue(e.getMessage().contains("version"));
    }

    @Test
    void decode_rejectsUnknownMessageType() {
        byte[] data = codec.encode(NetworkMessage.of(MessageType.LEAVE, 1));
        data[1] = (byte) MessageType.values().length;

        assertThrows(IllegalArgumentException.class, () -> codec.decode(data));
    }

    @Test
    void decode_rejectsTruncatedMessageOfEveryType() {
        for (NetworkMessage message : oneOfEachType()) {
            byte[] data = codec.encode(message);
            byte[] truncated = Arrays.copyOf(data, data.length - 1);
            assertThrows(IllegalArgumentException.class, () -> codec.decode(truncated), message.getType().name());
        }
    }

    @Test
    void decode_rejectsTrailingBytes() {
        byte[] data = codec.encode(NetworkMessage.of(MessageType.PAUSE, 1));
        byte[] longer = Arrays.copyOf(data, data.length + 1);

        assertThrows(IllegalArgumentException.class, () -> codec.decode(longer));
    }

    @Test
    void decode_rejectsInvalidBlockTypeInAttack() {
        byte[] data = codec.encode(NetworkTestData.attack(1, 1));
        data[data.length - 1] = 99;   // 마지막 셀

        assertThrows(IllegalArgumentException.class, () -> codec.decode(data));
    }

    @Test
    void decode_rejectsTooManyAttackRows() {
        byte[] data = codec.encode(NetworkTestData.attack(1, 1));
        int rowCountIndex = 1 + 1 + 8 + 8;
        data[rowCountIndex] = (byte) (MessagePayload.MAX_ATTACK_ROWS + 1);

        assertThrows(IllegalArgumentException.class, () -> codec.decode(data));
    }

    @Test
    void decode_rejectsNegativeSequence() {
        byte[] data = codec.encode(NetworkMessage.of(MessageType.PAUSE, 1));
        Arrays.fill(data, 2, 10, (byte) 0xFF);   // sequence = -1

        assertThrows(IllegalArgumentException.class, () -> codec.decode(data));
    }

    @Test
    void frame_writeAndReadSeveralFrames() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        byte[] first = codec.encode(NetworkMessage.of(MessageType.PAUSE, 1));
        byte[] second = codec.encode(NetworkTestData.snapshotMessage(1, 9));

        MessageCodec.writeFrame(out, first);
        MessageCodec.writeFrame(out, second);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertArrayEquals(first, MessageCodec.readFrame(in));
        assertArrayEquals(second, MessageCodec.readFrame(in));
    }

    @Test
    void frame_rejectsInvalidLengthOnWrite() {
        DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());

        assertThrows(IllegalArgumentException.class, () -> MessageCodec.writeFrame(out, new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> MessageCodec.writeFrame(out, new byte[MessageCodec.MAX_FRAME_BYTES + 1]));
    }

    @Test
    void frame_rejectsInvalidLengthOnRead() throws IOException {
        for (int length : new int[] {0, -1, MessageCodec.MAX_FRAME_BYTES + 1}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new DataOutputStream(bytes).writeInt(length);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

            assertThrows(IOException.class, () -> MessageCodec.readFrame(in), "length " + length);
        }
    }
}
