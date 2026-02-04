# AI Log Panel

AI Log Panel은 Kotlin Multiplatform Compose 기반의 데스크탑(JVM) 서버 관리 및 로그 분석 도구입니다. SSH/로컬 서버를 등록하고, 실시간 로그 모니터링, 오류 탐지, AI 기반 로그 분석, 서버 성능 모니터링(CPU/RAM/네트워크) 등 다양한 기능을 제공합니다.

## 주요 기능
- **서버 관리**: SSH/로컬 서버 등록, 수정, 삭제, 서버별 실행/중단/명령어 전송
- **실시간 로그 패널**: 서버 로그 실시간 출력, 한글 폰트 지원, 경고/오류 색상 구분
- **AI 오류 분석**: Gemini API 연동, 오류 로그 AI 분석, Markdown 결과 렌더링, 추가 질문 기능
- **서버 성능 모니터링**: CPU/RAM/네트워크 사용량 실시간 그래프(로컬/원격)
- **UI/UX**: 블랙 글래스모피즘, 인텔리제이 스타일, 반응형 레이아웃, 다크모드
- **설정/저장**: 서버 정보 YAML 자동 저장(`%USERPROFILE%/.ai-log-panel/servers.yml`)

## 폴더 구조
```
AI-Log Panel/
├─ composeApp/
│  ├─ src/
│  │  ├─ commonMain/      # 공통 코드
│  │  ├─ jvmMain/         # 데스크탑(JVM) 전용 코드
│  │  │  ├─ kotlin/dev/skarch/ai_logpanel/
│  │  │  ├─ resources/fonts/D2Coding.ttf
│  │  └─ ...
│  ├─ build.gradle.kts    # 서브 프로젝트 빌드 스크립트
├─ build.gradle.kts       # 루트 빌드 스크립트
├─ gradle.properties
├─ gradlew, gradlew.bat   # Gradle Wrapper
├─ settings.gradle.kts
└─ README.md
```

## 빌드 및 실행 방법
### 1. 의존성 설치
- JDK 17 이상 필요
- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) 사용

### 2. 실행 명령어
- **Windows**
  ```powershell
  .\gradlew :composeApp:run
  ```
- **macOS/Linux**
  ```sh
  ./gradlew :composeApp:run
  ```

### 3. API 키 설정
- `composeApp/src/jvmMain/resources/local.properties` 파일에 아래와 같이 Gemini API 키를 추가하세요.
  ```properties
  GEMINI_API_KEY=your-gemini-api-key
  ```

## 주요 의존성
- [JetBrains Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [JSch](http://www.jcraft.com/jsch/) (SSH 클라이언트)
- [SnakeYAML](https://bitbucket.org/asomov/snakeyaml) (YAML 파싱)
- [Google Gemini API](https://ai.google.dev/)

## 서버 정보 저장 위치
- `%USERPROFILE%/.ai-log-panel/servers.yml` (자동 생성/저장)

## 라이선스
- Apache License 2.0

## 기여 방법
1. 이슈/PR 등록 전 [Discussions](https://github.com/SKARCH218/ai_logpanel/discussions)에서 먼저 문의해주세요.
2. 포크 후 브랜치 생성, 커밋, PR 제출
3. 코드 스타일 및 커밋 메시지 가이드 준수

---

자세한 내용 및 최신 정보는 [공식 저장소](https://github.com/SKARCH218/ai_logpanel)를 참고하세요.
