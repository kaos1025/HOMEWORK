package kr.co._29cm.homework.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.request.OrderItemRequest;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 계약 테스트: API 응답 바디 스냅샷을 고정하여 회귀 방지
 * 200/400/409 응답에 대한 정확한 JSON 구조 검증
 */
@SpringBootTest
@AutoConfigureWebMvc
@Transactional
@TestPropertySource(properties = {
        "order.strategy=pessimistic"
})
class OrderApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Product product;
    private final Long PRODUCT_NUMBER = 768848L;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성
        product = new Product(PRODUCT_NUMBER, "계약 테스트 상품", new BigDecimal("10000"), 10);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("계약 테스트: 성공 주문 응답 바디 스냅샷 (200 OK)")
    void testOrderSuccessResponse_ContractSnapshot_200Ok() throws Exception {
        // Given
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 1);

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        assertThat(responseBody).contains("\"orderNumber\"");
        assertThat(responseBody).contains("\"orderItems\"");
        assertThat(responseBody).contains("\"totalAmount\"");
        assertThat(responseBody).contains("\"shippingFee\"");
        assertThat(responseBody).contains("\"totalPayment\"");
        
        // 구체적인 값 검증 (스냅샷)
        assertThat(responseBody).contains("\"totalAmount\":10000");
        assertThat(responseBody).contains("\"shippingFee\":2500");
        assertThat(responseBody).contains("\"totalPayment\":12500");
        
        // 주문 아이템 구조 검증
        assertThat(responseBody).contains("\"productNumber\":768848");
        assertThat(responseBody).contains("\"productName\":\"계약 테스트 상품\"");
        assertThat(responseBody).contains("\"price\":10000");
        assertThat(responseBody).contains("\"quantity\":1");
        assertThat(responseBody).contains("\"amount\":10000");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 재고 부족 에러 응답 바디 스냅샷 (409 CONFLICT)")
    void testInsufficientStockResponse_ContractSnapshot_409Conflict() throws Exception {
        // Given - 재고보다 많은 수량 주문
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 15);

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 에러 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"code\"");
        assertThat(responseBody).contains("\"message\"");
        assertThat(responseBody).contains("\"timestamp\"");
        
        // 구체적인 에러 정보 검증 (스냅샷)
        assertThat(responseBody).contains("\"code\":\"INSUFFICIENT_STOCK\"");
        assertThat(responseBody).contains("재고가 부족합니다");
        assertThat(responseBody).contains("768848");
        assertThat(responseBody).contains("계약 테스트 상품");
        assertThat(responseBody).contains("15");
        assertThat(responseBody).contains("10");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 유효성 검증 에러 응답 바디 스냅샷 (400 BAD_REQUEST)")
    void testValidationErrorResponse_ContractSnapshot_400BadRequest() throws Exception {
        // Given - 잘못된 요청 데이터 (수량 0)
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 0);

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 유효성 검증 에러 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"code\"");
        assertThat(responseBody).contains("\"message\"");
        assertThat(responseBody).contains("\"data\"");
        
        // 구체적인 에러 정보 검증 (스냅샷)
        assertThat(responseBody).contains("\"code\":\"VALIDATION_ERROR\"");
        assertThat(responseBody).contains("입력값 검증에 실패했습니다");
        assertThat(responseBody).contains("\"fieldErrors\"");
        assertThat(responseBody).contains("\"globalErrors\"");
        
        // 필드 에러 구조 검증
        assertThat(responseBody).contains("orderItems[0].quantity");
        assertThat(responseBody).contains("수량은 1 이상이어야 합니다");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 존재하지 않는 상품 에러 응답 바디 스냅샷 (404 NOT_FOUND)")
    void testProductNotFoundResponse_ContractSnapshot_404NotFound() throws Exception {
        // Given - 존재하지 않는 상품번호
        OrderRequest orderRequest = createOrderRequest(999999L, 1);

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 상품 없음 에러 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"code\"");
        assertThat(responseBody).contains("\"message\"");
        assertThat(responseBody).contains("\"timestamp\"");
        
        // 구체적인 에러 정보 검증 (스냅샷)
        assertThat(responseBody).contains("\"code\":\"PRODUCT_NOT_FOUND\"");
        assertThat(responseBody).contains("상품을 찾을 수 없습니다");
        assertThat(responseBody).contains("999999");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 잘못된 JSON 요청 응답 바디 스냅샷 (400 BAD_REQUEST)")
    void testInvalidJsonResponse_ContractSnapshot_400BadRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then - JSON 파싱 에러 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"code\"");
        assertThat(responseBody).contains("\"message\"");
        
        // 구체적인 에러 정보 검증 (스냅샷)
        assertThat(responseBody).contains("\"code\":\"INTERNAL_SERVER_ERROR\"");
        assertThat(responseBody).contains("서버 내부 오류가 발생했습니다");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 무료배송 주문 응답 바디 스냅샷 (200 OK)")
    void testFreeShippingOrderResponse_ContractSnapshot_200Ok() throws Exception {
        // Given - 무료배송 기준 이상 금액 (50,000원 상품 1개)
        Product expensiveProduct = new Product(999999L, "고가 상품", new BigDecimal("50000"), 5);
        expensiveProduct = productRepository.save(expensiveProduct);
        
        OrderRequest orderRequest = createOrderRequest(999999L, 1);

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 무료배송 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        
        // 구체적인 값 검증 (스냅샷)
        assertThat(responseBody).contains("\"totalAmount\":50000");
        assertThat(responseBody).contains("\"shippingFee\":0");
        assertThat(responseBody).contains("\"totalPayment\":50000");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 다중 상품 주문 응답 바디 스냅샷 (200 OK)")
    void testMultipleProductsOrderResponse_ContractSnapshot_200Ok() throws Exception {
        // Given - 두 번째 상품 추가
        Product product2 = new Product(999998L, "두 번째 상품", new BigDecimal("20000"), 5);
        product2 = productRepository.save(product2);
        
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductNumber(PRODUCT_NUMBER);
        item1.setQuantity(2);
        
        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductNumber(999998L);
        item2.setQuantity(1);
        
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderItems(List.of(item1, item2));

        // When
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 다중 상품 주문 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        assertThat(responseBody).contains("\"orderItems\"");
        
        // 구체적인 값 검증 (스냅샷)
        assertThat(responseBody).contains("\"totalAmount\":40000"); // 10000*2 + 20000*1
        assertThat(responseBody).contains("\"shippingFee\":2500");
        assertThat(responseBody).contains("\"totalPayment\":42500");
        
        // 주문 아이템 개수 검증
        assertThat(responseBody).contains("\"productNumber\":768848");
        assertThat(responseBody).contains("\"productNumber\":999998");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    private OrderRequest createOrderRequest(Long productNumber, int quantity) {
        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setProductNumber(productNumber);
        orderItemRequest.setQuantity(quantity);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderItems(List.of(orderItemRequest));

        return orderRequest;
    }
}
