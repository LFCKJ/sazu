package com.example.sazu.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
@Component
public class StorageCleanupScheduler {

    @Value("${fortune.storage.upload-dir:./uploads/fortune-cards/}")
    private String uploadDir;

    /**
     * 매일 자정 및 매시간 실행되어 생성된 지 24시간이 지난 일회성 운세 이미지/메타데이터 파일을 삭제합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredFiles() {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            return;
        }

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int deletedCount = 0;

        try (Stream<Path> paths = Files.list(dir)) {
            for (Path file : paths.toList()) {
                if (Files.isRegularFile(file)) {
                    FileTime lastModified = Files.getLastModifiedTime(file);
                    if (lastModified.toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        deletedCount++;
                    }
                }
            }
            if (deletedCount > 0) {
                log.info("만료된 운세 파일 {}건을 정리했습니다.", deletedCount);
            }
        } catch (IOException e) {
            log.error("운세 파일 정리 중 오류 발생", e);
        }
    }
}
