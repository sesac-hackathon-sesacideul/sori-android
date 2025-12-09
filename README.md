



## 📖 프로젝트 개요 (Project Overview)

**Sori**는 청각 장애인 및 난청인이 일상생활에서 겪는 **소리 정보의 부재(위험 상황, 대화 맥락)** 를 AI 기술로 해결하여 안전하고 독립적인 삶을 지원하는 안드로이드 애플리케이션입니다.
> "소리를 보다, 마음을 듣다."

비언어적 소리(자동차 경적, 사이렌 등)를 시각적으로 인지할 수 있도록 돕고, 대화 중 상대방의 음성을 텍스트로 변환하는 것을 넘어 감정까지 분석하여 원활한 소통을 지원합니다.
**청각 장애인을 위한 실시간 대화 통역 및 환경 소리 시각화 비서**

### 🎯 개발 목표
*   **안전 사고 예방**: 위험 소리의 방향과 거리를 시각화하여 즉각적인 대처 가능
*   **소통 능력 향상**: 음성 인식(STT) 및 감정 분석을 통해 대화의 맥락까지 파악 지원

### 👥 타겟 사용자
*   청각 장애인 및 난청인
*   노인성 난청으로 인해 일상 소리 감지에 어려움을 겪는 분들

---

## 🚀 핵심 기능 (Key Features)

### 1. 🛡️ 환경 소리 감지 (Safety Monitoring)
주변의 소리를 실시간으로 분석하여 **위험 소리(사이렌, 경적 등)** 와 **일상 소리**를 구별합니다.
*   **위험도 분류**: 감지된 소리의 위험도에 따라 High, Medium, Low로 분류하여 사용자에게 알립니다.
*   **백그라운드 감지**: 앱이 백그라운드에 있어도 위험 소리가 감지되면 진동과 알림을 통해 즉시 경고합니다.

### 2. 📡 음원 위치 시각화 (Radar View)
소리가 발생한 **방향(Angle)** 과 **거리(Distance)** 를 직관적인 레이더 UI로 시각화합니다.
*   사용자는 화면을 통해 소리가 어디서 들려오는지 직관적으로 파악할 수 있습니다.
*   위험한 소리는 붉은색 파동으로 강조되어 표시됩니다.

### 3. 💬 실시간 음성 인식 (Voice Recognition)
상대방의 음성을 실시간으로 텍스트로 변환하여 채팅 말풍선 형태로 보여줍니다.
*   **STT (Speech-to-Text)** 기술을 활용하여 빠르고 정확하게 대화 내용을 자막으로 제공합니다.

### 4. 😊 감정 분석 (Emotion Analysis)
단순한 텍스트 변환을 넘어, 화자의 **음성 톤**과 **대화 내용**을 분석하여 감정을 파악합니다.
*   **GPT API**를 활용하여 긍정, 부정, 중립 등의 감정을 분석하고, 이를 이모티콘과 라벨로 표시합니다.
*   청각 장애인이 놓치기 쉬운 대화의 미묘한 분위기와 맥락을 이해하는 데 도움을 줍니다.

---

## 🛠️ 시스템 구성도 (System Architecture)

### 2.1 Input
*   **Audio Stream**: 스마트폰 마이크를 통한 실시간 소리 입력

### 2.2 Processing (AI Engine)
*   **Sound Analysis**:
    *   **소리 감지 및 분류 (SED)**: TensorFlow Lite (YAMNet) 모델을 사용하여 소리 이벤트를 분류합니다.
    *   **음원 위치 추정 (DOA)**: TDOA 및 Cross-Correlation 알고리즘을 활용하여 소리의 방향을 추정합니다.
*   **Speech Analysis**:
    *   **음성 인식 (STT)**: Android SpeechRecognizer 또는 Google STT API를 사용합니다.
    *   **감정 분석**: OpenAI GPT API를 활용하여 텍스트와 음성 특징을 기반으로 감정을 분석합니다.

