package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.response.OrderResponse;
import kr.co._29cm.homework.dto.response.OrderSummaryResponse;
import kr.co._29cm.homework.exception.OrderNotFoundException;
import kr.co._29cm.homework.mapper.OrderMapper;
import kr.co._29cm.homework.service.IdempotencyService;
import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController의 주문 조회 기능 테스트
 */
@WebMvcTest({OrderController.class, kr.co._29cm.homework.exception.GlobalExceptionHandler.class})
@DisplayName("주문 조회 컨트롤러 테스트")
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;
    
    @MockBean
    private OrderMapper orderMapper;
    
    @MockBean
    private IdempotencyService idempotencyService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;
    private OrderResponse orderResponse;
    private OrderSummaryResponse orderSummaryResponse;
    private String testOrderNumber;

    @BeforeEach
    void setUp() {
        testOrderNumber = UUID.randomUUID().toString();
        
        Product product1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        
        testOrder = new Order(testOrderNumber, LocalDateTime.now());
        testOrder.addItem(new OrderItem(product1, 1));
        testOrder.setPaymentAmount(BigDecimal.valueOf(23500)); // 21000 + 2500(배송비)

        orderResponse = OrderResponse.builder()
                .orderNumber(testOrderNumber)
                .orderedAt(testOrder.getOrderedAt())
                .items(List.of(
                    OrderResponse.OrderItemResponse.builder()
                        .productNumber(768848L)
                        .productName("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
                        .quantity(1)
                        .unitPrice(BigDecimal.valueOf(21000))
                        .subtotal(BigDecimal.valueOf(21000))
                        .build()
                ))
                .paymentAmount(BigDecimal.valueOf(23500))
                .build();

        orderSummaryResponse = new OrderSummaryResponse(
                testOrderNumber,
                testOrder.getOrderedAt(),
                1,
                BigDecimal.valueOf(23500)
        );
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void 주문_상세_조회_성공() throws Exception {
        // given
        when(orderService.findOrderByOrderNumber(testOrderNumber)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // when & then
        mockMvc.perform(get("/api/orders/{orderNumber}", testOrderNumber)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value(testOrderNumber))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.paymentAmount").value(23500));
    }

    // TODO: GlobalExceptionHandler 통합 후 활성화
    // @Test
    // @DisplayName("주문 상세 조회 실패 - 주문을 찾을 수 없음")
    // void 주문_상세_조회_실패_주문을_찾을_수_없음() throws Exception {
    //     // given
    //     String nonExistentOrderNumber = "non-existent-order";
    //     when(orderService.findOrderByOrderNumber(nonExistentOrderNumber)).thenReturn(Optional.empty());

    //     // when & then
    //     mockMvc.perform(get("/api/orders/{orderNumber}", nonExistentOrderNumber)
    //                     .contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(status().isNotFound())
    //             .andExpect(jsonPath("$.success").value(false))
    //             .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
    // }

    @Test
    @DisplayName("주문 목록 페이징 조회 성공")
    void 주문_목록_페이징_조회_성공() throws Exception {
        // given
        List<Order> orders = List.of(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders);
        when(orderService.findOrders(any(Pageable.class))).thenReturn(orderPage);
        when(orderMapper.toSummaryResponse(testOrder)).thenReturn(orderSummaryResponse);

        // when & then
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("기간별 주문 조회 성공")
    void 기간별_주문_조회_성공() throws Exception {
        // given
        List<Order> orders = List.of(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders);
        when(orderService.findOrdersByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(orderPage);
        when(orderMapper.toSummaryResponse(testOrder)).thenReturn(orderSummaryResponse);

        // when & then
        mockMvc.perform(get("/api/orders")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-01-31T23:59:59")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("주문 목록 조회 - 빈 결과")
    void 주문_목록_조회_빈_결과() throws Exception {
        // given
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(orderService.findOrders(any(Pageable.class))).thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
