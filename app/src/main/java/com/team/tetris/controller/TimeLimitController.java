package com.team.tetris.controller;

import java.util.function.IntConsumer;

import com.team.tetris.view.TimeLimitView;
import com.team.tetris.view.TimeLimitView.TimeLimitItem;

import javafx.scene.input.KeyCode;

public class TimeLimitController {

    private final TimeLimitView view;
    private final IntConsumer onSelect;
    private final Runnable onBack;

    private TimeLimitItem selected = TimeLimitItem.THREE_MIN;

    public TimeLimitController(
        TimeLimitView view,
        IntConsumer onSelect,
        Runnable onBack
    ) {
        this.view = view;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    public void show() {
        selected = TimeLimitItem.THREE_MIN;
        view.render(selected);
    }

    public void handleKey(KeyCode key) {
        if (key == KeyCode.UP) {
            move(-1);
        } else if (key == KeyCode.DOWN) {
            move(1);
        } else if (key == KeyCode.ENTER) {
            select();
        } else if (key == KeyCode.ESCAPE) {
            onBack.run();
        }
    }

    private void move(int direction) {
        TimeLimitItem[] items = TimeLimitItem.values();
        int index = (selected.ordinal() + direction + items.length) % items.length;

        selected = items[index];
        view.render(selected);
    }

    private void select() {
        if (selected == TimeLimitItem.BACK) {
            onBack.run();
            return;
        }

        onSelect.accept(view.getMinutes(selected));
    }

    public TimeLimitItem getSelected() {
        return selected;
    }
}