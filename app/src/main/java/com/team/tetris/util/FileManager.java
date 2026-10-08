package com.team.tetris.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 설정·스코어보드 파일 입출력 담당.
 * 파일 이름만 받고, 실제 저장 위치는 baseDir 아래로 정한다. (OS별 절대 경로 하드코딩 방지)
 */
public class FileManager {

    private final Path baseDir;

    // 기본 저장 위치: {사용자 홈}/.tetris/
    public FileManager() {
        this(Path.of(System.getProperty("user.home"), ".tetris"));
    }

    public FileManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    /** 파일 내용을 읽는다. 파일이 없으면 빈 문자열을 반환한다. */
    public String readFile(String fileName) {
        Path path = baseDir.resolve(fileName);
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 읽기 실패: " + path, e);
        }
    }

    /** 파일 내용을 덮어쓴다. 폴더가 없으면 만든다. */
    public void writeFile(String fileName, String content) {
        Path path = baseDir.resolve(fileName);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 쓰기 실패: " + path, e);
        }
    }
}
