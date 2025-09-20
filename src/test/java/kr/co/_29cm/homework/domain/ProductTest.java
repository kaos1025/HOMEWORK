package kr.co._29cm.homework.domain;

import kr.co._29cm.homework.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
    }

    @Test
    @DisplayName("정상적인 재고 차감")
    void 정상적인_재고_차감() {
        // when
        product.decreaseStock(10);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(35);
    }

    @Test
    @DisplayName("재고를 모두 차감")
    void 재고를_모두_차감() {
        // when
        product.decreaseStock(45);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void 재고_부족_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> product.decreaseStock(50))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다")
                .hasMessageContaining("768848")
                .hasMessageContaining("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
                .hasMessageContaining("요청수량: 50")
                .hasMessageContaining("재고: 45");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("0 이하 수량으로 재고 차감 시 예외 발생")
    void 잘못된_수량으로_재고_차감_시_예외_발생(int invalidQuantity) {
        // when & then
        assertThatThrownBy(() -> product.decreaseStock(invalidQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 수량은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("재고 차감 후 원본 재고는 변경되지 않음")
    void 재고_차감_후_원본_재고는_변경되지_않음() {
        // given
        int originalStock = product.getStockQuantity();

        // when
        product.decreaseStock(10);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(originalStock - 10);
        assertThat(originalStock).isEqualTo(45); // 원본 값은 그대로
    }

    @Test
    @DisplayName("여러 번 재고 차감")
    void 여러_번_재고_차감() {
        // when
        product.decreaseStock(10);
        product.decreaseStock(20);
        product.decreaseStock(5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("재고가 0인 상품에서 재고 차감 시 예외 발생")
    void 재고가_0인_상품에서_재고_차감_시_예외_발생() {
        // given
        Product zeroStockProduct = new Product(999L, "재고없음", BigDecimal.valueOf(1000), 0);

        // when & then
        assertThatThrownBy(() -> zeroStockProduct.decreaseStock(1))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다");
    }
}
