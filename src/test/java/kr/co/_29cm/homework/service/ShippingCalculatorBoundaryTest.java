package kr.co._29cm.homework.service;

import kr.co._29cm.homework.config.ShippingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "shipping.policy.free-shipping-threshold=50000",
        "shipping.policy.fee=2500"
})
class ShippingCalculatorBoundaryTest {

    private ShippingCalculator shippingCalculator;

    @BeforeEach
    void setUp() {
        ShippingProperties shippingProperties = new ShippingProperties();
        ShippingProperties.Policy policy = new ShippingProperties.Policy();
        policy.setFreeShippingThreshold(new BigDecimal("50000"));
        policy.setFee(new BigDecimal("2500"));
        shippingProperties.setPolicy(policy);
        
        shippingCalculator = new ShippingCalculator(shippingProperties);
    }

    @Test
    @DisplayName("배송비 경계값 테스트: 49,999원일 때 2,500원 배송비 부과")
    void testShippingFeeBoundary_49999_ShouldApplyFee() {
        // Given
        BigDecimal totalAmount = new BigDecimal("49999");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then
        BigDecimal expectedTotal = new BigDecimal("52499"); // 49999 + 2500
        assertThat(totalPayment).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("배송비 경계값 테스트: 50,000원일 때 0원 배송비 (무료배송)")
    void testShippingFeeBoundary_50000_ShouldBeFree() {
        // Given
        BigDecimal totalAmount = new BigDecimal("50000");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then
        assertThat(totalPayment).isEqualByComparingTo(totalAmount); // 배송비 0원
    }

    @Test
    @DisplayName("배송비 경계값 테스트: 50,001원일 때 0원 배송비 (무료배송)")
    void testShippingFeeBoundary_50001_ShouldBeFree() {
        // Given
        BigDecimal totalAmount = new BigDecimal("50001");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then
        assertThat(totalPayment).isEqualByComparingTo(totalAmount); // 배송비 0원
    }

    @Test
    @DisplayName("배송비 경계값 테스트: 0원일 때 2,500원 배송비 부과")
    void testShippingFeeBoundary_0_ShouldApplyFee() {
        // Given
        BigDecimal totalAmount = BigDecimal.ZERO;

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then
        BigDecimal expectedTotal = new BigDecimal("2500"); // 0 + 2500
        assertThat(totalPayment).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("배송비 경계값 테스트: 1원일 때 2,500원 배송비 부과")
    void testShippingFeeBoundary_1_ShouldApplyFee() {
        // Given
        BigDecimal totalAmount = new BigDecimal("1");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then
        BigDecimal expectedTotal = new BigDecimal("2501"); // 1 + 2500
        assertThat(totalPayment).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("배송비 정책 조회 테스트")
    void testGetShippingPolicy() {
        // When
        ShippingCalculator.ShippingPolicy policy = shippingCalculator.getShippingPolicy();

        // Then
        assertThat(policy.freeShippingThreshold()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(policy.fee()).isEqualByComparingTo(new BigDecimal("2500"));
    }
}
