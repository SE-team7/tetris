package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class BattleModeView {

    public enum BattleModeItem {
        NORMAL,
        ITEM,
        TIME_LIMIT,
        BACK
    }

    private final VBox root = new VBox(18);
    private final List<Label> menuLabels = new ArrayList<>();

    public BattleModeView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("battle-mode-root");

        Label title = new Label("BATTLE MODE");
        title.getStyleClass().add("battle-mode-title");

        menuLabels.add(new Label("일반 모드"));
        menuLabels.add(new Label("아이템 모드"));
        menuLabels.add(new Label("시간제한 모드"));
        menuLabels.add(new Label("뒤로가기"));

        menuLabels.forEach(label ->
            label.getStyleClass().add("battle-mode-item")
        );

        root.getChildren().add(title);
        root.getChildren().addAll(menuLabels);

        render(BattleModeItem.NORMAL);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(BattleModeItem selected) {
        for (int i = 0; i < menuLabels.size(); i++) {
            Label label = menuLabels.get(i);

            label.getStyleClass().removeAll(
                "battle-mode-selected",
                "battle-mode-unselected"
            );

            label.getStyleClass().add(
                i == selected.ordinal()
                    ? "battle-mode-selected"
                    : "battle-mode-unselected"
            );
        }
    }
}