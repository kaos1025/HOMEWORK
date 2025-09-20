package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.request.OrderItemRequest;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.repository.OrderRepository;
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

@SpringBootTest
@AutoConfigureWebMvc
@Transactional
@TestPropertySource(properties = {
        "order.strategy=pessimistic"
})
class OrderControllerStockConflictTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product product;
    private final Long PRODUCT_NUMBER = 768848L;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성 (재고 10개)
        product = new Product(PRODUCT_NUMBER, "테스트 상품", new BigDecimal("10000"), 10);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("재고 부족 시 409 CONFLICT 상태 코드 반환")
    void testInsufficientStock_ShouldReturn409Conflict() throws Exception {
        // Given - 재고보다 많은 수량 주문
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 15); // 재고 10개인데 15개 주문

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isConflict()) // 409 CONFLICT
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("재고가 부족합니다")))
                .andReturn();

        // 응답 내용 검증
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("INSUFFICIENT_STOCK");
        assertThat(responseBody).contains("재고가 부족합니다");
    }

    @Test
    @DisplayName("존재하지 않는 상품 주문 시 404 NOT_FOUND 상태 코드 반환")
    void testNonExistentProduct_ShouldReturn404NotFound() throws Exception {
        // Given - 존재하지 않는 상품번호
        OrderRequest orderRequest = createOrderRequest(999999L, 1);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound()) // 404 NOT_FOUND
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("상품을 찾을 수 없습니다")));
    }

    @Test
    @DisplayName("정확한 재고 수량 주문 시 200 OK 반환")
    void testExactStockOrder_ShouldReturn200Ok() throws Exception {
        // Given - 정확한 재고 수량으로 주문
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 10); // 재고와 동일한 수량

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.totalAmount").value(100000)) // 10000 * 10
                .andExpect(jsonPath("$.data.shippingFee").value(2500))
                .andExpect(jsonPath("$.data.totalPayment").value(102500)) // 100000 + 2500
                .andReturn();

        // 주문이 실제로 저장되었는지 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        
        Order savedOrder = orders.get(0);
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().get(0).getQuantity()).isEqualTo(10);
        
        // 재고가 정확히 차감되었는지 확인
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 0개 상품 주문 시 409 CONFLICT 반환")
    void testZeroStockOrder_ShouldReturn409Conflict() throws Exception {
        // Given - 재고를 모두 소진
        product.decreaseStock(10);
        productRepository.save(product);

        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 1);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isConflict()) // 409 CONFLICT
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("잘못된 요청 데이터 시 400 BAD_REQUEST 반환")
    void testInvalidRequest_ShouldReturn400BadRequest() throws Exception {
        // Given - 잘못된 요청 데이터 (수량 0)
        OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, 0);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest()) // 400 BAD_REQUEST
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
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
