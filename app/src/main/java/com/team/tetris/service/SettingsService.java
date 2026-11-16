package com.team.tetris.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import com.team.tetris.domain.Difficulty;
import com.team.tetris.domain.PlayerId;
import com.team.tetris.domain.Settings;
import com.team.tetris.domain.Settings.KeyAction;
import com.team.tetris.domain.Settings.ScreenSize;
import com.team.tetris.util.FileManager;

import javafx.scene.input.KeyCode;

public class SettingsService {

    private static final String FILE_NAME = "settings.properties";
    private static final String KEY_PREFIX = "key.";
    private static final String BATTLE_PREFIX = "battle.";

    private final FileManager fileManager;

    public SettingsService(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public Settings load() {
        Settings settings = new Settings();
        Properties props = new Properties();

        try {
            props.load(new StringReader(fileManager.readFile(FILE_NAME)));
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            return settings;
        }

        try {
            settings.setScreenSize(
                ScreenSize.valueOf(props.getProperty("screenSize", ""))
            );
        } catch (IllegalArgumentException ignored) {}

        settings.setColorBlindMode(
            Boolean.parseBoolean(props.getProperty("colorBlindMode"))
        );

        try {
            settings.setDifficulty(
                Difficulty.valueOf(props.getProperty("difficulty", ""))
            );
        } catch (IllegalArgumentException ignored) {}

        loadGameKeys(settings, props);
        loadBattleKeys(settings, props);

        return settings;
    }

    private void loadGameKeys(Settings settings, Properties props) {
        Map<KeyAction, KeyCode> saved = new EnumMap<>(KeyAction.class);

        for (KeyAction action : KeyAction.values()) {
            if (action.isFixed()) continue;

            KeyCode key = readKey(props, KEY_PREFIX + action.name());
            if (key != null && !Settings.isFixedKey(key)) {
                saved.put(action, key);
            }
        }

        if (!saved.isEmpty() && !settings.setKeys(saved)) {
            for (Map.Entry<KeyAction, KeyCode> entry : saved.entrySet()) {
                settings.setKey(entry.getKey(), entry.getValue());
            }
        }
    }

    private void loadBattleKeys(Settings settings, Properties props) {
        for (PlayerId player : PlayerId.values()) {
            Map<KeyAction, KeyCode> saved =
                new EnumMap<>(KeyAction.class);

            for (KeyAction action : battleActions()) {
                KeyCode key = readKey(
                    props,
                    BATTLE_PREFIX + player.name() + "." + action.name()
                );

                if (key != null) saved.put(action, key);
            }

            if (!saved.isEmpty()) {
                settings.setBattleKeys(player, saved);
            }
        }
    }

    private KeyCode readKey(Properties props, String name) {
        String value = props.getProperty(name);
        if (value == null) return null;

        try {
            return KeyCode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean save(Settings settings) {
        Properties props = new Properties();

        props.setProperty("screenSize", settings.getScreenSize().name());
        props.setProperty(
            "colorBlindMode",
            String.valueOf(settings.isColorBlindMode())
        );
        props.setProperty(
            "difficulty",
            settings.getDifficulty().name()
        );

        for (KeyAction action : KeyAction.values()) {
            if (!action.isFixed()) {
                props.setProperty(
                    KEY_PREFIX + action.name(),
                    settings.getKey(action).name()
                );
            }
        }

        for (PlayerId player : PlayerId.values()) {
            for (KeyAction action : battleActions()) {
                KeyCode key = settings.getBattleKey(player, action);

                if (key != null) {
                    props.setProperty(
                        BATTLE_PREFIX
                            + player.name()
                            + "."
                            + action.name(),
                        key.name()
                    );
                }
            }
        }

        try {
            StringWriter writer = new StringWriter();
            props.store(writer, "Tetris settings");
            fileManager.writeFile(FILE_NAME, writer.toString());
            return true;
        } catch (IOException | UncheckedIOException e) {
            System.err.println("설정 저장 실패: " + e.getMessage());
            return false;
        }
    }

    public Settings resetToDefault() {
        Settings settings = new Settings();
        save(settings);
        return settings;
    }

    private static KeyAction[] battleActions() {
        return new KeyAction[] {
            KeyAction.MOVE_LEFT,
            KeyAction.MOVE_RIGHT,
            KeyAction.SOFT_DROP,
            KeyAction.ROTATE,
            KeyAction.HARD_DROP,
            KeyAction.SKIP
        };
    }
}