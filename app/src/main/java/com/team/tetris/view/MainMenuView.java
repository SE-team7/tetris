package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainMenuView {

    public enum MainMenuItem {
        START_GAME,
        START_ITEM_GAME,
        SETTINGS,
        SCOREBOARD,
        EXIT
    }

    private final VBox root;
    private final List<Label> menuLabels;
    private final Label keyGuideLabel;

    public MainMenuView() {

        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");

        /*
         * TITLE
         */
        Label dropLabel = new Label("DROP");
Label theLabel = new Label("the");
Label sevenLabel = new Label("SEVEN");

dropLabel.setStyle(
    "-fx-font-family: 'Georgia';"
    + "-fx-font-size: 54px;"
    + "-fx-font-weight: bold;"
    + "-fx-text-fill: #222222;"
    + "-fx-effect: dropshadow(gaussian, #b8b8b8, 5, 0.2, 1, 2);"
);

theLabel.setStyle(
    "-fx-font-family: 'Georgia';"
    + "-fx-font-size: 32px;"
    + "-fx-font-style: italic;"
    + "-fx-text-fill: #555555;"
    + "-fx-effect: dropshadow(gaussian, #c8c8c8, 4, 0.2, 1, 1);"
);

sevenLabel.setStyle(
    "-fx-font-family: 'Georgia';"
    + "-fx-font-size: 54px;"
    + "-fx-font-weight: bold;"
    + "-fx-text-fill: #222222;"
    + "-fx-effect: dropshadow(gaussian, #b8b8b8, 5, 0.2, 1, 2);"
);

HBox titleBox = new HBox(
    10,
    dropLabel,
    theLabel,
    sevenLabel
);

titleBox.setAlignment(Pos.BASELINE_CENTER);
        /*
         * MENU
         */
        menuLabels =
            new ArrayList<>();

        menuLabels.add(
            new Label("일반 모드 시작")
        );

        menuLabels.add(
            new Label("아이템 모드 시작")
        );

        menuLabels.add(
            new Label("설정")
        );

        menuLabels.add(
            new Label("스코어보드")
        );

        menuLabels.add(
            new Label("종료")
        );

        for (Label label : menuLabels) {
            label.getStyleClass()
                .add("menu-item");
        }

        /*
         * KEY GUIDE
         */
        keyGuideLabel =
            new Label();

        keyGuideLabel
            .getStyleClass()
            .add("key-guide");

        keyGuideLabel.setStyle(
            "-fx-font-size: 13px;"
            + "-fx-text-fill: #888888;"
        );

        root.getChildren().add(
            titleBox
        );

        root.getChildren().addAll(
            menuLabels
        );

        root.getChildren().add(
            keyGuideLabel
        );

        render(
            MainMenuItem.START_GAME
        );
    }

    public Parent getRoot() {
        return root;
    }

    public void render(
        MainMenuItem selectedMenu
    ) {

        String[] menuTexts = {
            "일반 모드 시작",
            "아이템 모드 시작",
            "설정",
            "스코어보드",
            "종료"
        };

        for (
            int i = 0;
            i < menuLabels.size();
            i++
        ) {

            Label label =
                menuLabels.get(i);

            if (
                i == selectedMenu.ordinal()
            ) {

                label.setText(
                    "▶ " + menuTexts[i]
                );

                label.setStyle(
                    "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #222222;"
                    + "-fx-background-color: #eeeeee;"
                    + "-fx-border-color: #4A90E2;"
                    + "-fx-border-width: 2px;"
                    + "-fx-border-radius: 4px;"
                    + "-fx-background-radius: 4px;"
                    + "-fx-padding: 6 15 6 15;"
                );

            } else {

                label.setText(
                    "   " + menuTexts[i]
                );

                label.setStyle(
                    "-fx-font-size: 20px;"
                    + "-fx-font-weight: normal;"
                    + "-fx-text-fill: #333333;"
                    + "-fx-background-color: transparent;"
                    + "-fx-border-color: transparent;"
                    + "-fx-border-width: 2px;"
                    + "-fx-padding: 6 15 6 15;"
                );
            }
        }
    }

    public void showKeyGuide(
        String moveKeys,
        String selectKey
    ) {

        keyGuideLabel.setText(
            moveKeys
            + ": 이동   "
            + selectKey
            + ": 선택"
        );
    }
}