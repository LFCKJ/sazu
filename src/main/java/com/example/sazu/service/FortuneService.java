package com.example.sazu.service;

import com.example.sazu.dto.FortuneRequest;
import com.example.sazu.dto.FortuneResponse;
import com.example.sazu.dto.FortuneResponse.PillarInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FortuneService {

    private final ObjectMapper objectMapper;

    @Value("${fortune.storage.upload-dir:./uploads/fortune-cards/}")
    private String uploadDir;

    @Value("${fortune.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${sazu.api.base-url:https://api.sazu.app}")
    private String sazuApiBaseUrl;

    @Value("${sazu.api.key:}")
    private String sazuApiKey;

    private RestClient restClient;
    private final Map<String, FortuneResponse> fortuneStore = new ConcurrentHashMap<>();

    private record CardArchetype(
            int number,
            String title,
            String hanja,
            String symbol,
            List<String> keywords,
            String defaultWish,
            String baseSummary,
            String luckyItem,
            String luckyDirection
    ) {}

    private static final List<CardArchetype> CARDS = List.of(
            new CardArchetype(1, "청룡의 비상", "靑龍飛上", "🐉",
                    List.of("새로운 시작", "승진·합격", "강력한 추진력"),
                    "말하는 대로 다 이뤄지는 청룡 대길 부적",
                    "구름을 뚫고 솟아오르는 청룡처럼 막혔던 흐름이 시원하게 트이는 날입니다.",
                    "푸른빛 노트 또는 만년필", "동쪽 (東)"),
            new CardArchetype(2, "주작의 축복", "朱雀祝福", "🦚",
                    List.of("매력 발산", "인기 상승", "기쁜 소식"),
                    "만나는 사람마다 나를 돕는 귀인 상봉 부적",
                    "붉은 봉황이 날개를 펼치니 가는 곳마다 사람들의 시선과 호감이 모입니다.",
                    "따뜻한 아메리카노 한 잔", "남쪽 (南)"),
            new CardArchetype(3, "백호의 결단", "白虎決斷", "🐅",
                    List.of("재물 획득", "과감한 선택", "위기 돌파"),
                    "재물과 행운이 넝쿨째 굴러오는 일확천금 부적",
                    "예리한 눈빛의 백호처럼 핵심을 꿰뚫어 보고 실속을 챙기는 날입니다.",
                    "메탈 시계 또는 은반지", "서쪽 (西)"),
            new CardArchetype(4, "현무의 지혜", "玄武智慧", "🐢",
                    List.of("내면의 평화", "깊은 통찰", "안정적 성장"),
                    "근심 걱정은 사라지고 평온만 가득한 수호 부적",
                    "깊은 물속의 현무처럼 차분함 속에서 가장 정확한 해답을 찾아냅니다.",
                    "다크 초콜릿 또는 텀블러", "북쪽 (北)"),
            new CardArchetype(5, "황룡의 등극", "黃龍登極", "👑",
                    List.of("최고의 행운", "중심 역할", "명예 상승"),
                    "말하는 대로 다 이뤄지는 비비디 바비디~부",
                    "하늘과 땅의 기운이 하나로 모여 당신을 가장 빛나는 자리로 이끕니다.",
                    "골드 포인트 액세서리", "중앙 (中)"),
            new CardArchetype(6, "일월성신", "日月星辰", "☀️",
                    List.of("명쾌한 해답", "운명적 만남", "완벽한 조화"),
                    "어둠은 걷히고 앞날이 환하게 빛나는 일월 부적",
                    "해와 달이 나란히 비추니 어둠이 걷히고 갈 길이 환하게 드러납니다.",
                    "작은 손거울 또는 안경클리너", "남동쪽 (南東)"),
            new CardArchetype(7, "북두칠성의 인도", "北斗七星", "✨",
                    List.of("길잡이", "영감 폭발", "소원 성취"),
                    "간절히 바라는 소원이 반드시 성취되는 칠성 부적",
                    "밤하늘의 일곱 별이 당신의 나침반이 되어 행운의 지점으로 안내합니다.",
                    "별 모양 키링 또는 이어폰", "북동쪽 (北東)"),
            new CardArchetype(8, "삼족오의 열정", "三足烏", "🔥",
                    List.of("에너지 충전", "도전 성공", "강한 생명력"),
                    "어떤 장애물도 가볍게 뛰어넘는 필승 합격 부적",
                    "태양을 품은 삼족오의 기운으로 어떤 장애물도 가볍게 뛰어넘습니다.",
                    "비타민 음료 또는 립밤", "남서쪽 (南西)"),
            new CardArchetype(9, "해태의 수호", "獬豸守護", "🦁",
                    List.of("액운 차단", "정의로운 해결", "든든한 방패"),
                    "나쁜 액운은 물러가고 좋은 일만 생기는 벽사 부적",
                    "신비로운 영수 해태가 나쁜 기운을 막아주고 좋은 인연만 들입니다.",
                    "카드지갑 또는 행운의 동전", "서북쪽 (西北)"),
            new CardArchetype(10, "봉황의 가락", "鳳凰之曲", "🪶",
                    List.of("예술적 감각", "설레는 인연", "풍요로운 여유"),
                    "사랑과 설렘이 꽃피고 인연이 닿는 연분 부적",
                    "아름다운 선율이 퍼지듯 마음의 여유와 낭만이 가득 채워집니다.",
                    "향수 또는 핸드크림", "동남쪽 (東南)"),
            new CardArchetype(11, "기린의 발걸음", "麒麟之步", "🦌",
                    List.of("귀인의 등장", "뜻밖의 횡재", "선한 영향력"),
                    "뜻밖의 행운과 기분 좋은 보너스가 찾아오는 횡재 부적",
                    "성인이 나타날 때 모습을 드러내는 기린처럼 귀한 인연과 기회가 찾아옵니다.",
                    "네잎클로버 책갈피 또는 에코백", "동쪽 (東)"),
            new CardArchetype(12, "천호의 신비", "天狐神祕", "🦊",
                    List.of("직관력 상승", "반전의 행운", "특별한 매력"),
                    "위기를 기회로 바꾸는 일발역전 만사형통 부적",
                    "천년의 지혜를 품은 천호의 기운으로 상황을 단숨에 역전시킵니다.",
                    "실버 링 또는 향초", "남쪽 (南)")
    );

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(sazuApiBaseUrl)
                .defaultHeader("x-api-key", sazuApiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            log.info("운세 이미지 저장 디렉토리 준비 완료: {}", dir);
        } catch (IOException e) {
            log.error("운세 이미지 저장 디렉토리 생성 실패", e);
        }
    }

    /**
     * SAZU API 공식 샘플 프로필 목록 조회 (GET /v2/sazu/samples)
     */
    public List<Map<String, Object>> getSazuSamples() {
        try {
            String raw = restClient.get()
                    .uri("/v2/sazu/samples")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode samplesNode = root.path("data").path("samples");
            if (samplesNode.isArray()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (JsonNode s : samplesNode) {
                    result.add(objectMapper.convertValue(s, Map.class));
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("SAZU API 샘플 목록 조회 실패", e);
        }
        return List.of();
    }

    /**
     * SAZU API (https://api.sazu.app)를 호출하여 만세력(/v2/sazu/manse) 및 오늘의 운세(/v2/sazu/today) 데이터를 결합해 반환합니다.
     */
    public FortuneResponse createFortune(FortuneRequest request) {
        String uuid = UUID.randomUUID().toString();
        String name = (request.getName() == null || request.getName().isBlank())
                ? "행운의 주인공"
                : request.getName().trim();
        String birthDateStr = request.getBirthDate().trim();
        String birthTimeStr = (request.getBirthTime() == null || request.getBirthTime().isBlank())
                ? "10:00 (사시)"
                : request.getBirthTime().trim();
        String styleTheme = (request.getStyleTheme() == null || request.getStyleTheme().isBlank())
                ? "photocard"
                : request.getStyleTheme().trim();

        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(birthDateStr);
        } catch (Exception e) {
            birthDate = LocalDate.of(1998, 5, 19);
        }

        Integer birthHour = request.getBirthHour();
        if (birthHour == null && !birthTimeStr.contains("모름") && !birthTimeStr.contains("미상")) {
            birthHour = parseBirthHour(birthTimeStr);
        }

        boolean isFemale = "여성".equals(request.getGender()) || "female".equalsIgnoreCase(request.getGender());
        boolean isLunar = Boolean.TRUE.equals(request.getIsLunar());

        int cardIdx = Math.min(12, Math.max(1, request.getCardNumber())) - 1;
        CardArchetype card = CARDS.get(cardIdx);

        String wishMessage = (request.getWishMessage() == null || request.getWishMessage().isBlank())
                ? card.defaultWish()
                : request.getWishMessage().trim();

        // 1. SAZU API 요청 페이로드 구성
        Map<String, Object> apiPayload = new LinkedHashMap<>();
        apiPayload.put("birthYear", birthDate.getYear());
        apiPayload.put("birthMonth", birthDate.getMonthValue());
        apiPayload.put("birthDay", birthDate.getDayOfMonth());
        apiPayload.put("birthHour", birthHour);
        apiPayload.put("birthMinute", 0);
        apiPayload.put("isFemale", isFemale);
        apiPayload.put("birthCity", "서울");
        apiPayload.put("isLunar", isLunar);

        // 2. SAZU API 호출 (/v2/sazu/today + /v2/sazu/manse)
        SazuApiResult sazuResult = fetchFromSazuApi(apiPayload);

        // 3. SAZU API 응답 파싱하여 FortuneResponse 구성
        FortuneResponse response = buildFortuneResponseFromSazu(
                uuid,
                name,
                birthDateStr,
                birthTimeStr,
                request.getGender(),
                styleTheme,
                wishMessage,
                card,
                sazuResult
        );

        fortuneStore.put(uuid, response);
        saveMetadataToDisk(response);
        return response;
    }

    private record SazuApiResult(
            boolean connected,
            String tier,
            String appliedSampleLabel,
            JsonNode todayNode,
            JsonNode manseNode
    ) {}

    private SazuApiResult fetchFromSazuApi(Map<String, Object> initialPayload) {
        Map<String, Object> payloadToUse = initialPayload;
        String appliedSampleLabel = null;

        try {
            String todayRaw;
            try {
                todayRaw = restClient.post()
                        .uri("/v2/sazu/today")
                        .body(payloadToUse)
                        .retrieve()
                        .body(String.class);
            } catch (HttpClientErrorException.BadRequest badReq) {
                // Free 샌드박스 플랜에서 샘플 외 생년월일 입력 시 nearestSample로 자동 폴백 재호출
                JsonNode errNode = objectMapper.readTree(badReq.getResponseBodyAsString());
                JsonNode nearest = errNode.path("error").path("details").path("nearestSample");
                if (!nearest.isMissingNode() && nearest.has("input")) {
                    appliedSampleLabel = nearest.path("label").asText("SAZU Free 샌드박스 샘플");
                    payloadToUse = objectMapper.convertValue(nearest.get("input"), Map.class);
                    log.info("SAZU Free 플랜 샌드박스 자동 매핑 적용 ({}): {}", appliedSampleLabel, payloadToUse);
                    todayRaw = restClient.post()
                            .uri("/v2/sazu/today")
                            .body(payloadToUse)
                            .retrieve()
                            .body(String.class);
                } else {
                    throw badReq;
                }
            }

            String manseRaw = restClient.post()
                    .uri("/v2/sazu/manse")
                    .body(payloadToUse)
                    .retrieve()
                    .body(String.class);

            JsonNode todayRoot = objectMapper.readTree(todayRaw);
            JsonNode manseRoot = objectMapper.readTree(manseRaw);
            String tier = todayRoot.path("meta").path("tier").asText("free");
            if (appliedSampleLabel == null && todayRoot.path("meta").path("sample").asBoolean(false)) {
                appliedSampleLabel = todayRoot.path("meta").path("sampleProfile").asText("official-sample");
            }

            return new SazuApiResult(true, tier, appliedSampleLabel, todayRoot, manseRoot);
        } catch (Exception e) {
            log.error("SAZU 외부 API 호출 중 오류 발생", e);
            return new SazuApiResult(false, "offline", null, null, null);
        }
    }

    private FortuneResponse buildFortuneResponseFromSazu(
            String uuid,
            String name,
            String birthDateStr,
            String birthTimeStr,
            String gender,
            String styleTheme,
            String wishMessage,
            CardArchetype card,
            SazuApiResult sazuResult
    ) {
        PillarInfo yearPillar = null;
        PillarInfo monthPillar = null;
        PillarInfo dayPillarInfo = null;
        PillarInfo hourPillar = null;

        String dayPillarSummary = "병인(丙寅) · 노중화";
        String fiveElementSummary = "화(火) 중심 · 목(木) 상생";
        Map<String, Integer> elementCounts = new LinkedHashMap<>();

        String sinStrength = "신강";
        int sinStrengthScore = 80;
        String sinStrengthAnalysis = card.baseSummary();

        String todayIlju = "경인(庚寅)";
        String todaySipseong = "편재 · 편인";
        String todayTwelveStage = "장생(長生)";
        String todayInteractionSummary = "장생 · 귀인 상생";
        List<String> angelStars = new ArrayList<>();
        List<String> dailyInsights = new ArrayList<>();

        if (sazuResult.connected() && sazuResult.todayNode() != null && sazuResult.manseNode() != null) {
            JsonNode todayModules = sazuResult.todayNode().path("data").path("modules");
            JsonNode manseModules = sazuResult.manseNode().path("data").path("modules");

            // 1. 사주 4주(년·월·일·시) 파싱
            JsonNode fp = todayModules.path("fourPillars");
            yearPillar = parsePillar("년주", fp.path("year"));
            monthPillar = parsePillar("월주", fp.path("month"));
            dayPillarInfo = parsePillar("일주", fp.path("day"));
            hourPillar = parsePillar("시주", fp.path("hour"));

            if (dayPillarInfo != null) {
                dayPillarSummary = dayPillarInfo.getFull() + " (" + dayPillarInfo.getSky() + "·" + dayPillarInfo.getEarth() + ") · " + dayPillarInfo.getNaeeum();
            }

            // 2. 오행 분포(elements) 파싱
            JsonNode elementsNode = manseModules.path("elements");
            if (!elementsNode.isMissingNode()) {
                elementCounts.put("목(木)", elementsNode.path("wood").path("total").path("count").asInt(0));
                elementCounts.put("화(火)", elementsNode.path("fire").path("total").path("count").asInt(0));
                elementCounts.put("토(土)", elementsNode.path("earth").path("total").path("count").asInt(0));
                elementCounts.put("금(金)", elementsNode.path("metal").path("total").path("count").asInt(0));
                elementCounts.put("수(水)", elementsNode.path("water").path("total").path("count").asInt(0));
            }

            // 3. 신강/신약(sinStrength) 파싱
            JsonNode sinNode = manseModules.path("sinStrength");
            if (!sinNode.isMissingNode()) {
                sinStrength = sinNode.path("strength").asText("중화");
                sinStrengthScore = sinNode.path("score").asInt(78);
                sinStrengthAnalysis = sinNode.path("analysis").asText(card.baseSummary());
            }

            // 4. 오늘의 일진 상호작용(dailyInteraction) 파싱
            JsonNode dailyNode = todayModules.path("dailyInteraction");
            if (!dailyNode.isMissingNode()) {
                String ganji = dailyNode.path("ilju").path("ganji").asText("경인");
                String stemEl = dailyNode.path("ilju").path("stemElement").asText("금");
                String branchEl = dailyNode.path("ilju").path("branchElement").asText("목");
                todayIlju = ganji + " (" + stemEl + "·" + branchEl + ")";

                String stemSip = dailyNode.path("toDayMaster").path("stemSipseong").asText("편재");
                String branchSip = dailyNode.path("toDayMaster").path("branchSipseong").asText("편인");
                todaySipseong = stemSip + " · " + branchSip;
                todayTwelveStage = dailyNode.path("toDayMaster").path("twelveStage").asText("장생");
                todayInteractionSummary = dailyNode.path("summary").asText("");

                // 오늘 보충되는 오행
                List<String> gained = new ArrayList<>();
                for (JsonNode g : dailyNode.path("elementShift").path("gained")) {
                    gained.add(g.asText());
                }
                if (!gained.isEmpty()) {
                    fiveElementSummary = "보충 오행: " + String.join(", ", gained) + " (" + sinStrength + " " + sinStrengthScore + "점)";
                } else {
                    fiveElementSummary = sinStrength + " (" + sinStrengthScore + "점) · " + todayTwelveStage;
                }

                // 오늘의 신살 및 합충 관계 해설 수집
                for (JsonNode s : dailyNode.path("sinsal")) {
                    dailyInsights.add("[" + s.path("name").asText() + "] " + s.path("meaning").asText());
                }
                for (JsonNode rel : dailyNode.path("relations")) {
                    String relDesc = String.format("[%s-%s %s] %s",
                            rel.path("sourcePosition").asText(),
                            rel.path("targetPosition").asText(),
                            rel.path("type").asText(),
                            rel.path("meaning").asText());
                    if (dailyInsights.size() < 4) {
                        dailyInsights.add(relDesc);
                    }
                }
            }

            // 5. 사주 원국 길신(angels) 파싱
            JsonNode angelsNode = todayModules.path("sinsal").path("angels");
            if (angelsNode.isArray()) {
                for (JsonNode a : angelsNode) {
                    String starName = a.path("name").asText();
                    if (!starName.isBlank() && !angelStars.contains(starName)) {
                        angelStars.add(starName);
                    }
                }
            }
        }

        if (angelStars.isEmpty()) {
            angelStars.addAll(List.of("천을귀인", "문곡귀인", "건록"));
        }

        // SAZU API 명리 데이터 + 뽑은 수호 카드 시드로 카테고리별 점수 산출
        int seed = Objects.hash(birthDateStr, card.number(), todayIlju, sinStrengthScore);
        int baseScore = Math.min(96, Math.max(78, sinStrengthScore + (card.number() % 7)));
        int wealthScore = Math.min(99, Math.max(76, baseScore + Math.floorMod(seed, 11) - 3));
        int loveScore = Math.min(99, Math.max(75, baseScore + Math.floorMod(seed >> 2, 13) - 4));
        int careerScore = Math.min(99, Math.max(78, baseScore + Math.floorMod(seed >> 4, 11) - 2));
        int healthScore = Math.min(99, Math.max(76, baseScore + Math.floorMod(seed >> 6, 11) - 3));
        int overallScore = Math.round((wealthScore + loveScore + careerScore + healthScore) / 4.0f);

        String dayKeyword = (dayPillarInfo != null && dayPillarInfo.getTwelveStage() != null)
                ? dayPillarInfo.getTwelveStage()
                : todayTwelveStage;

        String summaryQuote = String.format("오늘 일진 [%s · %s]의 기운과 [%s] 카드가 만나 %s의 흐름을 엽니다.",
                todayIlju, todaySipseong, card.title(), dayKeyword);

        String detailMessage = sinStrengthAnalysis;
        if (!dailyInsights.isEmpty()) {
            detailMessage = dailyInsights.get(0) + " " + sinStrengthAnalysis;
        }

        String[] luckyColors = {"황금빛 앰버 (Gold)", "주홍빛 코랄 (Coral Red)", "비취 에메랄드 (Jade)", "청아한 로얄블루 (Blue)", "백옥 아이보리 (Ivory)"};
        String luckyColor = luckyColors[Math.floorMod(card.number() + sinStrengthScore, luckyColors.length)];
        String luckyNumber = String.format("%d, %d", card.number(), (sinStrengthScore % 30) + 7);

        String serialCode = String.format("SAZU-%s-%02d-%s",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")),
                card.number(),
                uuid.substring(0, 6).toUpperCase());

        String lanIp = detectLanIp();
        String qrBase = resolveQrBaseUrl(lanIp);

        return FortuneResponse.builder()
                .uuid(uuid)
                .issuedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")))
                .name(name)
                .birthDate(birthDateStr)
                .birthTime(birthTimeStr)
                .gender(gender)
                .styleTheme(styleTheme)
                .wishMessage(wishMessage)
                .yearPillar(yearPillar)
                .monthPillar(monthPillar)
                .dayPillarInfo(dayPillarInfo)
                .hourPillar(hourPillar)
                .dayPillar(dayPillarSummary)
                .fiveElement(fiveElementSummary)
                .elementCounts(elementCounts)
                .sinStrength(sinStrength)
                .sinStrengthScore(sinStrengthScore)
                .sinStrengthAnalysis(sinStrengthAnalysis)
                .todayIlju(todayIlju)
                .todaySipseong(todaySipseong)
                .todayTwelveStage(todayTwelveStage)
                .todayInteractionSummary(todayInteractionSummary)
                .angelStars(angelStars)
                .dailyInsights(dailyInsights)
                .sazuApiConnected(sazuResult.connected())
                .sazuApiTier(sazuResult.tier())
                .appliedSampleLabel(sazuResult.appliedSampleLabel())
                .cardNumber(card.number())
                .cardTitle(card.title())
                .cardHanja(card.hanja())
                .cardSymbol(card.symbol())
                .cardKeywords(card.keywords())
                .overallScore(overallScore)
                .wealthScore(wealthScore)
                .wealthLuck("일진 십성 [" + todaySipseong + "] 작용으로 실속 있는 재물 흐름")
                .loveScore(loveScore)
                .loveLuck("귀인성 [" + String.join("·", angelStars.subList(0, Math.min(2, angelStars.size()))) + "]의 도움으로 호감 상승")
                .careerScore(careerScore)
                .careerLuck("12운성 [" + todayTwelveStage + "] 기운으로 추진력과 성취도 상승")
                .healthScore(healthScore)
                .healthLuck(fiveElementSummary + " 균형 유지")
                .summaryQuote(summaryQuote)
                .detailMessage(detailMessage)
                .luckyColor(luckyColor)
                .luckyNumber(luckyNumber)
                .luckyItem(card.luckyItem())
                .luckyDirection(card.luckyDirection())
                .serialCode(serialCode)
                .qrDownloadUrl(qrBase + "/download/" + uuid)
                .lanIp(lanIp)
                .imageReady(false)
                .build();
    }

    private PillarInfo parsePillar(String label, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return PillarInfo.builder()
                    .label(label)
                    .full("미상")
                    .sky("-")
                    .earth("-")
                    .sipseong("-")
                    .twelveStage("-")
                    .naeeum("-")
                    .build();
        }
        return PillarInfo.builder()
                .label(label)
                .full(node.path("full").asText("-"))
                .sky(node.path("skyFull").asText("-"))
                .earth(node.path("earthFull").asText("-"))
                .sipseong(node.path("sippiSeong").asText("-"))
                .twelveStage(node.path("twelveStage").asText("-"))
                .naeeum(node.path("naeeum").asText("-"))
                .build();
    }

    private Integer parseBirthHour(String birthTimeStr) {
        if (birthTimeStr.contains("자시")) return 0;
        if (birthTimeStr.contains("축시")) return 2;
        if (birthTimeStr.contains("인시")) return 4;
        if (birthTimeStr.contains("묘시")) return 6;
        if (birthTimeStr.contains("진시")) return 8;
        if (birthTimeStr.contains("사시")) return 10;
        if (birthTimeStr.contains("오시")) return 12;
        if (birthTimeStr.contains("미시")) return 14;
        if (birthTimeStr.contains("신시")) return 16;
        if (birthTimeStr.contains("유시")) return 18;
        if (birthTimeStr.contains("술시")) return 20;
        if (birthTimeStr.contains("해시")) return 22;
        return 10;
    }

    public Optional<FortuneResponse> getFortune(String uuid) {
        validateUuid(uuid);
        FortuneResponse cached = fortuneStore.get(uuid);
        if (cached == null) {
            cached = loadMetadataFromDisk(uuid);
            if (cached != null) {
                fortuneStore.put(uuid, cached);
            }
        }
        if (cached != null) {
            cached.setImageReady(Files.exists(getImagePath(uuid)));
        }
        return Optional.ofNullable(cached);
    }

    public void saveCardImage(String uuid, MultipartFile file) throws IOException {
        validateUuid(uuid);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 이미지 파일이 비어 있습니다.");
        }
        Path targetPath = getImagePath(uuid);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        FortuneResponse cached = fortuneStore.get(uuid);
        if (cached != null) {
            cached.setImageReady(true);
            saveMetadataToDisk(cached);
        }
        log.info("운세 카드 이미지 저장 완료: {}", targetPath);
    }

    public Resource loadCardImage(String uuid) {
        validateUuid(uuid);
        Path imagePath = getImagePath(uuid);
        if (!Files.exists(imagePath)) {
            throw new NoSuchElementException("해당 UUID의 운세 이미지를 찾을 수 없습니다: " + uuid);
        }
        return new FileSystemResource(imagePath);
    }

    public String detectLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallbackIp = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                String displayName = iface.getDisplayName().toLowerCase();
                if (displayName.contains("virtual") || displayName.contains("wsl") || displayName.contains("hyper-v") || displayName.contains("docker")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) {
                            return ip;
                        }
                        if (fallbackIp == null && (ip.startsWith("10.") || ip.startsWith("172."))) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
            if (fallbackIp != null) {
                return fallbackIp;
            }
        } catch (Exception e) {
            log.debug("LAN IP 감지 중 예외 발생", e);
        }
        return "localhost";
    }

    private String resolveQrBaseUrl(String lanIp) {
        if (baseUrl != null && baseUrl.contains("localhost") && !"localhost".equals(lanIp)) {
            return baseUrl.replace("localhost", lanIp);
        }
        return baseUrl;
    }

    private Path getImagePath(String uuid) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(uuid + ".png");
    }

    private Path getMetaPath(String uuid) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(uuid + ".json");
    }

    private void saveMetadataToDisk(FortuneResponse response) {
        try {
            Path metaPath = getMetaPath(response.getUuid());
            Files.createDirectories(metaPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaPath.toFile(), response);
        } catch (IOException e) {
            log.warn("운세 메타데이터 파일 저장 실패: {}", response.getUuid(), e);
        }
    }

    private FortuneResponse loadMetadataFromDisk(String uuid) {
        try {
            Path metaPath = getMetaPath(uuid);
            if (Files.exists(metaPath)) {
                return objectMapper.readValue(metaPath.toFile(), FortuneResponse.class);
            }
        } catch (IOException e) {
            log.warn("운세 메타데이터 파일 로드 실패: {}", uuid, e);
        }
        return null;
    }

    private void validateUuid(String uuid) {
        if (uuid == null || !uuid.matches("^[a-fA-F0-9\\-]{8,64}$")) {
            throw new IllegalArgumentException("유효하지 않은 UUID 형식입니다.");
        }
    }
}
