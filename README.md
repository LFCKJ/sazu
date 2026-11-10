# 🔮 SAZU · 데일리 사주 부적 포토카드 & 영수증 스튜디오

**SAZU 만세력·운세 API(`https://api.sazu.app`)**와 **12신수 수호 카드 뽑기 인터랙션**을 결합하여, 세상에 하나뿐인 **부적 포토카드 / 홀로그램 포토카드 / 운세 영수증**을 생성하고 **QR코드로 모바일 즉시 저장** 및 **프린터로 포토카드 규격 인쇄**까지 지원하는 웹 서비스입니다.

---

## ✨ 주요 기능

1. **SAZU API (`api.sazu.app`) 명리학 데이터 연동**
   - `POST /v2/sazu/today` 및 `POST /v2/sazu/manse` 연동을 통해 **사주 4주(시·일·월·년주)**, **신강/신약 지수**, **오늘의 일진 십성·12운성·신살·합충 관계**, **오행 분포**, **수호 길신(`천을귀인`, `문곡귀인` 등)** 분석
   - `GET /v2/sazu/samples` 공식 샘플 프로필 원클릭 불러오기 및 Free 샌드박스 자동 매핑(`nearestSample`) 지원
2. **12장 수호 카드 뽑기 마이크로 인터랙션**
   - 12신수 카드 셔플 애니메이션 및 3D 플립(Flip) 인터랙션
   - 3가지 카드 테마 실시간 전환: **🧧 황금 수호 부적카드** / **🔮 홀로그램 포토카드** / **🧾 만세력 운세 영수증**
3. **고해상도 이미지 자동 캡처 & QR코드 모바일 저장**
   - `html-to-image`(`pixelRatio: 3`)로 300DPI급 PNG 이미지를 캡처하여 백엔드(`POST /api/fortune/{uuid}/image`)에 자동 업로드
   - 스마트폰 카메라로 QR 스캔 시 모바일 전용 다운로드 뷰어(`/download/{uuid}`) 또는 즉시 파일 다운로드(`GET /api/fortune/{uuid}/download`) 제공
   - 로컬 와이파이 테스트를 위한 PC 내부 IPv4 주소 자동 감지 지원
4. **🖨️ 포토카드 & 영수증 프린터 출력 스튜디오**
   - **포토카드 정사이즈 (55 × 85 mm, 신용카드 규격)**
   - **4 × 6 인화지 포토카드 2매 모아찍기 (100 × 148 mm 용지에 55×85mm 2장)**
   - **4 × 6 포토프린터 꽉 찬 1매 (100 × 148 mm)**
   - **80mm 감열 영수증 프린터 전용 규격**
   - 재단선(가이드라인) 표시 및 포토카드 뒷면 아트워크 동시 인쇄 옵션 지원

---

## 🗂️ 프로젝트 구조

```text
sazu/
 ├── src/main/java/com/example/sazu/
 │    ├── config/WebConfig.java                  # CORS 설정
 │    ├── controller/FortuneController.java      # 운세 생성, 이미지 업로드/다운로드, 샘플/네트워크 API
 │    ├── dto/                                   # FortuneRequest, FortuneResponse
 │    ├── service/FortuneService.java            # SAZU API 연동 및 이미지/메타데이터 스토리지 관리
 │    └── scheduler/StorageCleanupScheduler.java # 24시간 경과 이미지 자동 삭제 스케줄러
 ├── src/main/resources/application.yaml         # 서버, Multipart, SAZU API 설정
 └── frontend/                                   # React + Vite 프론트엔드
      ├── public/card-back.jpeg                  # 포토카드 뒷면 아트워크
      └── src/App.jsx, App.css                   # 사주 입력, 카드 뽑기, QR 저장 & 프린터 스튜디오 UI
```

---

## 🚀 실행 방법

### 1. 백엔드 실행 (Spring Boot · Port 8080)
```powershell
.\gradlew.bat bootRun
```

### 2. 프론트엔드 실행 (React + Vite · Port 5173)
```powershell
cd frontend
npm install
npm run dev
```
