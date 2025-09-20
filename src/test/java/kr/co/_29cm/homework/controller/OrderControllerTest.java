package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        Product product1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        Product product2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);

        testOrder = new Order(UUID.randomUUID().toString(), LocalDateTime.now());
        testOrder.addItem(new OrderItem(product1, 1));
        testOrder.addItem(new OrderItem(product2, 2));
        testOrder.setPaymentAmount(BigDecimal.valueOf(26600)); // 21000 + (2800 * 2) + 2500(배송비)
    }

    @Test
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

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.orderedAt").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.items[0].productNumber").value(768848))
                .andExpect(jsonPath("$.items[0].productName").value("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice").value(21000))
                .andExpect(jsonPath("$.items[1].productNumber").value(759928))
                .andExpect(jsonPath("$.items[1].productName").value("마스크 스트랩 분실방지 오염방지 목걸이"))
                .andExpect(jsonPath("$.items[1].quantity").value(2))
                .andExpect(jsonPath("$.items[1].unitPrice").value(2800))
                .andExpect(jsonPath("$.paymentAmount").value(26600));
    }

    @Test
    void 주문_요청_유효성_검증_실패() throws Exception {
        // given - 잘못된 요청 데이터
        String requestJson = """
                {
                    "items": [
                        {"productNumber": null, "quantity": 1},
                        {"productNumber": 759928, "quantity": 0}
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 주문_요청_빈_항목() throws Exception {
        // given
        String requestJson = """
                {
                    "items": []
                }
                """;

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 주문_요청_JSON_형식_오류() throws Exception {
        // given - 잘못된 JSON 형식
        String requestJson = """
                {
                    "items": [
                        {"productNumber": 768848, "quantity": 1
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
