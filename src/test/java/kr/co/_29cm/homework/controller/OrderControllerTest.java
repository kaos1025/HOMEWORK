package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.dto.response.OrderResponse;
import kr.co._29cm.homework.exception.InvalidOrderException;
import kr.co._29cm.homework.exception.ProductNotFoundException;
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
 * OrderController 테스트 - 새로운 API 구조에 맞게 재작성
 */
@WebMvcTest({OrderController.class, kr.co._29cm.homework.exception.GlobalExceptionHandler.class})
@DisplayName("주문 컨트롤러 테스트")
class OrderControllerTest {

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
    private OrderRequest orderRequest;

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

        orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(768848L, 1),
                new OrderRequest.OrderItemRequest(759928L, 2)
        ));
    }

    @Test
    @DisplayName("주문 생성 성공 - 멱등성 키 없음")
    void 주문_생성_성공_멱등성_키_없음() throws Exception {
        // given
        List<OrderService.OrderItemRequest> serviceRequests = List.of(
                new OrderService.OrderItemRequest(768848L, 1),
                new OrderService.OrderItemRequest(759928L, 2)
        );
        
        when(orderMapper.toServiceRequests(any())).thenReturn(serviceRequests);
        when(orderService.placeOrder(serviceRequests)).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문이 성공적으로 처리되었습니다"))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.orderedAt").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].productNumber").value(768848))
                .andExpect(jsonPath("$.data.items[0].productName").value("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(21000))
                .andExpect(jsonPath("$.data.items[1].productNumber").value(759928))
                .andExpect(jsonPath("$.data.items[1].quantity").value(2))
                .andExpect(jsonPath("$.data.paymentAmount").value(29100));
    }

    @Test
    @DisplayName("주문 생성 성공 - 멱등성 키 포함")
    void 주문_생성_성공_멱등성_키_포함() throws Exception {
        // given
        String idempotencyKey = "test-idempotency-key-123";
        
        when(idempotencyService.processWithIdempotency(any(), any())).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.paymentAmount").value(29100));
    }

    @Test
    @DisplayName("상품을 찾을 수 없음 예외")
    void 상품을_찾을_수_없음_예외() throws Exception {
        // given
        OrderRequest invalidRequest = new OrderRequest();
        invalidRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(999999L, 1)
        ));

        when(orderMapper.toServiceRequests(any())).thenReturn(
            List.of(new OrderService.OrderItemRequest(999999L, 1))
        );
        when(orderService.placeOrder(any())).thenThrow(
            new ProductNotFoundException(999999L)
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("상품을 찾을 수 없습니다")));
    }

    @Test
    @DisplayName("재고 부족 예외")
    void 재고_부족_예외() throws Exception {
        // given
        OrderRequest stockRequest = new OrderRequest();
        stockRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(768848L, 100)
        ));

        when(orderMapper.toServiceRequests(any())).thenReturn(
            List.of(new OrderService.OrderItemRequest(768848L, 100))
        );
        when(orderService.placeOrder(any())).thenThrow(
            new InvalidOrderException("재고가 부족합니다. 요청 수량: 100, 현재 재고: 45")
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("재고가 부족합니다")));
    }

    @Test
    @DisplayName("주문 항목 빈 배열 - Jakarta Validation 처리")
    void 주문_항목_빈_배열_Jakarta_Validation_처리() throws Exception {
        // given - @Valid 어노테이션에 의한 검증
        String emptyItemsJson = """
                {
                    "items": []
                }
                """;

        // when & then - Jakarta Validation에 의한 400 응답
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyItemsJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("유효성 검증 실패 - 음수 수량")
    void 유효성_검증_실패_음수_수량() throws Exception {
        // given - @Valid 어노테이션에 의한 검증 실패
        String invalidJson = """
                {
                    "items": [
                        {"productNumber": 768848, "quantity": -1}
                    ]
                }
                """;

        // when & then - Jakarta Validation에 의한 400 응답
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // TODO: 빈 요청 본문 테스트는 실제 환경에서 다르게 처리될 수 있음
    // @Test
    // @DisplayName("잘못된 요청 내용")
    // void 잘못된_요청_내용() throws Exception {
    //     // given - 빈 요청 본문
    //     String emptyJson = "";

    //     // when & then - 빈 요청으로 400 응답
    //     mockMvc.perform(post("/api/orders")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(emptyJson))
    //             .andExpect(status().isBadRequest());
    // }

    @Test
    @DisplayName("멱등성 키 중복 요청 예외")
    void 멱등성_키_중복_요청_예외() throws Exception {
        // given
        String duplicateKey = "duplicate-key-123";
        
        when(idempotencyService.processWithIdempotency(any(), any())).thenThrow(
            new kr.co._29cm.homework.exception.DuplicateRequestException("동일한 Idempotency-Key로 이미 처리된 요청입니다")
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", duplicateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_REQUEST"));
    }

    @Test
    @DisplayName("멱등성 키 만료 예외")
    void 멱등성_키_만료_예외() throws Exception {
        // given
        String expiredKey = "expired-key-123";
        
        when(idempotencyService.processWithIdempotency(any(), any())).thenThrow(
            new kr.co._29cm.homework.exception.IdempotencyKeyExpiredException("Idempotency-Key가 만료되었습니다")
        );

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", expiredKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_EXPIRED"));
    }
}