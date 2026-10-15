package com.team.tetris.view;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.view.MainMenuView.MainMenuItem;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;

class MainMenuViewTest {

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

            MainMenuView view =
                new MainMenuView();

            Parent root =
                view.getRoot();

            assertNotNull(root);

            return null;
        });
    }

    @Test
    void defaultSelectionIsStartGame()
        throws Exception {

        runFx(() -> {

            MainMenuView view =
                new MainMenuView();

            List<Label> labels =
                getMenuLabels(view);

            assertEquals(
                5,
                labels.size()
            );

            assertEquals(
                "▶ 일반 모드 시작",
                labels.get(0).getText()
            );

            assertTrue(
                labels.get(0)
                    .getStyle()
                    .contains(
                        "font-weight: bold"
                    )
            );

            assertEquals(
                "   아이템 모드 시작",
                labels.get(1).getText()
            );

            return null;
        });
    }

    @Test
    void allMenuItemsCanBeSelected()
        throws Exception {

        runFx(() -> {

            MainMenuView view =
                new MainMenuView();

            List<Label> labels =
                getMenuLabels(view);

            String[] texts = {
                "일반 모드 시작",
                "아이템 모드 시작",
                "설정",
                "스코어보드",
                "종료"
            };

            MainMenuItem[] items =
                MainMenuItem.values();

            assertEquals(
                5,
                items.length
            );

            for (
                int selected = 0;
                selected < items.length;
                selected++
            ) {

                view.render(
                    items[selected]
                );

                for (
                    int i = 0;
                    i < labels.size();
                    i++
                ) {

                    Label label =
                        labels.get(i);

                    if (i == selected) {

                        assertEquals(
                            "▶ " + texts[i],
                            label.getText()
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "-fx-font-size: 22px"
                                )
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "-fx-font-weight: bold"
                                )
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "#dddddd"
                                )
                        );

                    } else {

                        assertEquals(
                            "   " + texts[i],
                            label.getText()
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "-fx-font-size: 20px"
                                )
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "-fx-font-weight: normal"
                                )
                        );

                        assertTrue(
                            label.getStyle()
                                .contains(
                                    "transparent"
                                )
                        );
                    }
                }
            }

            return null;
        });
    }

    @Test
    void menuLabelsHaveMenuItemStyleClass()
        throws Exception {

        runFx(() -> {

            MainMenuView view =
                new MainMenuView();

            List<Label> labels =
                getMenuLabels(view);

            for (
                Label label : labels
            ) {

                assertTrue(
                    label
                        .getStyleClass()
                        .contains(
                            "menu-item"
                        )
                );
            }

            return null;
        });
    }

    @Test
    void keyGuideCanBeDisplayed()
        throws Exception {

        runFx(() -> {

            MainMenuView view =
                new MainMenuView();

            view.showKeyGuide(
                "↑/↓",
                "ENTER"
            );

            Label keyGuideLabel =
                getField(
                    view,
                    "keyGuideLabel",
                    Label.class
                );

            assertEquals(
                "↑/↓: 이동   ENTER: 선택",
                keyGuideLabel.getText()
            );

            assertTrue(
                keyGuideLabel
                    .getStyleClass()
                    .contains(
                        "key-guide"
                    )
            );

            return null;
        });
    }

    @Test
    void keyGuideUsesPassedKeys()
        throws Exception {

        runFx(() -> {

            MainMenuView view =
                new MainMenuView();

            view.showKeyGuide(
                "LEFT/RIGHT",
                "SPACE"
            );

            Label keyGuideLabel =
                getField(
                    view,
                    "keyGuideLabel",
                    Label.class
                );

            assertEquals(
                "LEFT/RIGHT: 이동   SPACE: 선택",
                keyGuideLabel.getText()
            );

            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Label> getMenuLabels(
        MainMenuView view
    ) throws Exception {

        Field field =
            MainMenuView.class
                .getDeclaredField(
                    "menuLabels"
                );

        field.setAccessible(true);

        return (List<Label>)
            field.get(view);
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

        field.setAccessible(true);

        return type.cast(
            field.get(target)
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
