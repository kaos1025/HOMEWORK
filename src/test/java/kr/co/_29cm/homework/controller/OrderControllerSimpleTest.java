package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.response.OrderResponse;
import kr.co._29cm.homework.mapper.OrderMapper;
import kr.co._29cm.homework.service.IdempotencyService;
import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController 기본 기능 테스트 (기존 테스트 대체)
 */
@WebMvcTest({OrderController.class, kr.co._29cm.homework.exception.GlobalExceptionHandler.class})
@DisplayName("주문 컨트롤러 기본 테스트")
class OrderControllerSimpleTest {

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

    @BeforeEach
    void setUp() {
        Product product1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        Product product2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);

        testOrder = new Order(UUID.randomUUID().toString(), LocalDateTime.now());
        testOrder.addItem(new OrderItem(product1, 1));
        testOrder.addItem(new OrderItem(product2, 2));
        testOrder.setPaymentAmount(BigDecimal.valueOf(29100));

        orderResponse = OrderResponse.builder()
                .orderNumber(testOrder.getOrderNumber())
                .orderedAt(testOrder.getOrderedAt())
                .items(List.of(
                    OrderResponse.OrderItemResponse.builder()
                        .productNumber(768848L)
                        .productName("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
                        .quantity(1)
                        .unitPrice(BigDecimal.valueOf(21000))
                        .subtotal(BigDecimal.valueOf(21000))
                        .build(),
                    OrderResponse.OrderItemResponse.builder()
                        .productNumber(759928L)
                        .productName("마스크 스트랩 분실방지 오염방지 목걸이")
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(2800))
                        .subtotal(BigDecimal.valueOf(5600))
                        .build()
                ))
                .paymentAmount(BigDecimal.valueOf(29100))
                .build();
    }

    @Test
    @DisplayName("주문 생성 성공")
    void 주문_생성_성공() throws Exception {
        // given
        String requestJson = """
                {
                    "items": [
                        {"productNumber": 768848, "quantity": 1},
                        {"productNumber": 759928, "quantity": 2}
                    ]
                }
                """;

        when(orderService.placeOrder(any())).thenReturn(testOrder);
        when(orderMapper.toServiceRequests(any())).thenReturn(
            List.of(
                new OrderService.OrderItemRequest(768848L, 1),
                new OrderService.OrderItemRequest(759928L, 2)
            )
        );
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.orderedAt").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.paymentAmount").value(29100));
    }

    @Test
    @DisplayName("잘못된 JSON 형식 요청")
    void 잘못된_JSON_형식_요청() throws Exception {
        // given - 잘못된 JSON 형식
        String requestJson = """
                {
                    "items": [
                        {"productNumber": 768848, "quantity": 1
                    ]
                }
                """;

        // when & then - JSON 파싱 오류로 400 Bad Request
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비즈니스 예외 처리 테스트")
    void 비즈니스_예외_처리_테스트() throws Exception {
        // given
        String requestJson = """
                {
                    "items": [
                        {"productNumber": 999999, "quantity": 1}
                    ]
                }
                """;

        when(orderService.placeOrder(any())).thenThrow(
            new kr.co._29cm.homework.exception.ProductNotFoundException(999999L)
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }
}
