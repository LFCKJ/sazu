# 🔮 데일리 운세 영수증 & QR 다운로드 서비스 (sazu)

본 프로젝트는 **카드를 뽑는 마이크로 인터랙션**과 **운세 영수증/부적 발급기**를 결합하고, 생성된 카드를 캡처하여 **QR코드를 통해 모바일로 즉시 다운로드(서버 업로드 방식)**하는 웹 서비스입니다.

---

## 1. 전체 데이터 및 처리 흐름 (Architecture Flow)

1. **운세 조회 (`POST /api/fortune`)**
   * 사용자가 생년월일시를 입력하고 카드를 뽑으면, 백엔드에서 오늘의 운세 결과(JSON)와 고유 식별자(`UUID`)를 반환합니다.
2. **화면 렌더링 & QR 생성**
   * 프론트엔드에서 영수증/부적 UI를 화면에 그리고, 다운로드 URL(`http://{서버IP 또는 도메인}/download/{UUID}`)이 담긴 QR코드를 카드 하단에 함께 렌더링합니다.
3. **이미지 변환 및 업로드 (`POST /api/fortune/{UUID}/image`)**
   * 렌더링이 완료된 카드 DOM 영역을 `html-to-image`로 캡처해 고해상도 `Blob(PNG)` 파일로 변환한 뒤, 백엔드로 백그라운드 업로드합니다.
4. **QR 스캔 및 다운로드 (`GET /api/fortune/{UUID}/download`)**
   * 사용자가 스마트폰으로 화면의 QR코드를 찍으면 모바일 웹페이지(또는 이미지 직링크)가 열리며 저장된 이미지를 즉시 확인하고 다운로드합니다.

---

## 2. 프로젝트 디렉토리 구조

```text
sazu/                     <-- 최상위 루트 폴더 (Spring Boot Backend - Gradle, JDK 17)
 ├── src/                 <-- 백엔드 소스 코드 및 설정 (application.yaml)
 ├── build.gradle         <-- 백엔드 Gradle 설정
 └── frontend/            <-- React 프로젝트 (Vite)
```

---

## 3. 실행 방법

### 백엔드 (Spring Boot)
```bash
./gradlew bootRun
```

### 프론트엔드 (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
