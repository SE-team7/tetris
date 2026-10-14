package com.team.tetris;

/**
 * 배포용 진입점.
 *
 * jar·jpackage로 실행할 때는 JavaFX가 클래스패스에 올라가는데,
 * 이때 Application을 상속한 클래스(Main)를 바로 실행하면 JavaFX가 시작되지 않는다.
 * 그래서 Application을 상속하지 않은 이 클래스에서 Main을 실행한다.
 */
public class Launcher {

    public static void main(String[] args) {
        Main.main(args);
    }
}
