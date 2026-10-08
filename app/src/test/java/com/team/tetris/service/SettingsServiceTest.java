package com.team.tetris.service;

import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.util.FileManager;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void load_returnsDefaultsWhenNoFile() {
        Settings settings = new SettingsService(new FileManager(tempDir)).load();

        assertEquals(ScreenSize.MEDIUM, settings.getScreenSize());
        assertFalse(settings.isColorBlindMode());
        assertEquals(KeyCode.A, settings.getKey(KeyAction.MOVE_LEFT));
        assertEquals(KeyCode.ENTER, settings.getKey(KeyAction.SELECT));
    }

    @Test
    void saveAndLoad() {
        SettingsService service = new SettingsService(new FileManager(tempDir));
        Settings settings = new Settings();
        settings.setScreenSize(ScreenSize.LARGE);
        settings.setColorBlindMode(true);
        settings.setKey(KeyAction.ROTATE, KeyCode.UP);
        service.save(settings);

        Settings loaded = service.load();
        assertEquals(ScreenSize.LARGE, loaded.getScreenSize());
        assertTrue(loaded.isColorBlindMode());
        assertEquals(KeyCode.UP, loaded.getKey(KeyAction.ROTATE));
    }

    @Test
    void resetToDefault() {
        SettingsService service = new SettingsService(new FileManager(tempDir));
        Settings settings = new Settings();
        settings.setColorBlindMode(true);
        service.save(settings);

        assertFalse(service.resetToDefault().isColorBlindMode());
        assertFalse(service.load().isColorBlindMode());
    }

    @Test
    void fileErrors_doNotStopGame() throws IOException {
        // 저장 폴더 자리에 일반 파일이 있어 읽기·쓰기 모두 실패하는 상황
        Path blocker = Files.writeString(tempDir.resolve("blocker"), "");
        SettingsService service = new SettingsService(new FileManager(blocker));

        Settings loaded = assertDoesNotThrow(service::load);
        assertEquals(ScreenSize.MEDIUM, loaded.getScreenSize()); // 기본값으로 시작
        assertDoesNotThrow(() -> service.save(new Settings()));
        assertNotNull(assertDoesNotThrow(service::resetToDefault));
    }

    @Test
    void load_ignoresInvalidValues() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("settings.properties",
                "screenSize=HUGE\nkey.MOVE_LEFT=NOT_A_KEY\nkey.ROTATE=UP\n");

        Settings loaded = new SettingsService(fileManager).load();
        assertEquals(ScreenSize.MEDIUM, loaded.getScreenSize());
        assertEquals(KeyCode.A, loaded.getKey(KeyAction.MOVE_LEFT));
        assertEquals(KeyCode.UP, loaded.getKey(KeyAction.ROTATE));
    }

    @Test
    void setKey_rejectsDuplicateInSameGroupOnly() {
        Settings settings = new Settings();

        // 게임 키끼리 중복 불가
        assertFalse(settings.setKey(KeyAction.ROTATE, KeyCode.A));
        assertEquals(KeyCode.P, settings.getKey(KeyAction.ROTATE));

        // 게임 키와 메뉴 키는 겹쳐도 됨 (기본값도 S, ENTER가 겹침)
        assertTrue(settings.setKey(KeyAction.MENU_UP, KeyCode.A));
    }

    @Test
    void findAction_separatesGameAndMenuKeys() {
        Settings settings = new Settings();

        assertEquals(KeyAction.SOFT_DROP, settings.findGameAction(KeyCode.S));
        assertEquals(KeyAction.MENU_DOWN, settings.findMenuAction(KeyCode.S));
        assertEquals(KeyAction.HARD_DROP, settings.findGameAction(KeyCode.ENTER));
        assertEquals(KeyAction.SELECT, settings.findMenuAction(KeyCode.ENTER));
        assertNull(settings.findMenuAction(KeyCode.Z));
    }
}
