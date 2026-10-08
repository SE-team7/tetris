package com.team.tetris.service;

import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.util.FileManager;

import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * 설정 파일 저장·불러오기 담당.
 * 파일 형식: Properties (screenSize=MEDIUM, colorBlindMode=false, key.MOVE_LEFT=A ...)
 */
public class SettingsService {

    private static final String FILE_NAME = "settings.properties";
    private static final String SCREEN_SIZE = "screenSize";
    private static final String COLOR_BLIND_MODE = "colorBlindMode";
    private static final String KEY_PREFIX = "key.";

    private final FileManager fileManager;

    public SettingsService(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /** 저장된 설정을 불러온다. 파일이 없거나 값이 잘못되면 그 항목은 기본값을 쓴다. */
    public Settings load() {
        Settings settings = new Settings();
        Properties props = new Properties();
        try {
            props.load(new StringReader(fileManager.readFile(FILE_NAME)));
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            // 읽기 실패 시 기본 설정으로 시작 (게임은 계속 진행)
            System.err.println("설정 불러오기 실패: " + e.getMessage());
            return settings;
        }

        try {
            settings.setScreenSize(ScreenSize.valueOf(props.getProperty(SCREEN_SIZE, "")));
        } catch (IllegalArgumentException ignored) {
            // 기본값 유지
        }
        settings.setColorBlindMode(Boolean.parseBoolean(props.getProperty(COLOR_BLIND_MODE)));

        for (KeyAction action : KeyAction.values()) {
            String value = props.getProperty(KEY_PREFIX + action.name());
            if (value == null) {
                continue;
            }
            try {
                settings.setKey(action, KeyCode.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // 기본값 유지
            }
        }
        return settings;
    }

    /** 설정을 파일에 저장한다. 저장에 실패해도 예외를 던지지 않는다. (현재 실행 중에는 설정이 유지됨) */
    public void save(Settings settings) {
        Properties props = new Properties();
        props.setProperty(SCREEN_SIZE, settings.getScreenSize().name());
        props.setProperty(COLOR_BLIND_MODE, String.valueOf(settings.isColorBlindMode()));
        for (KeyAction action : KeyAction.values()) {
            props.setProperty(KEY_PREFIX + action.name(), settings.getKey(action).name());
        }

        StringWriter writer = new StringWriter();
        try {
            props.store(writer, "Tetris settings");
            fileManager.writeFile(FILE_NAME, writer.toString());
        } catch (IOException | UncheckedIOException e) {
            System.err.println("설정 저장 실패: " + e.getMessage());
        }
    }

    /** 모든 설정을 기본값으로 되돌려 저장하고, 기본 설정을 반환한다. */
    public Settings resetToDefault() {
        Settings settings = new Settings();
        save(settings);
        return settings;
    }
}
