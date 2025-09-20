package kr.co._29cm.homework.service;

import kr.co._29cm.homework.config.ShippingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "shipping.policy.free-shipping-threshold=30000",
        "shipping.policy.fee=1500"
})
class ShippingCalculatorConfigTest {

    @Autowired
    private ShippingCalculator shippingCalculator;

    @Test
    void 설정파일_배송비_정책_적용_테스트() {
        // given
        BigDecimal totalAmount1 = BigDecimal.valueOf(25000); // 3만원 미만
        BigDecimal totalAmount2 = BigDecimal.valueOf(35000); // 3만원 이상

        // when
        BigDecimal shippingFee1 = shippingCalculator.calculateShippingFee(totalAmount1);
        BigDecimal shippingFee2 = shippingCalculator.calculateShippingFee(totalAmount2);
        ShippingCalculator.ShippingPolicy policy = shippingCalculator.getShippingPolicy();

        // then
        assertThat(shippingFee1).isEqualTo(BigDecimal.valueOf(1500)); // 설정된 배송비
        assertThat(shippingFee2).isEqualTo(BigDecimal.ZERO); // 무료배송
        assertThat(policy.freeShippingThreshold()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(policy.shippingFee()).isEqualTo(BigDecimal.valueOf(1500));
    }
}
