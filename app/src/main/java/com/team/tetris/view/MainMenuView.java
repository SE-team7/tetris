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

        Label titleLabel = new Label("TETRIS");
        titleLabel.getStyleClass().add("title");

        menuLabels = new ArrayList<>();

        menuLabels.add(new Label("게임 시작"));
        menuLabels.add(new Label("설정"));
        menuLabels.add(new Label("스코어보드"));
        menuLabels.add(new Label("종료"));

        for (Label label : menuLabels) {
            label.getStyleClass().add("menu-item");
        }

        keyGuideLabel = new Label();
        keyGuideLabel.getStyleClass().add("key-guide");

        root.getChildren().add(titleLabel);
        root.getChildren().addAll(menuLabels);
        root.getChildren().add(keyGuideLabel);

        render(MainMenuItem.START_GAME);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(MainMenuItem selectedMenu) {
        for (int i = 0; i < menuLabels.size(); i++) {
            Label label = menuLabels.get(i);

            label.getStyleClass().remove("menu-item-selected");

            if (i == selectedMenu.ordinal()) {
                label.getStyleClass().add("menu-item-selected");
            }
        }
    }

    public void showKeyGuide(String moveKeys, String selectKey) {
        keyGuideLabel.setText(
            moveKeys + ": 이동   " + selectKey + ": 선택"
        );
    }
}