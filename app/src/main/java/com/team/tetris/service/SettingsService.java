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
import com.team.tetris.domain.Settings.Theme;
import com.team.tetris.util.FileManager;

import javafx.scene.input.KeyCode;

public class SettingsService {

    private static final String FILE = "settings.properties";
    private static final String KEY = "key.";
    private static final String BATTLE = "battle.";

    private final FileManager fileManager;

    public SettingsService(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public Settings load() {
        Settings settings = new Settings();
        Properties p = new Properties();

        try {
            p.load(new StringReader(fileManager.readFile(FILE)));
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            return settings;
        }

        try {
            settings.setScreenSize(
                ScreenSize.valueOf(p.getProperty("screenSize", "MEDIUM"))
            );
        } catch (IllegalArgumentException ignored) {}

        try {
            settings.setTheme(
                Theme.valueOf(p.getProperty("theme", "LIGHT"))
            );
        } catch (IllegalArgumentException ignored) {}

        try {
            settings.setDifficulty(
                Difficulty.valueOf(p.getProperty("difficulty", "NORMAL"))
            );
        } catch (IllegalArgumentException ignored) {}

        settings.setColorBlindMode(
            Boolean.parseBoolean(p.getProperty("colorBlindMode", "false"))
        );

        loadNormalKeys(settings, p);
        loadBattleKeys(settings, p);

        return settings;
    }

    private void loadNormalKeys(Settings settings, Properties p) {
        Map<KeyAction, KeyCode> saved = new EnumMap<>(KeyAction.class);

        for (KeyAction action : KeyAction.values()) {
            if (action.isFixed()) continue;

            String value = p.getProperty(KEY + action.name());
            if (value == null) continue;

            try {
                KeyCode key = KeyCode.valueOf(value);
                if (!Settings.isFixedKey(key)) saved.put(action, key);
            } catch (IllegalArgumentException ignored) {}
        }

        if (!settings.setKeys(saved)) {
            for (var entry : saved.entrySet()) {
                settings.setKey(entry.getKey(), entry.getValue());
            }
        }
    }

    private void loadBattleKeys(Settings settings, Properties p) {
        for (PlayerId player : PlayerId.values()) {
            for (KeyAction action : KeyAction.values()) {
                String value = p.getProperty(
                    BATTLE + player.name() + "." + action.name()
                );

                if (value == null) continue;

                try {
                    settings.setBattleKey(
                        player,
                        action,
                        KeyCode.valueOf(value)
                    );
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public boolean save(Settings settings) {
        Properties p = new Properties();

        p.setProperty("screenSize", settings.getScreenSize().name());
        p.setProperty("theme", settings.getTheme().name());
        p.setProperty(
            "colorBlindMode",
            String.valueOf(settings.isColorBlindMode())
        );
        p.setProperty("difficulty", settings.getDifficulty().name());

        for (KeyAction action : KeyAction.values()) {
            if (!action.isFixed()) {
                p.setProperty(
                    KEY + action.name(),
                    settings.getKey(action).name()
                );
            }
        }

        for (PlayerId player : PlayerId.values()) {
            for (KeyAction action : KeyAction.values()) {
                KeyCode key = settings.getBattleKey(player, action);

                if (key != null) {
                    p.setProperty(
                        BATTLE + player.name() + "." + action.name(),
                        key.name()
                    );
                }
            }
        }

        try {
            StringWriter writer = new StringWriter();
            p.store(writer, "Tetris settings");
            fileManager.writeFile(FILE, writer.toString());
            return true;
        } catch (IOException | UncheckedIOException e) {
            return false;
        }
    }

    public Settings resetToDefault() {
        Settings settings = new Settings();
        save(settings);
        return settings;
    }
}