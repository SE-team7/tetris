package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
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

        Label titleLabel =
            new Label("tetris_noname");

        titleLabel.setStyle(
            "-fx-font-size: 52px;"
            + "-fx-font-weight: bold;"
        );

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

        keyGuideLabel =
            new Label();

        keyGuideLabel
            .getStyleClass()
            .add("key-guide");

        root.getChildren().add(
            titleLabel
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
                    + "-fx-background-color: #dddddd;"
                    + "-fx-padding: 6 15 6 15;"
                );

            } else {

                label.setText(
                    "   " + menuTexts[i]
                );

                label.setStyle(
                    "-fx-font-size: 20px;"
                    + "-fx-font-weight: normal;"
                    + "-fx-background-color: transparent;"
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