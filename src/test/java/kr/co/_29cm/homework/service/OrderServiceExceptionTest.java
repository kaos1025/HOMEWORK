package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.exception.InsufficientStockException;
import kr.co._29cm.homework.exception.InvalidOrderException;
import kr.co._29cm.homework.exception.ProductNotFoundException;
import kr.co._29cm.homework.repository.OrderRepository;
import kr.co._29cm.homework.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * OrderService 예외 시나리오 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 예외 처리 테스트")
class OrderServiceExceptionTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShippingCalculator shippingCalculator;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("존재하지 않는 상품 주문 시 예외 발생")
    void 존재하지_않는_상품_주문_시_예외_발생() {
        // given
        Long nonExistentProductNumber = 999999L;
        when(productRepository.findWithLockByProductNumber(nonExistentProductNumber))
                .thenReturn(Optional.empty());

        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(nonExistentProductNumber, 1)
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void 재고_부족_시_예외_발생() {
        // given
        Long productNumber = 768848L;
        Product product = new Product(productNumber, "테스트 상품", BigDecimal.valueOf(10000), 5);
        when(productRepository.findWithLockByProductNumber(productNumber))
                .thenReturn(Optional.of(product));

        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(productNumber, 10) // 재고(5)보다 많은 수량
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("주문 항목이 비어있을 때 예외 발생")
    void 주문_항목이_비어있을_때_예외_발생() {
        // given
        List<OrderService.OrderItemRequest> emptyRequests = List.of();

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(emptyRequests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("주문 항목이 비어있습니다");
    }

    @Test
    @DisplayName("주문 항목이 null일 때 예외 발생")
    void 주문_항목이_null일_때_예외_발생() {
        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("주문 항목이 비어있습니다");
    }

    @Test
    @DisplayName("상품번호가 null일 때 예외 발생")
    void 상품번호가_null일_때_예외_발생() {
        // given
        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(null, 1)
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("상품번호는 필수입니다");
    }

    @Test
    @DisplayName("주문 수량이 0일 때 예외 발생")
    void 주문_수량이_0일_때_예외_발생() {
        // given
        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(768848L, 0)
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("주문 수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("주문 수량이 음수일 때 예외 발생")
    void 주문_수량이_음수일_때_예외_발생() {
        // given
        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(768848L, -1)
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("주문 수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("주문 수량이 null일 때 예외 발생")
    void 주문_수량이_null일_때_예외_발생() {
        // given
        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(768848L, null)
        );

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("주문 수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("복합 검증 오류 - 첫 번째 오류가 우선 발생")
    void 복합_검증_오류_첫_번째_오류가_우선_발생() {
        // given - 상품번호 null과 수량 0 동시 오류
        List<OrderService.OrderItemRequest> requests = List.of(
                new OrderService.OrderItemRequest(null, 0)
        );

        // when & then - 상품번호 오류가 먼저 검증됨
        assertThatThrownBy(() -> orderService.placeOrder(requests))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("상품번호는 필수입니다");
    }
}
