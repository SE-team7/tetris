package com.team.tetris.network;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.team.tetris.util.FileManager;

/**
 * 최근 접속한 서버 주소 저장.
 * 클라이언트가 P2P 화면에서 이전 주소를 골라 입력할 수 있게 한다.
 * 파일 한 줄에 "주소:포트" 하나, 최근 접속이 맨 위. 설정 파일과 따로 저장한다.
 */
public class RecentHostStore {

    public static final String FILE_NAME = "recent_hosts.txt";
    public static final int MAX_ENTRIES = 5;

    /** 저장된 접속 주소 한 개 */
    public record HostEntry(String host, int port) {
        public HostEntry {
            Objects.requireNonNull(host, "host must not be null");
            host = host.trim();
            if (host.isEmpty() || host.contains(",") || host.contains("\n")) {
                throw new IllegalArgumentException("invalid host: " + host);
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("port must be between 1 and 65535");
            }
        }

        /** "주소:포트" 형식. IPv6 주소는 [주소]:포트 */
        @Override
        public String toString() {
            return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
        }

        /** 저장 형식을 읽는다. 형식이 틀리면 null */
        static HostEntry parse(String line) {
            String text = line.trim();
            int split = text.lastIndexOf(':');
            if (split <= 0 || split == text.length() - 1) {
                return null;
            }
            String host = text.substring(0, split);
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            try {
                return new HostEntry(host, Integer.parseInt(text.substring(split + 1)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final FileManager fileManager;

    public RecentHostStore(FileManager fileManager) {
        this.fileManager = Objects.requireNonNull(fileManager, "fileManager must not be null");
    }

    /** 최근 접속 순 목록. 파일이 없거나 읽을 수 없으면 빈 목록, 잘못된 줄은 건너뜀 */
    public List<HostEntry> load() {
        String content;
        try {
            content = fileManager.readFile(FILE_NAME);
        } catch (UncheckedIOException e) {
            return new ArrayList<>();
        }
        List<HostEntry> entries = new ArrayList<>();
        for (String line : content.split("\\R")) {
            HostEntry entry = HostEntry.parse(line);
            if (entry != null && !entries.contains(entry) && entries.size() < MAX_ENTRIES) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** 가장 최근 접속 주소. 없으면 null */
    public HostEntry getLatest() {
        List<HostEntry> entries = load();
        return entries.isEmpty() ? null : entries.get(0);
    }

    /**
     * 접속에 성공한 주소를 맨 위에 기록. 같은 주소는 위로 옮기고, 최대 MAX_ENTRIES개 유지.
     * 저장에 실패해도 접속은 계속되어야 하므로 false만 반환한다.
     */
    public boolean add(String host, int port) {
        HostEntry entry = new HostEntry(host, port);
        List<HostEntry> entries = load();
        entries.remove(entry);
        entries.add(0, entry);
        if (entries.size() > MAX_ENTRIES) {
            entries = entries.subList(0, MAX_ENTRIES);
        }
        StringBuilder content = new StringBuilder();
        for (HostEntry e : entries) {
            content.append(e).append('\n');
        }
        try {
            fileManager.writeFile(FILE_NAME, content.toString());
            return true;
        } catch (UncheckedIOException e) {
            return false;
        }
    }

    /** 기록 전체 삭제 */
    public boolean clear() {
        try {
            fileManager.writeFile(FILE_NAME, "");
            return true;
        } catch (UncheckedIOException e) {
            return false;
        }
    }
}
