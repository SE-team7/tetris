package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class BattleTypeView {

    public enum BattleTypeItem {
        LOCAL,
        P2P,
        BACK
    }

    private final VBox root = new VBox(18);
    private final List<Label> menuLabels = new ArrayList<>();

    public BattleTypeView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("battle-type-root");

        Label title = new Label("BATTLE TYPE");
        title.getStyleClass().add("battle-type-title");

        menuLabels.add(new Label("로컬 대전"));
        menuLabels.add(new Label("P2P 대전"));

        menuLabels.forEach(label ->
            label.getStyleClass().add("battle-type-item")
        );

        root.getChildren().add(title);
        root.getChildren().addAll(menuLabels);
        Label backHint = new Label("TAB : 뒤로가기");
        backHint.getStyleClass().add("battle-back-hint");
        root.getChildren().add(backHint);
        VBox.setMargin(backHint, new Insets(28, 0, 0, 0));

        render(BattleTypeItem.LOCAL);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(BattleTypeItem selected) {
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