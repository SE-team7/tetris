package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class OpponentTypeView {

    public enum OpponentType {
        TWO_PLAYER,
        CPU
    }

    private final VBox root = new VBox(18);
    private final List<Label> menuLabels = new ArrayList<>();

    public OpponentTypeView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("battle-type-root");

        Label title = new Label("OPPONENT");
        title.getStyleClass().add("battle-type-title");

        menuLabels.add(new Label("2인 대전"));
        menuLabels.add(new Label("컴퓨터 봇 대전"));

        menuLabels.forEach(label -> label.getStyleClass().add("battle-type-item"));

        root.getChildren().add(title);
        root.getChildren().addAll(menuLabels);

        Label backHint = new Label("TAB : 뒤로가기");
        backHint.getStyleClass().add("battle-back-hint");
        root.getChildren().add(backHint);
        VBox.setMargin(backHint, new Insets(28, 0, 0, 0));

        render(OpponentType.TWO_PLAYER);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(OpponentType selected) {
        for (int i = 0; i < menuLabels.size(); i++) {
            Label label = menuLabels.get(i);

            label.getStyleClass().removeAll(
                "battle-type-selected",
                "battle-type-unselected"
            );

            label.getStyleClass().add(
                i == selected.ordinal()
                    ? "battle-type-selected"
                    : "battle-type-unselected"
            );
        }
    }
}
