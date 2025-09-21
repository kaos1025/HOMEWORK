package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderService 주문 조회 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 조회 기능 테스트")
class OrderServiceQueryTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder1;
    private Order testOrder2;
    private String testOrderNumber1;
    private String testOrderNumber2;

    @BeforeEach
    void setUp() {
        testOrderNumber1 = UUID.randomUUID().toString();
        testOrderNumber2 = UUID.randomUUID().toString();

        Product product = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);

        testOrder1 = new Order(testOrderNumber1, LocalDateTime.now().minusDays(1));
        testOrder1.addItem(new OrderItem(product, 1));
        testOrder1.setPaymentAmount(BigDecimal.valueOf(23500));

        testOrder2 = new Order(testOrderNumber2, LocalDateTime.now());
        testOrder2.addItem(new OrderItem(product, 2));
        testOrder2.setPaymentAmount(BigDecimal.valueOf(46500));
    }

    @Test
    @DisplayName("주문번호로 주문 조회 성공")
    void 주문번호로_주문_조회_성공() {
        // given
        when(orderRepository.findByOrderNumber(testOrderNumber1)).thenReturn(Optional.of(testOrder1));

        // when
        Optional<Order> result = orderService.findOrderByOrderNumber(testOrderNumber1);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo(testOrderNumber1);
        assertThat(result.get().getPaymentAmount()).isEqualTo(BigDecimal.valueOf(23500));
        
        verify(orderRepository).findByOrderNumber(testOrderNumber1);
    }

    @Test
    @DisplayName("주문번호로 주문 조회 실패 - 존재하지 않는 주문")
    void 주문번호로_주문_조회_실패_존재하지_않는_주문() {
        // given
        String nonExistentOrderNumber = "non-existent-order";
        when(orderRepository.findByOrderNumber(nonExistentOrderNumber)).thenReturn(Optional.empty());

        // when
        Optional<Order> result = orderService.findOrderByOrderNumber(nonExistentOrderNumber);

        // then
        assertThat(result).isEmpty();
        
        verify(orderRepository).findByOrderNumber(nonExistentOrderNumber);
    }

    @Test
    @DisplayName("주문 목록 페이징 조회 성공")
    void 주문_목록_페이징_조회_성공() {
        // given
        List<Order> orders = List.of(testOrder2, testOrder1); // 최신순 정렬
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 2);
        when(orderRepository.findAllByOrderByOrderedAtDesc(any(Pageable.class))).thenReturn(orderPage);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderService.findOrders(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo(testOrderNumber2);
        assertThat(result.getContent().get(1).getOrderNumber()).isEqualTo(testOrderNumber1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        
        verify(orderRepository).findAllByOrderByOrderedAtDesc(pageable);
    }

    @Test
    @DisplayName("기간별 주문 조회 성공")
    void 기간별_주문_조회_성공() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<Order> orders = List.of(testOrder1, testOrder2);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 2);
        
        when(orderRepository.findOrdersByDateRange(eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(orderPage);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderService.findOrdersByDateRange(startDate, endDate, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        verify(orderRepository).findOrdersByDateRange(startDate, endDate, pageable);
    }

    @Test
    @DisplayName("주문 목록 조회 - 빈 결과")
    void 주문_목록_조회_빈_결과() {
        // given
        Page<Order> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(orderRepository.findAllByOrderByOrderedAtDesc(any(Pageable.class))).thenReturn(emptyPage);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderService.findOrders(pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        
        verify(orderRepository).findAllByOrderByOrderedAtDesc(pageable);
    }

    @Test
    @DisplayName("다른 페이지 조회 테스트")
    void 다른_페이지_조회_테스트() {
        // given
        List<Order> orders = List.of(testOrder1);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(1, 5), 6); // 전체 6개 중 2페이지
        when(orderRepository.findAllByOrderByOrderedAtDesc(any(Pageable.class))).thenReturn(orderPage);

        // when
        Pageable pageable = PageRequest.of(1, 5);
        Page<Order> result = orderService.findOrders(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1); // 현재 페이지
        assertThat(result.getSize()).isEqualTo(5); // 페이지 크기
        assertThat(result.getTotalElements()).isEqualTo(6); // 전체 요소 수
        assertThat(result.getTotalPages()).isEqualTo(2); // 전체 페이지 수
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        
        verify(orderRepository).findAllByOrderByOrderedAtDesc(pageable);
    }
}
