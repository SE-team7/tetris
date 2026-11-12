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
        BATTLE_MODE,
        SETTINGS,
        SCOREBOARD,
        EXIT
    }

    private final VBox root;
    private final List<Label> menuLabels;
    private final Label keyGuideLabel;

    private static final String[] MENU_TEXTS = {
        "일반 모드 시작",
        "아이템 모드 시작",
        "대전 모드",
        "설정",
        "스코어보드",
        "종료"
    };

    public MainMenuView() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label mainTitle = new Label("DROP");
        Label subTitle = new Label("the");
        Label sevenTitle = new Label("SEVEN");

        mainTitle.getStyleClass().add("main-menu-title-main");
        subTitle.getStyleClass().add("main-menu-title-sub");
        sevenTitle.getStyleClass().add("main-menu-title-main");

        HBox titleBox = new HBox(10, mainTitle, subTitle, sevenTitle);
        titleBox.setAlignment(Pos.BASELINE_CENTER);

        menuLabels = new ArrayList<>();

        for (String text : MENU_TEXTS) {
            Label label = new Label(text);
            label.getStyleClass().add("menu-item");
            menuLabels.add(label);
        }

        keyGuideLabel = new Label();
        keyGuideLabel.getStyleClass().addAll(
            "key-guide",
            "main-menu-key-guide"
        );

        root.getChildren().add(titleBox);
        root.getChildren().addAll(menuLabels);
        root.getChildren().add(keyGuideLabel);

        render(MainMenuItem.START_GAME);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(MainMenuItem selectedMenu) {
        if (selectedMenu == null) {
            return;
        }

        for (int i = 0; i < menuLabels.size(); i++) {
            Label label = menuLabels.get(i);
            boolean selected = i == selectedMenu.ordinal();

            label.setText(
                (selected ? "▶ " : "   ") + MENU_TEXTS[i]
            );

            label.getStyleClass().removeAll(
                "menu-item-selected",
                "menu-item-unselected"
            );

            label.getStyleClass().add(
                selected
                    ? "menu-item-selected"
                    : "menu-item-unselected"
            );
        }
    }

    public void showKeyGuide(
        String moveKey,
        String selectKey
    ) {
        keyGuideLabel.setText(
            moveKey + ": 이동   "
                + selectKey + ": 선택"
        );
    }
}