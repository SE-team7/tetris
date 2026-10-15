package com.team.tetris.service;

import com.team.tetris.domain.Difficulty;
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
        assertEquals(KeyCode.K, settings.getKey(KeyAction.SKIP));
        assertEquals(KeyCode.SHIFT, settings.getKey(KeyAction.PAUSE));
    }

    @Test
    void difficulty_defaultSaveLoadAndReset() {
        SettingsService service = new SettingsService(new FileManager(tempDir));
        assertEquals(Difficulty.NORMAL, service.load().getDifficulty());

        Settings settings = new Settings();
        settings.setDifficulty(Difficulty.HARD);
        service.save(settings);
        assertEquals(Difficulty.HARD, service.load().getDifficulty());

        assertEquals(Difficulty.NORMAL, service.resetToDefault().getDifficulty());
        assertEquals(Difficulty.NORMAL, service.load().getDifficulty());
    }

    @Test
    void difficulty_invalidValueUsesDefault() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("settings.properties", "difficulty=VERY_HARD\n");

        assertEquals(Difficulty.NORMAL, new SettingsService(fileManager).load().getDifficulty());
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
    void setKey_rejectsDuplicateKey() {
        Settings settings = new Settings();

        assertFalse(settings.setKey(KeyAction.ROTATE, KeyCode.A)); // MOVE_LEFT가 사용 중
        assertEquals(KeyCode.P, settings.getKey(KeyAction.ROTATE));
        assertTrue(settings.setKey(KeyAction.ROTATE, KeyCode.UP));
    }

    @Test
    void skipKey_changeSaveLoadAndReset() {
        SettingsService service = new SettingsService(new FileManager(tempDir));
        Settings settings = new Settings();
        assertTrue(settings.setKey(KeyAction.SKIP, KeyCode.Q));
        service.save(settings);

        assertEquals(KeyCode.Q, service.load().getKey(KeyAction.SKIP));
        assertEquals(KeyCode.K, service.resetToDefault().getKey(KeyAction.SKIP));
        assertEquals(KeyCode.K, service.load().getKey(KeyAction.SKIP));
    }

    @Test
    void fixedKeys_cannotBeChangedOrAssigned() {
        Settings settings = new Settings();

        // 고정 동작은 변경 불가
        assertFalse(settings.setKey(KeyAction.PAUSE, KeyCode.Z));
        assertFalse(settings.setKey(KeyAction.QUIT, KeyCode.Z));
        assertFalse(settings.setKey(KeyAction.EXIT, KeyCode.Z));
        assertEquals(KeyCode.SHIFT, settings.getKey(KeyAction.PAUSE));

        // 고정키를 다른 동작에 지정 불가
        assertFalse(settings.setKey(KeyAction.SKIP, KeyCode.SHIFT));
        assertFalse(settings.setKey(KeyAction.ROTATE, KeyCode.TAB));
        assertFalse(settings.setKey(KeyAction.HARD_DROP, KeyCode.ESCAPE));
        assertTrue(Settings.isFixedKey(KeyCode.TAB));
        assertFalse(Settings.isFixedKey(KeyCode.K));
    }

    @Test
    void fixedKeys_inFileAreIgnored() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("settings.properties",
                "key.PAUSE=P\nkey.SKIP=TAB\nkey.MOVE_LEFT=J\n");

        Settings loaded = new SettingsService(fileManager).load();
        assertEquals(KeyCode.SHIFT, loaded.getKey(KeyAction.PAUSE));
        assertEquals(KeyCode.K, loaded.getKey(KeyAction.SKIP));
        assertEquals(KeyCode.J, loaded.getKey(KeyAction.MOVE_LEFT)); // 올바른 키는 적용
    }

    @Test
    void swappedKeys_surviveSaveLoadAndReset() {
        SettingsService service = new SettingsService(new FileManager(tempDir));
        Settings settings = new Settings();
        // ROTATE(P)와 SKIP(K)를 서로 바꿈
        assertTrue(settings.setKey(KeyAction.SKIP, KeyCode.Z));
        assertTrue(settings.setKey(KeyAction.ROTATE, KeyCode.K));
        assertTrue(settings.setKey(KeyAction.SKIP, KeyCode.P));
        service.save(settings);

        Settings loaded = service.load();
        assertEquals(KeyCode.K, loaded.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.P, loaded.getKey(KeyAction.SKIP));

        // 바뀐 상태에서 기본 설정 복구
        loaded.copyFrom(service.resetToDefault());
        assertEquals(KeyCode.P, loaded.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.K, loaded.getKey(KeyAction.SKIP));
    }

    @Test
    void swappedKeys_loadEvenWithInvalidFixedKeyInFile() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("settings.properties",
                "key.ROTATE=K\nkey.SKIP=P\nkey.MOVE_LEFT=SHIFT\n");

        Settings loaded = new SettingsService(fileManager).load();
        assertEquals(KeyCode.K, loaded.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.P, loaded.getKey(KeyAction.SKIP));
        assertEquals(KeyCode.A, loaded.getKey(KeyAction.MOVE_LEFT)); // 고정키라 무시
    }

    @Test
    void oldMenuKeysInFileAreIgnored() {
        FileManager fileManager = new FileManager(tempDir);
        fileManager.writeFile("settings.properties",
                "key.MENU_UP=W\nkey.MENU_DOWN=S\nkey.SELECT=ENTER\nkey.ROTATE=UP\n");

        Settings loaded = new SettingsService(fileManager).load();
        assertEquals(KeyCode.UP, loaded.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.S, loaded.getKey(KeyAction.SOFT_DROP));
    }

    @Test
    void findGameAction() {
        Settings settings = new Settings();

        assertEquals(KeyAction.SOFT_DROP, settings.findGameAction(KeyCode.S));
        assertEquals(KeyAction.HARD_DROP, settings.findGameAction(KeyCode.ENTER));
        assertEquals(KeyAction.SKIP, settings.findGameAction(KeyCode.K));
        assertEquals(KeyAction.QUIT, settings.findGameAction(KeyCode.TAB));
        assertNull(settings.findGameAction(KeyCode.Z));
    }

    @Test
    void save_returnsTrueOnSuccessAndFalseOnFailure() throws IOException {
        assertTrue(new SettingsService(new FileManager(tempDir)).save(new Settings()));

        Path blocker = Files.writeString(tempDir.resolve("blocker"), "");
        assertFalse(new SettingsService(new FileManager(blocker)).save(new Settings()));
    }

    @Test
    void unreadableFile_usesDefaults() throws IOException {
        // 설정 파일 자리에 폴더가 있어 읽을 수 없는 상황
        Files.createDirectory(tempDir.resolve("settings.properties"));

        Settings loaded = assertDoesNotThrow(() -> new SettingsService(new FileManager(tempDir)).load());
        assertEquals(ScreenSize.MEDIUM, loaded.getScreenSize());
        assertEquals(KeyCode.K, loaded.getKey(KeyAction.SKIP));
    }

    @Test
    void brokenFileFormat_usesDefaults() {
        FileManager fileManager = new FileManager(tempDir);
        // Properties 형식으로 읽을 수 없는 잘못된 유니코드 이스케이프
        fileManager.writeFile("settings.properties", "screenSize=\\uZZZZ\n");

        Settings loaded = assertDoesNotThrow(() -> new SettingsService(fileManager).load());
        assertEquals(ScreenSize.MEDIUM, loaded.getScreenSize());
    }

    @Test
    void duplicateKeysInFile_applyOnlyValidKeys() {
        FileManager fileManager = new FileManager(tempDir);
        // ROTATE=A는 MOVE_LEFT(A)와 겹쳐서 한 번에 적용 실패 → 하나씩 적용, 규칙에 맞는 SKIP만 반영
        fileManager.writeFile("settings.properties", "key.ROTATE=A\nkey.SKIP=Q\n");

        Settings loaded = new SettingsService(fileManager).load();
        assertEquals(KeyCode.P, loaded.getKey(KeyAction.ROTATE));
        assertEquals(KeyCode.Q, loaded.getKey(KeyAction.SKIP));
        assertEquals(KeyCode.A, loaded.getKey(KeyAction.MOVE_LEFT));
    }
}
