package com.team.tetris;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * JavaFX 화면(View)을 쓰는 테스트용 도우미.
 * JavaFX를 한 번만 시작하고, 테스트 코드를 JavaFX 스레드에서 실행한다.
 */
public final class FxTestSupport {

    private FxTestSupport() {
    }

    public static void startJavaFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX가 이미 시작된 경우
        }
    }

    /** JavaFX 스레드에서 실행하고 결과를 돌려준다. 실패(assert 포함)는 그대로 다시 던진다. */
    public static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw e;
        }
    }

    public interface FxAction {
        void run() throws Exception;
    }

    public static void onFx(FxAction action) throws Exception {
        onFx(() -> {
            action.run();
            return null;
        });
    }
}
