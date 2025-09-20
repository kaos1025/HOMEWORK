package kr.co._29cm.homework.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("정수로 Money 객체 생성")
    void testCreateMoneyFromLong() {
        // When
        Money money = Money.of(10000);

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(money.getAmountAsLong()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("BigDecimal로 Money 객체 생성")
    void testCreateMoneyFromBigDecimal() {
        // When
        Money money = Money.of(new BigDecimal("12345.67"));

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("12345.67"));
        assertThat(money.getAmountAsLong()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("문자열로 Money 객체 생성")
    void testCreateMoneyFromString() {
        // When
        Money money = Money.of("99999.99");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("99999.99"));
    }

    @Test
    @DisplayName("null 금액으로 Money 생성 시 예외 발생")
    void testCreateMoneyWithNull_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 null일 수 없습니다.");
    }

    @Test
    @DisplayName("Money 객체 더하기")
    void testAddMoney() {
        // Given
        Money money1 = Money.of(10000);
        Money money2 = Money.of(2500);

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("12500.00"));
    }

    @Test
    @DisplayName("Money 객체 빼기")
    void testSubtractMoney() {
        // Given
        Money money1 = Money.of(10000);
        Money money2 = Money.of(2500);

        // When
        Money result = money1.subtract(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("7500.00"));
    }

    @Test
    @DisplayName("정수로 곱하기")
    void testMultiplyByInt() {
        // Given
        Money money = Money.of(1000);

        // When
        Money result = money.multiply(5);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("BigDecimal로 곱하기")
    void testMultiplyByBigDecimal() {
        // Given
        Money money = Money.of(1000);

        // When
        Money result = money.multiply(new BigDecimal("1.5"));

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("정수로 나누기")
    void testDivideByInt() {
        // Given
        Money money = Money.of(1000);

        // When
        Money result = money.divide(3);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("333.33")); // HALF_UP 라운딩
    }

    @Test
    @DisplayName("BigDecimal로 나누기")
    void testDivideByBigDecimal() {
        // Given
        Money money = Money.of(1000);

        // When
        Money result = money.divide(new BigDecimal("3.0"));

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("333.33")); // HALF_UP 라운딩
    }

    @Test
    @DisplayName("0으로 나누기 시 예외 발생")
    void testDivideByZero_ShouldThrowException() {
        // Given
        Money money = Money.of(1000);

        // When & Then
        assertThatThrownBy(() -> money.divide(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0으로 나눌 수 없습니다.");

        assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0으로 나눌 수 없습니다.");
    }

    @Test
    @DisplayName("Money 객체 비교")
    void testCompareMoney() {
        // Given
        Money money1 = Money.of(1000);
        Money money2 = Money.of(2000);
        Money money3 = Money.of(1000);

        // When & Then
        assertThat(money1.compareTo(money2)).isNegative();
        assertThat(money2.compareTo(money1)).isPositive();
        assertThat(money1.compareTo(money3)).isZero();

        assertThat(money1.isLessThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isTrue();
        assertThat(money1.equals(money3)).isTrue();
        assertThat(money1.isGreaterThanOrEqual(money3)).isTrue();
        assertThat(money1.isLessThanOrEqual(money3)).isTrue();
    }

    @Test
    @DisplayName("0원 확인")
    void testIsZero() {
        // Given
        Money zeroMoney = Money.of(0);
        Money positiveMoney = Money.of(1000);

        // When & Then
        assertThat(zeroMoney.isZero()).isTrue();
        assertThat(positiveMoney.isZero()).isFalse();

        assertThat(zeroMoney.isPositive()).isFalse();
        assertThat(positiveMoney.isPositive()).isTrue();

        assertThat(zeroMoney.isNegative()).isFalse();
    }

    @Test
    @DisplayName("음수 확인")
    void testIsNegative() {
        // Given
        Money negativeMoney = Money.of(-1000);

        // When & Then
        assertThat(negativeMoney.isNegative()).isTrue();
        assertThat(negativeMoney.isPositive()).isFalse();
        assertThat(negativeMoney.isZero()).isFalse();
    }

    @Test
    @DisplayName("문자열 표현")
    void testToString() {
        // Given
        Money money = Money.of(12345.67);

        // When & Then
        assertThat(money.toString()).isEqualTo("12345.67");
        assertThat(money.toFormattedString()).isEqualTo("12,345원");
    }

    @Test
    @DisplayName("라운딩 정책 확인 - HALF_UP")
    void testRoundingPolicy() {
        // Given & When
        Money money1 = Money.of(new BigDecimal("123.125")); // 소수점 셋째 자리가 5
        Money money2 = Money.of(new BigDecimal("123.124")); // 소수점 셋째 자리가 4
        Money money3 = Money.of(new BigDecimal("123.126")); // 소수점 셋째 자리가 6

        // Then - HALF_UP 라운딩 적용
        assertThat(money1.getAmount()).isEqualByComparingTo(new BigDecimal("123.13")); // 반올림
        assertThat(money2.getAmount()).isEqualByComparingTo(new BigDecimal("123.12")); // 내림
        assertThat(money3.getAmount()).isEqualByComparingTo(new BigDecimal("123.13")); // 반올림
    }

    @Test
    @DisplayName("복합 연산 - 주문 총액 계산")
    void testComplexCalculation() {
        // Given - 상품 가격 1000원, 수량 3개
        Money unitPrice = Money.of(1000);
        int quantity = 3;

        // When
        Money totalAmount = unitPrice.multiply(quantity);
        Money shippingFee = Money.of(2500);
        Money totalPayment = totalAmount.add(shippingFee);

        // Then
        assertThat(totalAmount.getAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(totalPayment.getAmount()).isEqualByComparingTo(new BigDecimal("5500.00"));
    }

    @Test
    @DisplayName("복합 연산 - 할인율 적용")
    void testDiscountCalculation() {
        // Given - 원가 10000원, 15% 할인
        Money originalPrice = Money.of(10000);
        BigDecimal discountRate = new BigDecimal("0.15");

        // When
        Money discountAmount = originalPrice.multiply(discountRate);
        Money finalPrice = originalPrice.subtract(discountAmount);

        // Then
        assertThat(discountAmount.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(finalPrice.getAmount()).isEqualByComparingTo(new BigDecimal("8500.00"));
    }

    @Test
    @DisplayName("Money 객체 동등성 확인")
    void testEqualsAndHashCode() {
        // Given
        Money money1 = Money.of(1000);
        Money money2 = Money.of(1000);
        Money money3 = Money.of(2000);

        // When & Then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1).isNotEqualTo(money3);
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        assertThat(money1.hashCode()).isNotEqualTo(money3.hashCode());
    }
}
