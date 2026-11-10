package com.example.sazu.controller;

import com.example.sazu.dto.FortuneRequest;
import com.example.sazu.dto.FortuneResponse;
import com.example.sazu.service.FortuneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/fortune")
@RequiredArgsConstructor
public class FortuneController {

    private final FortuneService fortuneService;

    /**
     * 0. SAZU API 공식 샘플 프로필 조회 (GET /api/fortune/samples)
     */
    @GetMapping("/samples")
    public ResponseEntity<List<Map<String, Object>>> getSazuSamples() {
        return ResponseEntity.ok(fortuneService.getSazuSamples());
    }

    /**
     * 1. 운세 조회 및 UUID 발급 (POST /api/fortune)
     */
    @PostMapping
    public ResponseEntity<FortuneResponse> createFortune(@Valid @RequestBody FortuneRequest request) {
        FortuneResponse response = fortuneService.createFortune(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 발급된 운세 메타데이터 조회 (GET /api/fortune/{uuid})
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<FortuneResponse> getFortune(@PathVariable String uuid) {
        return fortuneService.getFortune(uuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 3. 캡처된 카드 PNG 이미지 업로드 (POST /api/fortune/{uuid}/image)
     */
    @PostMapping(value = "/{uuid}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFortuneImage(
            @PathVariable String uuid,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        fortuneService.saveCardImage(uuid, file);
        return ResponseEntity.ok(Map.of(
                "uuid", uuid,
                "saved", true,
                "downloadUrl", "/api/fortune/" + uuid + "/download",
                "imageUrl", "/api/fortune/" + uuid + "/image"
        ));
    }

    /**
     * 4. 모바일 뷰어 및 인쇄 미리보기용 인라인 이미지 서빙 (GET /api/fortune/{uuid}/image)
     */
    @GetMapping("/{uuid}/image")
    public ResponseEntity<Resource> viewFortuneImage(@PathVariable String uuid) {
        try {
            Resource resource = fortuneService.loadCardImage(uuid);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 5. QR 스캔 후 이미지 파일 즉시 다운로드 (GET /api/fortune/{uuid}/download)
     */
    @GetMapping("/{uuid}/download")
    public ResponseEntity<Resource> downloadFortuneImage(@PathVariable String uuid) {
        try {
            Resource resource = fortuneService.loadCardImage(uuid);
            String filename = "sazu-photocard-" + uuid.substring(0, 8) + ".png";
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(resource);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 6. 로컬 와이파이 QR 테스트를 위한 PC LAN IP 조회 (GET /api/fortune/network-info)
     */
    @GetMapping("/network-info")
    public ResponseEntity<Map<String, String>> getNetworkInfo() {
        String lanIp = fortuneService.detectLanIp();
        return ResponseEntity.ok(Map.of("lanIp", lanIp));
    }
}
