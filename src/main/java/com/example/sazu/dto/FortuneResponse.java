package com.example.sazu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FortuneResponse {

    private String uuid;
    private String issuedAt;
    private String name;
    private String birthDate;
    private String birthTime;
    private String gender;
    private String styleTheme;
    private String wishMessage;

    // SAZU API 사주팔자 4주 (시주·일주·월주·년주)
    private PillarInfo yearPillar;
    private PillarInfo monthPillar;
    private PillarInfo dayPillarInfo;
    private PillarInfo hourPillar;

    // 요약 일주 및 오행 정보
    private String dayPillar;
    private String fiveElement;
    private Map<String, Integer> elementCounts;

    // SAZU API 신강/신약 및 용신·성향 분석
    private String sinStrength;
    private int sinStrengthScore;
    private String sinStrengthAnalysis;

    // SAZU API 오늘의 일진(dailyInteraction) & 길신(angels)
    private String todayIlju;
    private String todaySipseong;
    private String todayTwelveStage;
    private String todayInteractionSummary;
    private List<String> angelStars;
    private List<String> dailyInsights;

    // SAZU API 연동 메타 정보
    private boolean sazuApiConnected;
    private String sazuApiTier;
    private String appliedSampleLabel;

    // 뽑은 카드 정보
    private int cardNumber;
    private String cardTitle;
    private String cardHanja;
    private String cardSymbol;
    private List<String> cardKeywords;

    // 운세 점수 및 카테고리별 운세
    private int overallScore;
    private int wealthScore;
    private String wealthLuck;
    private int loveScore;
    private String loveLuck;
    private int careerScore;
    private String careerLuck;
    private int healthScore;
    private String healthLuck;

    // 핵심 점사 및 행운 아이템
    private String summaryQuote;
    private String detailMessage;
    private String luckyColor;
    private String luckyNumber;
    private String luckyItem;
    private String luckyDirection;

    // 영수증/포토카드 시리얼 & QR 관련
    private String serialCode;
    private String qrDownloadUrl;
    private String lanIp;
    private boolean imageReady;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PillarInfo {
        private String label;        // 년주, 월주, 일주, 시주
        private String full;         // 예: 병인
        private String sky;          // 예: 병화
        private String earth;        // 예: 인목
        private String sipseong;     // 예: 비견 / 식신 / 편재
        private String twelveStage;  // 예: 장생 / 건록 / 제왕
        private String naeeum;       // 예: 노중화
    }
}
