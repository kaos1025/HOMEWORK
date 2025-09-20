package kr.co._29cm.homework.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShippingCalculator {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(50000);
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(2500);

    public BigDecimal calculateShippingFee(BigDecimal totalOrderAmount) {
        if (totalOrderAmount.compareTo(FREE_SHIPPING_THRESHOLD) < 0) {
            return SHIPPING_FEE;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalPaymentAmount(BigDecimal totalOrderAmount) {
        BigDecimal shippingFee = calculateShippingFee(totalOrderAmount);
        return totalOrderAmount.add(shippingFee);
    }
}
