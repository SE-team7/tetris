package com.team.tetris.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import com.team.tetris.domain.BlockType;
import com.team.tetris.domain.Board;
import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.ItemType;

/**
 * NetworkMessage ↔ 바이트 변환.
 * Java 기본 직렬화 대신 필요한 값만 정해진 순서로 쓴다.
 * (보내는 크기를 줄여 지연을 낮추고, 받은 데이터로 임의 클래스가 만들어지지 않게 함)
 *
 * 형식: [버전 1B][종류 1B][순서 번호 8B][대전 번호 8B][종류별 데이터]
 * 소켓에서는 [길이 4B][위 바이트] 단위(프레임)로 주고받는다.
 */
public class MessageCodec {

    /** 형식이 바뀌면 올린다. 버전이 다른 상대의 메시지는 거부 */
    public static final byte VERSION = 1;

    /** 한 프레임 최대 크기. 이보다 크면 잘못된 데이터로 보고 거부 */
    public static final int MAX_FRAME_BYTES = 64 * 1024;

    private static final byte NONE = -1;
    private static final MessageType[] TYPES = MessageType.values();
    private static final BlockType[] BLOCK_TYPES = BlockType.values();
    private static final ItemType[] ITEM_TYPES = ItemType.values();
    private static final Difficulty[] DIFFICULTIES = Difficulty.values();