### 2.3 Output (User Interface)
*   **Radar View**: 소리의 위치 시각화 (Canvas API 활용)
*   **Safety List**: 위험도에 따른 색상 구분 리스트 (LazyColumn)
*   **Chat View**: 대화 내용 및 감정 이모티콘 표시

---

## UI

### 기획서상 UI
<img width="1786" height="915" alt="UI-" src="https://github.com/user-attachments/assets/4463c7c5-44e6-4e92-a8e7-8347a0df9620" />

### 실제구현 UI
<img width="8234" height="2523" alt="Component 1" src="https://github.com/user-attachments/assets/7f69c0e7-57a4-420c-bf94-2a4a49a2673c" />

---

## 💻 기술 스택 (Tech Stack)

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **UI Framework** | Jetpack Compose (Material3), Canvas (Radar Visualization) |
| **AI / ML** | TensorFlow Lite (Audio Classification), OpenAI GPT API |
| **Concurrency** | Coroutines, Flow |
| **Network** | Retrofit2, OkHttp3 |

---

## 📥 설치 및 실행 방법 (Installation & Setup)

### 1. 프로젝트 클론 (Clone Repository)
```bash
git clone https://github.com/sesac-hackathon-sesacideul/sori-android
```

### 2. 프로젝트 열기 (Open Project)
*   Android Studio를 실행하고 `Open`을 선택하여 클론한 프로젝트 폴더를 엽니다.

### 3. API 키 설정 (API Key Setup)
*   `local.properties` 파일에 OpenAI API 키를 추가해야 감정 분석 기능을 사용할 수 있습니다.
```properties
OPENAI_API_KEY=sk-your-api-key-here
```

### 4. 빌드 및 실행 (Build & Run)
*   Android 기기를 연결하거나 에뮬레이터를 실행합니다.
*   Android Studio 상단의 `Run` 버튼(▶️)을 클릭하여 앱을 설치하고 실행합니다.
*   **권한 허용**: 앱 실행 시 마이크 사용 권한을 반드시 허용해야 합니다.

---

## 📱 사용 가이드 (Usage Guide)


### 메인 화면
*   앱 실행 시 **'환경 소리 모드'** 와 **'음성 인식 모드'** 중 원하는 기능을 선택할 수 있습니다.

### 환경 소리 모드 (Safety Mode)
1.  화면 중앙의 레이더를 통해 주변 소리의 위치를 확인합니다.
2.  하단 리스트에서 감지된 소리의 종류(예: 사이렌, 개 짖는 소리)와 위험도를 확인합니다.
3.  위험 소리가 감지되면 화면이 붉게 점멸하며 진동이 울립니다.

### 음성 인식 모드 (Communication Mode)
1.  마이크 버튼을 눌러 대화를 시작합니다.
2.  상대방의 말이 실시간으로 텍스트로 변환되어 화면에 표시됩니다.
3.  말풍선 위의 이모티콘을 통해 상대방의 현재 감정 상태를 파악합니다.

---

## 🔮 향후 발전 방향 (Future Work)

1. AI 웨어러블 기기 연동: 스마트워치와 연동하여 핸드폰을 보지 않아도 손목 진동으로
   위험 알림을 수신.
2. 사용자 맞춤형 소리 등록: 우리 집 초인종 소리, 반려견 소리 등 사용자만의 고유한
   소리를 직접 녹음하여 학습시키고 알림을 받는 기능.
3. 온디바이스 감정 분석: 현재 서버(GPT)를 거치는 감정 분석을 기기 내부의 경량 언어
   모델(SLM)로 대체하여, 데이터 통신 없이도 빠르고 안전하게 감정을 분석.
4. 멀티모달 상황: 마이크(청각)뿐만 아니라 카메라(시각) 정보를 결합하여, "사이렌
   소리"가 실제 구급차인지 TV 소리인지 구분하는 등 상황 인식 정확도 향상.
---
## 📄 라이선스 (License)
Solar Icons by 480 Design, licensed under CC BY 4.0
