package com.team.tetris.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void readFile_returnsEmptyWhenFileMissing() {
        FileManager fileManager = new FileManager(tempDir);

        assertEquals("", fileManager.readFile("missing.txt"));
    }

    @Test
    void writeAndRead() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("test.txt", "홍길동,100\nKIM,200\n");

        assertEquals("홍길동,100\nKIM,200\n", fileManager.readFile("test.txt"));
    }

    @Test
    void writeFile_overwrites() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("test.txt", "first");
        fileManager.writeFile("test.txt", "second");

        assertEquals("second", fileManager.readFile("test.txt"));
    }

    @Test
    void writeFile_createsMissingDirectories() {
        Path baseDir = tempDir.resolve("a").resolve("b");
        FileManager fileManager = new FileManager(baseDir);
        fileManager.writeFile("test.txt", "hello");

        assertTrue(Files.exists(baseDir.resolve("test.txt")));
    }

    @Test
    void writeFile_throwsWhenCannotWrite() throws IOException {
        // 저장 폴더 자리에 일반 파일이 있으면 폴더를 만들 수 없음
        Path blocker = Files.writeString(tempDir.resolve("blocker"), "");
        FileManager fileManager = new FileManager(blocker);

        assertThrows(UncheckedIOException.class, () -> fileManager.writeFile("test.txt", "hello"));
    }

    @Test
    void readFile_throwsWhenCannotRead() throws IOException {
        // 파일 이름 자리에 폴더가 있으면 읽을 수 없음
        Files.createDirectory(tempDir.resolve("folder"));
        FileManager fileManager = new FileManager(tempDir);

        assertThrows(UncheckedIOException.class, () -> fileManager.readFile("folder"));
    }

    @Test
    void defaultConstructor_usesHomeFolder() {
        // 실제 사용자 폴더에는 쓰지 않고, 없는 파일 읽기만 확인
        FileManager fileManager = new FileManager();

        assertEquals("", fileManager.readFile("no-such-file-" + System.nanoTime() + ".txt"));
    }
}
