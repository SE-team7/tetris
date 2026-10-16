package com.team.tetris.view;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.domain.Difficulty;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

class SettingsViewTest {

    @BeforeAll
    static void startJavaFx() throws Exception {

        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 시작된 경우
        }
    }

    @Test
    void rootExists() throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            Parent root =
                view.getRoot();

            assertNotNull(root);

            return null;
        });
    }

    @Test
    void defaultScreenSizeIsMedium()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            ComboBox<String> combo =
                view.getScreenSizeComboBox();

            assertEquals(
                "MEDIUM",
                combo.getValue()
            );

            assertEquals(
                List.of(
                    "SMALL",
                    "MEDIUM",
                    "LARGE"
                ),
                combo.getItems()
            );

            return null;
        });
    }

    @Test
    void defaultDifficultyIsNormal()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            ComboBox<Difficulty> combo =
                view.getDifficultyComboBox();

            assertEquals(
                Difficulty.NORMAL,
                combo.getValue()
            );

            assertEquals(
                List.of(
                    Difficulty.EASY,
                    Difficulty.NORMAL,
                    Difficulty.HARD
                ),
                combo.getItems()
            );

            return null;
        });
    }

    @Test
    void difficultyCanBeChanged()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.setDifficulty(
                Difficulty.HARD
            );

            assertEquals(
                Difficulty.HARD,
                view
                    .getDifficultyComboBox()
                    .getValue()
            );

            view.setDifficulty(
                Difficulty.EASY
            );

            assertEquals(
                Difficulty.EASY,
                view
                    .getDifficultyComboBox()
                    .getValue()
            );

            return null;
        });
    }

    @Test
    void nullDifficultyDoesNotChangeValue()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.setDifficulty(
                Difficulty.HARD
            );

            view.setDifficulty(
                null
            );

            assertEquals(
                Difficulty.HARD,
                view
                    .getDifficultyComboBox()
                    .getValue()
            );

            return null;
        });
    }

    @Test
    void difficultyHandlerReceivesSelectedDifficulty()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            AtomicReference<Difficulty>
                received =
                    new AtomicReference<>();

            view.setOnDifficultyChanged(
                received::set
            );

            ComboBox<Difficulty> combo =
                view.getDifficultyComboBox();

            combo.setValue(
                Difficulty.HARD
            );

            combo
                .getOnAction()
                .handle(
                    new ActionEvent()
                );

            assertEquals(
                Difficulty.HARD,
                received.get()
            );

            return null;
        });
    }

    @Test
    void difficultyHandlerIgnoresNullValue()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            AtomicReference<Difficulty>
                received =
                    new AtomicReference<>();

            view.setOnDifficultyChanged(
                received::set
            );

            ComboBox<Difficulty> combo =
                view.getDifficultyComboBox();

            combo.setValue(null);

            received.set(null);

            combo
                .getOnAction()
                .handle(
                    new ActionEvent()
                );

            assertNull(
                received.get()
            );

            return null;
        });
    }

    @Test
    void nullDifficultyHandlerDoesNotCrash()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.setOnDifficultyChanged(
                null
            );

            ComboBox<Difficulty> combo =
                view.getDifficultyComboBox();

            combo.setValue(
                Difficulty.EASY
            );

            assertDoesNotThrow(
                () ->
                    combo
                        .getOnAction()
                        .handle(
                            new ActionEvent()
                        )
            );

            return null;
        });
    }

    @Test
    void colorBlindModeIsOffByDefault()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            Button button =
                view.getColorBlindButton();

            assertEquals(
                "OFF",
                button.getText()
            );

            return null;
        });
    }

    @Test
    void colorBlindModeCanBeTurnedOnAndOff()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.setColorBlindMode(
                true
            );

            assertEquals(
                "ON",
                view
                    .getColorBlindButton()
                    .getText()
            );

            view.setColorBlindMode(
                false
            );

            assertEquals(
                "OFF",
                view
                    .getColorBlindButton()
                    .getText()
            );

            return null;
        });
    }

    @Test
    void allKeyActionNamesExist()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            assertEquals(
                List.of(
                    "MOVE_LEFT",
                    "MOVE_RIGHT",
                    "SOFT_DROP",
                    "HARD_DROP",
                    "ROTATE",
                    "SKIP"
                ),
                view.getKeyActionNames()
            );

            return null;
        });
    }

    @Test
    void defaultKeyTextsAreCorrect()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            assertEquals(
                "A",
                view
                    .getKeyButton(
                        "MOVE_LEFT"
                    )
                    .getText()
            );

            assertEquals(
                "D",
                view
                    .getKeyButton(
                        "MOVE_RIGHT"
                    )
                    .getText()
            );

            assertEquals(
                "S",
                view
                    .getKeyButton(
                        "SOFT_DROP"
                    )
                    .getText()
            );

            assertEquals(
                "ENTER",
                view
                    .getKeyButton(
                        "HARD_DROP"
                    )
                    .getText()
            );

            assertEquals(
                "P",
                view
                    .getKeyButton(
                        "ROTATE"
                    )
                    .getText()
            );

            assertEquals(
                "-",
                view
                    .getKeyButton(
                        "SKIP"
                    )
                    .getText()
            );

            return null;
        });
    }

    @Test
    void keyTextCanBeChanged()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.setKeyText(
                "MOVE_LEFT",
                "LEFT"
            );

            assertEquals(
                "LEFT",
                view
                    .getKeyButton(
                        "MOVE_LEFT"
                    )
                    .getText()
            );

            view.setKeyText(
                "SKIP",
                "K"
            );

            assertEquals(
                "K",
                view
                    .getKeyButton(
                        "SKIP"
                    )
                    .getText()
            );

            return null;
        });
    }

    @Test
    void unknownKeyActionReturnsNull()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            assertNull(
                view.getKeyButton(
                    "UNKNOWN"
                )
            );

            return null;
        });
    }

    @Test
    void unknownKeyTextDoesNotCrash()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            assertDoesNotThrow(
                () ->
                    view.setKeyText(
                        "UNKNOWN",
                        "X"
                    )
            );

            return null;
        });
    }

    @Test
    void actionButtonsExist()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            assertNotNull(
                view.getSaveButton()
            );

            assertNotNull(
                view.getBackButton()
            );

            assertNotNull(
                view
                    .getResetDefaultButton()
            );

            assertNotNull(
                view
                    .getResetScoreboardButton()
            );

            assertEquals(
                "저장",
                view
                    .getSaveButton()
                    .getText()
            );

            assertEquals(
                "메인 메뉴",
                view
                    .getBackButton()
                    .getText()
            );

            assertEquals(
                "기본 설정 복구",
                view
                    .getResetDefaultButton()
                    .getText()
            );

            assertEquals(
                "스코어보드 초기화",
                view
                    .getResetScoreboardButton()
                    .getText()
            );

            return null;
        });
    }

    @Test
    void showMessageUpdatesMessageLabel()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.showMessage(
                "저장되었습니다."
            );

            Label messageLabel =
                getField(
                    view,
                    "messageLabel",
                    Label.class
                );

            assertEquals(
                "저장되었습니다.",
                messageLabel.getText()
            );

            return null;
        });
    }

    @Test
    void showMessageCanDisplayEmptyText()
        throws Exception {

        runFx(() -> {

            SettingsView view =
                new SettingsView();

            view.showMessage("");

            Label messageLabel =
                getField(
                    view,
                    "messageLabel",
                    Label.class
                );

            assertEquals(
                "",
                messageLabel.getText()
            );

            return null;
        });
    }

    private static <T> T getField(
        Object target,
        String fieldName,
        Class<T> type
    ) throws Exception {

        Field field =
            target
                .getClass()
                .getDeclaredField(
                    fieldName
                );

        field.setAccessible(
            true
        );

        return type.cast(
            field.get(
                target
            )
        );
    }

    private static <T> T runFx(
        Callable<T> action
    ) throws Exception {

        if (
            Platform
                .isFxApplicationThread()
        ) {

            return action.call();
        }

        FutureTask<T> task =
            new FutureTask<>(
                action
            );

        Platform.runLater(
            task
        );

        return task.get();
    }
}