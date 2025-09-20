package kr.co._29cm.homework.service;

import kr.co._29cm.homework.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ShippingCalculator {

    private final ShippingProperties shippingProperties;

    public BigDecimal calculateShippingFee(BigDecimal totalOrderAmount) {
        if (totalOrderAmount.compareTo(shippingProperties.getFreeShippingThreshold()) < 0) {
            return shippingProperties.getFee();
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalPaymentAmount(BigDecimal totalOrderAmount) {
        BigDecimal shippingFee = calculateShippingFee(totalOrderAmount);
        return totalOrderAmount.add(shippingFee);
    }

    /**
     * 현재 배송비 정책 정보를 반환합니다.
     */
    public ShippingPolicy getShippingPolicy() {
        return new ShippingPolicy(
                shippingProperties.getFreeShippingThreshold(),
                shippingProperties.getFee()
        );
    }

    public record ShippingPolicy(
            BigDecimal freeShippingThreshold,
            BigDecimal shippingFee
    ) {}
}
