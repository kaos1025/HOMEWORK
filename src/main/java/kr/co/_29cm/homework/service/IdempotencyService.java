package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.IdempotencyKey;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.exception.DuplicateRequestException;
import kr.co._29cm.homework.exception.IdempotencyKeyExpiredException;
import kr.co._29cm.homework.repository.IdempotencyKeyRepository;
import kr.co._29cm.homework.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OrderRepository orderRepository;

    private static final int IDEMPOTENCY_EXPIRY_HOURS = 24;

    @Transactional
    public Order processWithIdempotency(String idempotencyKey, OrderProcessingFunction orderFunction) {
        log.info("Idempotency 처리 시작: key={}", idempotencyKey);

        // 1. 기존 키 조회
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey);
        
        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            
            // 만료된 키 처리
            if (key.isExpired()) {
                log.warn("만료된 Idempotency 키: {}", idempotencyKey);
                throw new IdempotencyKeyExpiredException(idempotencyKey);
            }
            
            // 처리 중인 요청
            if (key.isProcessing()) {
                log.warn("이미 처리 중인 Idempotency 키: {}", idempotencyKey);
                throw new DuplicateRequestException(idempotencyKey, "요청이 이미 처리 중입니다");
            }
            
            // 완료된 요청 - 기존 주문 반환
            if (key.isCompleted() && key.getOrderId() != null) {
                log.info("기존 주문 반환: orderId={}", key.getOrderId());
                return orderRepository.findById(key.getOrderId())
                        .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + key.getOrderId()));
            }
        }

        // 2. 새로운 키 생성
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(IDEMPOTENCY_EXPIRY_HOURS);
        IdempotencyKey newKey = new IdempotencyKey(idempotencyKey, null, expiresAt);
        idempotencyKeyRepository.save(newKey);

        try {
            // 3. 주문 처리
            Order order = orderFunction.process();
            
            // 4. 키에 주문 ID 연결 및 완료 처리
            newKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey).orElseThrow();
            newKey.markCompleted();
            // 주문 ID는 Order 엔티티가 저장된 후에 설정해야 함
            idempotencyKeyRepository.save(newKey);
            
            log.info("Idempotency 처리 완료: key={}, orderId={}", idempotencyKey, order.getId());
            return order;
            
        } catch (Exception e) {
            // 5. 실패 시 키 상태 업데이트
            try {
                newKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey).orElseThrow();
                newKey.markFailed();
                idempotencyKeyRepository.save(newKey);
            } catch (Exception ex) {
                log.error("Idempotency 키 상태 업데이트 실패: {}", ex.getMessage());
            }
            
            log.error("Idempotency 처리 실패: key={}", idempotencyKey, e);
            throw e;
        }
    }

    @Transactional
    public void cleanupExpiredKeys() {
        log.info("만료된 Idempotency 키 정리 시작");
        idempotencyKeyRepository.deleteExpiredKeys(LocalDateTime.now());
        log.info("만료된 Idempotency 키 정리 완료");
    }

    @FunctionalInterface
    public interface OrderProcessingFunction {
        Order process();
    }
}
