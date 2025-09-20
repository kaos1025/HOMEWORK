package kr.co._29cm.homework.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액을 나타내는 값 객체 (Value Object)
 * 
 * 금액의 정밀도를 보장하고 모든 금액 연산을 캡슐화합니다.
 * 정밀도 정책: precision=19, scale=2 (소수점 둘째 자리까지)
 * 모든 금액 연산 후 HALF_UP 라운딩을 적용하여 일관성을 보장합니다.
 * 
 * @author 29CM Homework
 * @version 1.0
 * @since 2025-01-19
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Money {

    private static final int PRECISION = 19;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * 정수 금액으로 Money 객체 생성
     */
    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    /**
     * BigDecimal 금액으로 Money 객체 생성
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * 문자열 금액으로 Money 객체 생성
     */
    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    /**
     * Money 객체 더하기
     */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /**
     * Money 객체 빼기
     */
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    /**
     * 정수로 곱하기
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    /**
     * BigDecimal로 곱하기
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }

    /**
     * 정수로 나누기
     */
    public Money divide(int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("0으로 나눌 수 없습니다.");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING_MODE));
    }

    /**
     * BigDecimal로 나누기
     */
    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("0으로 나눌 수 없습니다.");
        }
        return new Money(this.amount.divide(divisor, SCALE, ROUNDING_MODE));
    }

    /**
     * 다른 Money와 비교 (크다면 양수, 작다면 음수, 같다면 0)
     */
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    /**
     * 다른 Money보다 큰지 확인
     */
    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    /**
     * 다른 Money보다 크거나 같은지 확인
     */
    public boolean isGreaterThanOrEqual(Money other) {
        return this.compareTo(other) >= 0;
    }

    /**
     * 다른 Money보다 작은지 확인
     */
    public boolean isLessThan(Money other) {
        return this.compareTo(other) < 0;
    }

    /**
     * 다른 Money보다 작거나 같은지 확인
     */
    public boolean isLessThanOrEqual(Money other) {
        return this.compareTo(other) <= 0;
    }

    /**
     * 다른 Money와 같은지 확인
     */
    public boolean equals(Money other) {
        return this.compareTo(other) == 0;
    }

    /**
     * 0원인지 확인
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 양수인지 확인
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 음수인지 확인
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * BigDecimal 형태로 반환 (JPA 매핑용)
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 정수 형태로 반환 (원 단위)
     */
    public long getAmountAsLong() {
        return amount.longValue();
    }

    /**
     * 문자열 형태로 반환
     */
    @Override
    public String toString() {
        return amount.toString();
    }

    /**
     * 통화 형태로 포맷된 문자열 반환 (예: "10,000원")
     */
    public String toFormattedString() {
        return String.format("%,d원", amount.longValue());
    }

    /**
     * 정밀도 검증 (precision=19, scale=2)
     */
    private void validatePrecision() {
        if (amount.precision() > PRECISION) {
            throw new IllegalArgumentException(
                String.format("금액의 정밀도가 초과되었습니다. 최대 정밀도: %d, 현재 정밀도: %d", 
                    PRECISION, amount.precision()));
        }
    }
}
