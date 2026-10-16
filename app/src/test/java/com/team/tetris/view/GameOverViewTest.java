package com.team.tetris.view;

import static com.team.tetris.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.team.tetris.FxTestSupport;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

class GameOverViewTest {

    @BeforeAll
    static void startJavaFx() {
        FxTestSupport.startJavaFx();
    }

    // 화면 구성: [제목, 결과, 이름 입력 영역(새 기록 안내, 이름), 키 안내]
    private static Label resultLabel(GameOverView view) {
        return (Label) ((VBox) view.getRoot()).getChildren().get(1);
    }

    private static VBox nameInputBox(GameOverView view) {
        return (VBox) ((VBox) view.getRoot()).getChildren().get(2);
    }

    private static Label newRecordLabel(GameOverView view) {
        return (Label) nameInputBox(view).getChildren().get(0);
    }

    private static Label nameLabel(GameOverView view) {
        return (Label) nameInputBox(view).getChildren().get(1);
    }

    private static Label keyGuide(GameOverView view) {
        return (Label) ((VBox) view.getRoot()).getChildren().get(3);
    }

    @Test
    void showResult_showsScoreAndHidesNameInput() throws Exception {
        onFx(() -> {
            GameOverView view = new GameOverView();
            view.showResult(1200, 5, 30);

            assertNotNull(view.getRoot());
            assertEquals("점수: 1200   레벨: 5   줄: 30", resultLabel(view).getText());
            assertFalse(nameInputBox(view).isVisible());
            assertFalse(nameInputBox(view).isManaged());
            assertEquals("ENTER: 스코어보드 보기", keyGuide(view).getText());
        });
    }

    @Test
    void showNameInput_showsNewRecordAndEmptyName() throws Exception {
        onFx(() -> {
            GameOverView view = new GameOverView();
            view.showResult(1200, 5, 30);
            view.showNameInput(1200);

            assertTrue(nameInputBox(view).isVisible());
            assertTrue(nameInputBox(view).isManaged());
            assertTrue(newRecordLabel(view).getText().contains("1200"));
            assertEquals("_", nameLabel(view).getText());
            assertTrue(keyGuide(view).getText().contains("ENTER: 저장"));
        });
    }

    @Test
    void updateName_showsTypedNameWithCursor() throws Exception {
        onFx(() -> {
            GameOverView view = new GameOverView();
            view.showNameInput(100);
            view.updateName("KIM");

            assertEquals("KIM_", nameLabel(view).getText());
        });
    }
}
