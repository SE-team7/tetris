# Tetris 게임 개발

## 개발 환경

- Java: JDK 21
- JavaFX: 21
- Build Tool: Gradle Wrapper
- Test: JUnit Jupiter
- IDE: VS Code
- 개발 OS: Windows / macOS

> 프로젝트는 Gradle Wrapper를 사용합니다. 팀원별로 별도의 Gradle 버전을 설치할 필요가 없습니다.
> JavaFX 의존성도 Gradle에서 관리하므로 JavaFX SDK를 별도로 다운로드하여 경로를 설정할 필요가 없습니다.

## 프로젝트 구조

```text
tetris/
├─ app/
│  ├─ build.gradle
│  └─ src/
│     ├─ main/
│     │  ├─ java/
│     │  │  └─ com/team/tetris/
│     │  │     └─ Main.java
│     │  └─ resources/
│     └─ test/
├─ gradle/
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
└─ settings.gradle
```

`app`은 실제 테트리스 애플리케이션이 위치하는 Gradle 서브프로젝트

## 실행 전 준비

JDK 21 사용을 권장하며, 프로젝트 자체의 Java 버전은 Gradle Java Toolchain에서 21로 지정되어 있습니다.

## 실행 방법

저장소를 clone한 후 프로젝트 루트(`tetris`)에서 실행합니다.

### Windows

```powershell
.\gradlew.bat run
```

### macOS

최초 실행 시 필요하면 Gradle Wrapper에 실행 권한을 부여합니다.

```bash
chmod +x gradlew
./gradlew run
```

정상 실행되면 JavaFX 게임 창이 표시됩니다.

## 빌드

### Windows

```powershell
.\gradlew.bat clean build
```

### macOS

```bash
./gradlew clean build
```

## 테스트

### Windows

```powershell
.\gradlew.bat test
```

### macOS

```bash
./gradlew test
```

테스트 코드는 필요에 따라 다음 경로에 작성합니다.

```text
app/src/test/java/com/team/tetris/
```

## 개발 규칙

- 공통 개발 버전은 Java 21 / JavaFX 21을 사용합니다.
- Gradle 명령은 가급적 프로젝트에 포함된 Gradle Wrapper(`gradlew`, `gradlew.bat`)를 사용합니다.
- 개인 PC의 절대 경로(JAVA_HOME 경로 등)를 프로젝트 설정 파일에 작성하지 않습니다.
- 새로운 외부 라이브러리가 필요한 경우 `app/build.gradle`의 `dependencies`에 추가하고 팀에 공유합니다.
- `build/`, `.gradle/` 등 자동 생성 파일은 Git에 커밋하지 않습니다.
- 소스 코드와 리소스에서는 Windows/macOS에 종속적인 절대 경로 사용을 피합니다.

## 주요 Gradle 설정

현재 `app/build.gradle`에는 다음 공통 환경이 설정되어 있습니다.

- Java Toolchain 21
- JavaFX 21
  - `javafx.controls`
  - `javafx.fxml`
- JUnit Jupiter
- Maven Central
- Main Class: `com.team.tetris.Main`

## 배포

실행 파일 패키징 및 배포 설정은 추후 추가합니다. 배포 단계에서는 JAR 및 `jpackage`를 이용한 OS별 실행 파일 생성을 구성할 예정입니다.