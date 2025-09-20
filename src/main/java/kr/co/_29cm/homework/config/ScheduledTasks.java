package kr.co._29cm.homework.config;

import kr.co._29cm.homework.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final IdempotencyService idempotencyService;

    /**
     * 매일 자정에 만료된 Idempotency-Key 정리
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredIdempotencyKeys() {
        log.info("만료된 Idempotency-Key 정리 작업 시작");
        try {
            idempotencyService.cleanupExpiredKeys();
            log.info("만료된 Idempotency-Key 정리 작업 완료");
        } catch (Exception e) {
            log.error("만료된 Idempotency-Key 정리 작업 실패", e);
        }
    }
}
