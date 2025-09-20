package kr.co._29cm.homework.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShippingCalculatorTest {

    @Autowired
    private ShippingCalculator shippingCalculator;

    @Test
    void 배송비_계산_5만원_미만() {
        // given
        BigDecimal totalAmount = BigDecimal.valueOf(30000);

        // when
        BigDecimal shippingFee = shippingCalculator.calculateShippingFee(totalAmount);

        // then
        assertThat(shippingFee).isEqualTo(BigDecimal.valueOf(2500));
    }

    @Test
    void 배송비_계산_5만원_이상() {
        // given
        BigDecimal totalAmount = BigDecimal.valueOf(50000);

        // when
        BigDecimal shippingFee = shippingCalculator.calculateShippingFee(totalAmount);

        // then
        assertThat(shippingFee).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void 총_결제금액_계산() {
        // given
        BigDecimal totalAmount = BigDecimal.valueOf(30000);

        // when
        BigDecimal paymentAmount = shippingCalculator.calculateTotalPaymentAmount(totalAmount);

        // then
        assertThat(paymentAmount).isEqualTo(BigDecimal.valueOf(32500));
    }
}
