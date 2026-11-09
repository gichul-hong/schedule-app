# ⚡ 전체 빌드 작업 절차 요약 (Quick Build Steps)

Android Studio 전체를 설치하지 않고도 터미널(CLI)에서 **가장 빠르게 환경 세팅부터 APK 빌드 및 테스트까지 완료하는 핵심 절차**를 요약한 문서입니다.

---

## 1단계: 필수 도구 설치 (최초 1회)

macOS (Homebrew 기준) 터미널에서 아래 명령어를 순서대로 실행합니다.

```bash
# 1) JDK 17 또는 21 확인 (미설치 시: brew install --cask temurin)
java -version

# 2) 경량 Android SDK Command Line Tools 설치
brew install --cask android-commandlinetools

# 3) Android SDK 라이선스 동의 및 Android 35 플랫폼/빌드툴 설치
yes | sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools --licenses
sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "platform-tools"
```

---

## 2단계: 프로젝트 클론 및 SDK 경로 지정

```bash
# 1) 저장소 클론 및 폴더 이동
git clone https://github.com/gichul-hong/schedule-app.git
cd schedule-app

# 2) local.properties에 Android SDK 경로 등록 (gitignore 대상이므로 각 PC에서 1회 생성)
echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties

# (참고) Android Studio 기본 SDK를 쓰는 경우:
# echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

---

## 3단계: 테스트 실행 및 APK 빌드

```bash
# 1) 전체 단위 테스트 및 4단계 E2E 테스트 실행
./gradlew :composeApp:testDebugUnitTest

# 2) 안드로이드 디버그 APK 빌드
./gradlew :composeApp:assembleDebug
```

* **생성된 APK 경로**:  
  `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
* **스마트폰에 바로 설치 (USB 연결 시)**:
  ```bash
  ./gradlew :composeApp:installDebug
  ```

---

## 4단계: iOS 빌드 (Xcode 설치된 Mac인 경우)

```bash
# iOS 시뮬레이터(Apple Silicon) 프레임워크 빌드
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# 실제 아이폰(iOS 기기) 프레임워크 빌드
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

> [!TIP]
> 상세한 스토어 배포(Google Play / App Store / TestFlight) 및 GitHub Actions 자동화 가이드는 [`docs/build_and_deployment_guide.md`](file:///Volumes/hynix-2t/dev/schedule-app/docs/build_and_deployment_guide.md)를 참고하세요.
