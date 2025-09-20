package kr.co._29cm.homework.service;

import kr.co._29cm.homework.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 배송비 계산 서비스
 * 
 * 주문 금액에 따라 배송비를 계산하고 총 결제 금액을 산출합니다.
 * 무료배송 기준 금액 미만일 경우 배송비를 부과하며,
 * 모든 금액 계산 시 HALF_UP 라운딩을 적용합니다.
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Service
@RequiredArgsConstructor
public class ShippingCalculator {

    private final ShippingProperties shippingProperties;

    /**
     * 배송비 계산
     * 
     * 주문 총액이 무료배송 기준 미만인 경우 배송비를 반환하고,
     * 기준 이상인 경우 0원을 반환합니다.
     * 
     * @param totalOrderAmount 주문 총액
     * @return 배송비 (무료배송 기준 미만: 설정된 배송비, 기준 이상: 0원)
     */
    public BigDecimal calculateShippingFee(BigDecimal totalOrderAmount) {
        if (totalOrderAmount.compareTo(shippingProperties.getFreeShippingThreshold()) < 0) {
            return shippingProperties.getFee();
        }
        return BigDecimal.ZERO;
    }

    /**
     * 총 결제 금액 계산
     * 
     * 주문 총액에 배송비를 더하여 총 결제 금액을 산출합니다.
     * 계산 결과는 HALF_UP 라운딩을 적용하여 소수점 둘째 자리까지 반환합니다.
     * 
     * @param totalOrderAmount 주문 총액
     * @return 총 결제 금액 (주문 총액 + 배송비, HALF_UP 라운딩 적용)
     */
    public BigDecimal calculateTotalPaymentAmount(BigDecimal totalOrderAmount) {
        BigDecimal shippingFee = calculateShippingFee(totalOrderAmount);
        return totalOrderAmount.add(shippingFee).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 현재 배송비 정책 정보 조회
     * 
     * @return 현재 설정된 배송비 정책 (무료배송 기준 금액, 배송비)
     */
    public ShippingPolicy getShippingPolicy() {
        return new ShippingPolicy(
                shippingProperties.getFreeShippingThreshold(),
                shippingProperties.getFee()
        );
    }

    /**
     * 배송비 정책 정보를 담는 레코드
     * 
     * @param freeShippingThreshold 무료배송 기준 금액
     * @param shippingFee 배송비
     */
    public record ShippingPolicy(
            BigDecimal freeShippingThreshold,
            BigDecimal shippingFee
    ) {}
}
