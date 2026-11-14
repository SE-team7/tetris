package com.team.tetris.view;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TimeLimitView {

    public enum TimeLimitItem {
        THREE_MIN,
        FIVE_MIN,
        TEN_MIN,
        BACK
    }

    private final VBox root = new VBox(18);
    private final List<Label> menuLabels = new ArrayList<>();

    public TimeLimitView() {
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("time-limit-root");

        Label title = new Label("TIME LIMIT");
        title.getStyleClass().add("time-limit-title");

        menuLabels.add(new Label("3분"));
        menuLabels.add(new Label("5분"));
        menuLabels.add(new Label("10분"));
        menuLabels.add(new Label("뒤로가기"));

        menuLabels.forEach(label ->
            label.getStyleClass().add("time-limit-item")
        );

        root.getChildren().add(title);
        root.getChildren().addAll(menuLabels);

        render(TimeLimitItem.THREE_MIN);
    }

    public Parent getRoot() {
        return root;
    }

    public void render(TimeLimitItem selected) {
        for (int i = 0; i < menuLabels.size(); i++) {
            Label label = menuLabels.get(i);

            label.getStyleClass().removeAll(
                "time-limit-selected",
                "time-limit-unselected"
            );

            label.getStyleClass().add(
                i == selected.ordinal()
                    ? "time-limit-selected"
                    : "time-limit-unselected"
            );
        }
    }

    public int getMinutes(TimeLimitItem item) {
        return switch (item) {
            case THREE_MIN -> 3;
            case FIVE_MIN -> 5;
            case TEN_MIN -> 10;
            case BACK -> 0;
        };
    }
}