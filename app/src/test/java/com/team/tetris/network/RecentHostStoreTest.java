package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.team.tetris.network.RecentHostStore.HostEntry;
import com.team.tetris.util.FileManager;

/** RecentHostStore: 최근 접속 IP 저장·불러오기 (요구사항: 최근 접속 IP 저장 후 클라이언트가 확인·입력) */
class RecentHostStoreTest {

    @TempDir
    Path tempDir;

    private RecentHostStore store() {
        return new RecentHostStore(new FileManager(tempDir));
    }

    @Test
    void load_returnsEmptyWhenNoFile() {
        assertTrue(store().load().isEmpty());
        assertNull(store().getLatest());
    }

    @Test
    void add_savesAndLoadsAcrossInstances() {
        assertTrue(store().add("192.168.0.10", 7777));

        assertEquals(List.of(new HostEntry("192.168.0.10", 7777)), store().load());
        assertTrue(Files.exists(tempDir.resolve(RecentHostStore.FILE_NAME)));
    }

    @Test
    void add_putsLatestFirst() {
        RecentHostStore store = store();
        store.add("10.0.0.1", 7777);
        store.add("10.0.0.2", 7777);

        assertEquals(new HostEntry("10.0.0.2", 7777), store.getLatest());
        assertEquals(new HostEntry("10.0.0.1", 7777), store.load().get(1));
    }

    @Test
    void add_movesExistingEntryToTopWithoutDuplicate() {
        RecentHostStore store = store();
        store.add("10.0.0.1", 7777);
        store.add("10.0.0.2", 7777);
        store.add("10.0.0.1", 7777);

        assertEquals(List.of(new HostEntry("10.0.0.1", 7777), new HostEntry("10.0.0.2", 7777)), store.load());
    }

    @Test
    void add_treatsDifferentPortAsDifferentEntry() {
        RecentHostStore store = store();
        store.add("10.0.0.1", 7777);
        store.add("10.0.0.1", 8888);

        assertEquals(2, store.load().size());
    }

    @Test
    void add_keepsOnlyMostRecentEntries() {
        RecentHostStore store = store();
        for (int i = 1; i <= RecentHostStore.MAX_ENTRIES + 2; i++) {
            store.add("10.0.0." + i, 7777);
        }

        List<HostEntry> entries = store.load();
        assertEquals(RecentHostStore.MAX_ENTRIES, entries.size());
        assertEquals("10.0.0." + (RecentHostStore.MAX_ENTRIES + 2), entries.get(0).host());
        assertEquals("10.0.0.3", entries.get(RecentHostStore.MAX_ENTRIES - 1).host());
    }

    @Test
    void add_trimsHost() {
        store().add("  10.0.0.9 ", 7777);

        assertEquals("10.0.0.9", store().getLatest().host());
    }

    @Test
    void add_rejectsInvalidValues() {
        RecentHostStore store = store();

        assertThrows(NullPointerException.class, () -> store.add(null, 7777));
        assertThrows(IllegalArgumentException.class, () -> store.add(" ", 7777));
        assertThrows(IllegalArgumentException.class, () -> store.add("a,b", 7777));
        assertThrows(IllegalArgumentException.class, () -> store.add("10.0.0.1", 0));
        assertThrows(IllegalArgumentException.class, () -> store.add("10.0.0.1", 65536));
    }

    @Test
    void ipv6Host_roundTrips() {
        store().add("fe80::1", 7777);

        assertEquals(new HostEntry("fe80::1", 7777), store().getLatest());
        assertEquals("[fe80::1]:7777", store().getLatest().toString());
    }

    @Test
    void load_skipsBrokenLines() throws IOException {
        Files.writeString(tempDir.resolve(RecentHostStore.FILE_NAME),
                "10.0.0.1:7777\n\nbroken\n:7777\n10.0.0.2:\n10.0.0.3:abc\n10.0.0.4:99999\n10.0.0.5:8000\n");

        assertEquals(List.of(new HostEntry("10.0.0.1", 7777), new HostEntry("10.0.0.5", 8000)), store().load());
    }

    @Test
    void load_returnsEmptyWhenFileCannotBeRead() throws IOException {
        // 파일 자리에 폴더가 있으면 읽기 실패
        Files.createDirectories(tempDir.resolve(RecentHostStore.FILE_NAME));

        assertTrue(store().load().isEmpty());
    }

    @Test
    void add_returnsFalseWhenCannotWrite() throws IOException {
        Path blocker = Files.writeString(tempDir.resolve("blocker"), "");
        RecentHostStore store = new RecentHostStore(new FileManager(blocker));

        assertFalse(store.add("10.0.0.1", 7777));
        assertFalse(store.clear());
    }

    @Test
    void clear_removesAllEntries() {
        RecentHostStore store = store();
        store.add("10.0.0.1", 7777);

        assertTrue(store.clear());
        assertTrue(store.load().isEmpty());
    }

    @Test
    void constructor_rejectsNull() {
        assertThrows(NullPointerException.class, () -> new RecentHostStore(null));
    }
}
