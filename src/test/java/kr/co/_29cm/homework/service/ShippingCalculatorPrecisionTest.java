package kr.co._29cm.homework.service;

import kr.co._29cm.homework.config.ShippingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "shipping.policy.free-shipping-threshold=50000",
        "shipping.policy.fee=2500"
})
class ShippingCalculatorPrecisionTest {

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
    @DisplayName("배송비 계산 결과 정밀도 확인 - HALF_UP 라운딩")
    void testShippingFeeCalculationPrecision() {
        // Given - 소수점이 있는 금액
        BigDecimal totalAmount = new BigDecimal("12345.678");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - 소수점 둘째 자리까지 HALF_UP 라운딩
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("14845.68")); // 12345.68 + 2500
    }

    @Test
    @DisplayName("무료배송 금액 계산 결과 정밀도 확인")
    void testFreeShippingCalculationPrecision() {
        // Given - 무료배송 기준 이상의 소수점 금액
        BigDecimal totalAmount = new BigDecimal("50000.999");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - 소수점 둘째 자리까지 HALF_UP 라운딩
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("50101.00")); // 50001.00 + 0
    }

    @Test
    @DisplayName("복잡한 소수점 계산 정밀도 확인")
    void testComplexDecimalCalculationPrecision() {
        // Given - 여러 소수점 연산이 포함된 금액
        BigDecimal totalAmount = new BigDecimal("12345.125");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - HALF_UP 라운딩 적용
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("14845.13")); // 12345.13 + 2500
    }

    @Test
    @DisplayName("경계값에서의 정밀도 확인 - 49999.999원")
    void testBoundaryValuePrecision() {
        // Given - 무료배송 기준 바로 아래 소수점 금액
        BigDecimal totalAmount = new BigDecimal("49999.999");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - HALF_UP 라운딩으로 50000.00이 되어 무료배송
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("50000.00")); // 50000.00 + 0
    }

    @Test
    @DisplayName("매우 큰 금액에서의 정밀도 확인")
    void testLargeAmountPrecision() {
        // Given - 큰 금액
        BigDecimal totalAmount = new BigDecimal("999999999999999.999");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - 정밀도 유지
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("1000000000000000.00"));
    }

    @Test
    @DisplayName("매우 작은 금액에서의 정밀도 확인")
    void testSmallAmountPrecision() {
        // Given - 매우 작은 금액
        BigDecimal totalAmount = new BigDecimal("0.001");

        // When
        BigDecimal totalPayment = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // Then - HALF_UP 라운딩 적용
        assertThat(totalPayment.scale()).isEqualTo(2);
        assertThat(totalPayment).isEqualByComparingTo(new BigDecimal("2500.00")); // 0.00 + 2500
    }

    @Test
    @DisplayName("정밀도 일관성 확인 - 여러 연산 후 결과")
    void testPrecisionConsistency() {
        // Given
        BigDecimal[] amounts = {
                new BigDecimal("1000.125"),
                new BigDecimal("2000.375"),
                new BigDecimal("3000.625"),
                new BigDecimal("4000.875")
        };

        // When & Then - 모든 결과가 동일한 정밀도를 가져야 함
        for (BigDecimal amount : amounts) {
            BigDecimal result = shippingCalculator.calculateTotalPaymentAmount(amount);
            assertThat(result.scale()).isEqualTo(2);
            assertThat(result).isEqualTo(result.setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Test
    @DisplayName("라운딩 모드 검증 - HALF_UP")
    void testRoundingModeValidation() {
        // Given - 다양한 HALF_UP 테스트 케이스
        BigDecimal[] testCases = {
                new BigDecimal("123.125"), // 5로 끝남 - 올림
                new BigDecimal("123.124"), // 4로 끝남 - 내림
                new BigDecimal("123.126"), // 6으로 끝남 - 올림
                new BigDecimal("123.135"), // 5로 끝남 - 올림
                new BigDecimal("123.134")  // 4로 끝남 - 내림
        };

        BigDecimal[] expectedResults = {
                new BigDecimal("125.63"), // 123.13 + 2.5
                new BigDecimal("125.62"), // 123.12 + 2.5
                new BigDecimal("125.63"), // 123.13 + 2.5
                new BigDecimal("125.64"), // 123.14 + 2.5
                new BigDecimal("125.63")  // 123.13 + 2.5
        };

        // When & Then
        for (int i = 0; i < testCases.length; i++) {
            BigDecimal result = shippingCalculator.calculateTotalPaymentAmount(testCases[i]);
            assertThat(result).isEqualByComparingTo(expectedResults[i]);
        }
    }
}