    /** 메시지를 전송 데이터로 변환 */
    public byte[] encode(NetworkMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(VERSION);
            out.writeByte(message.getType().ordinal());
            out.writeLong(message.getSequence());
            out.writeLong(message.getBattleId());
            writePayload(out, message.getPayload());
        } catch (IOException e) {
            // 메모리에 쓰므로 실제로 발생하지 않음
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /**
     * 완성된 메시지 데이터를 객체로 변환.
     * 버전·종류·값 범위가 맞지 않거나 데이터가 모자라거나 남으면 IllegalArgumentException.
     */
    public NetworkMessage decode(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        ByteArrayInputStream bytes = new ByteArrayInputStream(data);
        try (DataInputStream in = new DataInputStream(bytes)) {
            byte version = in.readByte();
            if (version != VERSION) {
                throw new IllegalArgumentException("unsupported protocol version: " + version);
            }
            MessageType type = readEnum(in.readByte(), TYPES, "message type");
            long sequence = in.readLong();
            long battleId = in.readLong();
            MessagePayload payload = readPayload(in, type);
            if (bytes.available() > 0) {
                throw new IllegalArgumentException("unexpected trailing bytes: " + bytes.available());
            }
            return NetworkMessage.restore(type, sequence, battleId, payload);
        } catch (EOFException e) {
            throw new IllegalArgumentException("truncated message", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** [길이][데이터] 한 프레임 쓰기 */
    static void writeFrame(DataOutputStream out, byte[] data) throws IOException {
        if (data.length == 0 || data.length > MAX_FRAME_BYTES) {
            throw new IllegalArgumentException("invalid frame length: " + data.length);
        }
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    /** 한 프레임 읽기. 길이가 범위를 벗어나면 IOException(연결을 믿을 수 없음) */
    static byte[] readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("invalid frame length: " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    private void writePayload(DataOutputStream out, MessagePayload payload) throws IOException {
        if (payload == null) {
            return;
        }
        switch (payload) {
            case MessagePayload.Config c -> {
                out.writeUTF(c.mode());
                out.writeByte(c.difficulty() == null ? NONE : c.difficulty().ordinal());
                out.writeInt(c.timeLimitSeconds());
            }
            case MessagePayload.Ready r -> out.writeBoolean(r.ready());
            case MessagePayload.Start s -> out.writeLong(s.startDelayMillis());
            case MessagePayload.Attack a -> writeRows(out, a.rows());
            case MessagePayload.Snapshot s -> {
                writeCells(out, s.board());
                writeFlags(out, s.attackCells());
                writeBlock(out, s.currentBlock());
                writeBlock(out, s.nextBlock());
                out.writeInt(s.score());
                out.writeInt(s.level());
                out.writeInt(s.remainingSkips());
                writeRows(out, s.pendingAttack());
                out.writeBoolean(s.gameOver());
            }
            case MessagePayload.GameOver g -> {
                out.writeLong(g.elapsedMillis());
                out.writeInt(g.score());
            }
            case MessagePayload.Result r -> {
                out.writeBoolean(r.winner() != null);
                if (r.winner() != null) {
                    out.writeUTF(r.winner());
                }
                out.writeBoolean(r.draw());
                out.writeUTF(r.reason());
            }
            case MessagePayload.RematchResponse r -> out.writeBoolean(r.accepted());
            case MessagePayload.Ping p -> out.writeLong(p.timestampNanos());
        }
    }

    private MessagePayload readPayload(DataInputStream in, MessageType type) throws IOException {
        return switch (type) {
            case CONFIG -> new MessagePayload.Config(
                    in.readUTF(), readNullableEnum(in.readByte(), DIFFICULTIES, "difficulty"), in.readInt());
            case READY -> new MessagePayload.Ready(in.readBoolean());
            case START -> new MessagePayload.Start(in.readLong());
            case ATTACK -> new MessagePayload.Attack(readRows(in));
            case SNAPSHOT -> new MessagePayload.Snapshot(
                    readCells(in, Board.ROWS),
                    readFlags(in),
                    readBlock(in),
                    readBlock(in),
                    in.readInt(),
                    in.readInt(),
                    in.readInt(),
                    readRows(in),
                    in.readBoolean());
            case GAME_OVER -> new MessagePayload.GameOver(in.readLong(), in.readInt());
            case RESULT -> {
                String winner = in.readBoolean() ? in.readUTF() : null;
                yield new MessagePayload.Result(winner, in.readBoolean(), in.readUTF());
            }
            case REMATCH_RESPONSE -> new MessagePayload.RematchResponse(in.readBoolean());
            case PING, PONG -> new MessagePayload.Ping(in.readLong());
            case PAUSE, RESUME, REMATCH_REQUEST, LEAVE -> null;
        };
    }

    // 공격 행: [줄 수 1B][줄 수 × COLS 셀]
    private void writeRows(DataOutputStream out, BlockType[][] rows) throws IOException {
        out.writeByte(rows.length);
        writeCells(out, rows);
    }

    private BlockType[][] readRows(DataInputStream in) throws IOException {
        int count = in.readUnsignedByte();
        if (count > MessagePayload.MAX_ATTACK_ROWS) {
            throw new IllegalArgumentException("too many attack rows: " + count);
        }
        return readCells(in, count);
    }

    // 셀 1칸 = 1B (빈칸 -1, 그 외 BlockType 순번)
    private void writeCells(DataOutputStream out, BlockType[][] cells) throws IOException {
        for (BlockType[] row : cells) {
            for (BlockType cell : row) {
                out.writeByte(cell == null ? NONE : cell.ordinal());
            }
        }
    }

    private BlockType[][] readCells(DataInputStream in, int rows) throws IOException {
        BlockType[][] cells = new BlockType[rows][Board.COLS];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                cells[row][col] = readNullableEnum(in.readByte(), BLOCK_TYPES, "block type");
            }
        }
        return cells;
    }

    private void writeFlags(DataOutputStream out, boolean[][] flags) throws IOException {
        for (boolean[] row : flags) {
            for (boolean flag : row) {
                out.writeBoolean(flag);
            }
        }
    }

    private boolean[][] readFlags(DataInputStream in) throws IOException {
        boolean[][] flags = new boolean[Board.ROWS][Board.COLS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                flags[row][col] = in.readBoolean();
            }
        }
        return flags;
    }

    private void writeBlock(DataOutputStream out, MessagePayload.BlockState block) throws IOException {
        out.writeBoolean(block != null);
        if (block == null) {
            return;
        }
        out.writeByte(block.type().ordinal());
        out.writeInt(block.row());
        out.writeInt(block.col());
        out.writeByte(block.rotation());
        out.writeByte(block.itemType().ordinal());
        out.writeByte(block.itemCellIndex());
    }

    private MessagePayload.BlockState readBlock(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return new MessagePayload.BlockState(
                readEnum(in.readByte(), BLOCK_TYPES, "block type"),
                in.readInt(),
                in.readInt(),
                in.readByte(),
                readEnum(in.readByte(), ITEM_TYPES, "item type"),
                in.readByte());
    }

    private static <E> E readEnum(int ordinal, E[] values, String name) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("invalid " + name + ": " + ordinal);
        }
        return values[ordinal];
    }

    private static <E> E readNullableEnum(int ordinal, E[] values, String name) {
        return ordinal == NONE ? null : readEnum(ordinal, values, name);
    }
}
