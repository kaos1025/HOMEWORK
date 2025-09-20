package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.IdempotencyKey;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.exception.DuplicateRequestException;
import kr.co._29cm.homework.exception.IdempotencyKeyExpiredException;
import kr.co._29cm.homework.repository.IdempotencyKeyRepository;
import kr.co._29cm.homework.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order(UUID.randomUUID().toString(), LocalDateTime.now());
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    @DisplayName("새로운 Idempotency-Key로 주문 처리 성공")
    @Transactional
    void 새로운_IdempotencyKey로_주문_처리_성공() {
        // given
        String idempotencyKey = "test-key-001";

        // when
        Order result = idempotencyService.processWithIdempotency(idempotencyKey, () -> testOrder);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testOrder.getId());

        IdempotencyKey savedKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey).orElseThrow();
        assertThat(savedKey.getKeyValue()).isEqualTo(idempotencyKey);
        assertThat(savedKey.getStatus()).isEqualTo(IdempotencyKey.Status.COMPLETED);
        assertThat(savedKey.getOrderId()).isNull(); // Order 엔티티가 저장된 후에 설정되어야 함
    }

    @Test
    @DisplayName("동일한 Idempotency-Key로 재요청 시 기존 주문 반환")
    @Transactional
    void 동일한_IdempotencyKey로_재요청_시_기존_주문_반환() {
        // given
        String idempotencyKey = "test-key-002";
        
        // 첫 번째 요청
        idempotencyService.processWithIdempotency(idempotencyKey, () -> testOrder);

        // when - 두 번째 요청
        Order result = idempotencyService.processWithIdempotency(idempotencyKey, () -> {
            fail("두 번째 요청에서는 주문 처리 함수가 호출되지 않아야 함");
            return testOrder;
        });

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testOrder.getId());
    }

    @Test
    @DisplayName("처리 중인 Idempotency-Key로 재요청 시 예외 발생")
    @Transactional
    void 처리_중인_IdempotencyKey로_재요청_시_예외_발생() {
        // given
        String idempotencyKey = "test-key-003";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        IdempotencyKey processingKey = new IdempotencyKey(idempotencyKey, null, expiresAt);
        idempotencyKeyRepository.save(processingKey);

        // when & then
        assertThatThrownBy(() -> 
                idempotencyService.processWithIdempotency(idempotencyKey, () -> testOrder))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("요청이 이미 처리 중입니다");
    }

    @Test
    @DisplayName("만료된 Idempotency-Key로 요청 시 예외 발생")
    @Transactional
    void 만료된_IdempotencyKey로_요청_시_예외_발생() {
        // given
        String idempotencyKey = "test-key-004";
        LocalDateTime expiredAt = LocalDateTime.now().minusHours(1); // 1시간 전 만료
        IdempotencyKey expiredKey = new IdempotencyKey(idempotencyKey, testOrder.getId(), expiredAt);
        idempotencyKeyRepository.save(expiredKey);

        // when & then
        assertThatThrownBy(() -> 
                idempotencyService.processWithIdempotency(idempotencyKey, () -> testOrder))
                .isInstanceOf(IdempotencyKeyExpiredException.class)
                .hasMessageContaining("Idempotency-Key가 만료되었습니다");
    }

    @Test
    @DisplayName("주문 처리 실패 시 Idempotency-Key 상태가 FAILED로 변경")
    @Transactional
    void 주문_처리_실패_시_IdempotencyKey_상태가_FAILED로_변경() {
        // given
        String idempotencyKey = "test-key-005";

        // when
        assertThatThrownBy(() -> 
                idempotencyService.processWithIdempotency(idempotencyKey, () -> {
                    throw new RuntimeException("주문 처리 실패");
                }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("주문 처리 실패");

        // then
        IdempotencyKey failedKey = idempotencyKeyRepository.findByKeyValue(idempotencyKey).orElseThrow();
        assertThat(failedKey.getStatus()).isEqualTo(IdempotencyKey.Status.FAILED);
    }
}
