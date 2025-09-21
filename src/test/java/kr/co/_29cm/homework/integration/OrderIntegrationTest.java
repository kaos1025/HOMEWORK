package kr.co._29cm.homework.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.Product;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 관련 통합 테스트
 * 실제 데이터베이스와 전체 Spring Context를 사용한 End-to-End 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("주문 통합 테스트")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 데이터 직접 생성
        testProduct1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        testProduct2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);
        
        productRepository.save(testProduct1);
        productRepository.save(testProduct2);
    }

    @Test
    @DisplayName("전체 주문 플로우 통합 테스트")
    @Transactional
    void 전체_주문_플로우_통합_테스트() throws Exception {
        // given
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(768848L, 1),
                new OrderRequest.OrderItemRequest(759928L, 2)
        ));

        // when - 주문 생성
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.paymentAmount").value(29100)) // 21000 + 2800*2 + 2500
                .andReturn().getResponse().getContentAsString();

        // then - 데이터베이스 검증
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        
        Order savedOrder = orders.get(0);
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getPaymentAmount()).isEqualTo(BigDecimal.valueOf(29100));

        // 재고 차감 확인
        Optional<Product> updatedProduct1 = productRepository.findByProductNumber(768848L);
        Optional<Product> updatedProduct2 = productRepository.findByProductNumber(759928L);
        
        assertThat(updatedProduct1).isPresent();
        assertThat(updatedProduct1.get().getStockQuantity()).isEqualTo(44); // 45 - 1
        
        assertThat(updatedProduct2).isPresent();
        assertThat(updatedProduct2.get().getStockQuantity()).isEqualTo(83); // 85 - 2
    }

    @Test
    @DisplayName("주문 생성 후 조회 통합 테스트")
    @Transactional
    void 주문_생성_후_조회_통합_테스트() throws Exception {
        // given - 주문 생성
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(768848L, 1)
        ));

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 주문번호 추출 (실제 구현에서는 JSON 파싱 필요)
        List<Order> orders = orderRepository.findAll();
        String orderNumber = orders.get(0).getOrderNumber();

        // when - 주문 상세 조회
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").value(org.hamcrest.Matchers.hasSize(1)));

        // when - 주문 목록 조회
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("재고 부족 시나리오 통합 테스트")
    void 재고_부족_시나리오_통합_테스트() throws Exception {
        // given - 재고보다 많은 수량 주문
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(
                new OrderRequest.OrderItemRequest(768848L, 100) // 재고 45개보다 많음
        ));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER"));

        // 주문이 생성되지 않았는지 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isEmpty();

        // 재고가 변경되지 않았는지 확인
        Optional<Product> product = productRepository.findByProductNumber(768848L);
        assertThat(product).isPresent();
        assertThat(product.get().getStockQuantity()).isEqualTo(45); // 변경되지 않음
    }

    @Test
    @DisplayName("상품 페이징 조회 통합 테스트")
    void 상품_페이징_조회_통합_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name")
                        .param("direction", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    @DisplayName("상품 검색 기능 통합 테스트")
    void 상품_검색_기능_통합_테스트() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products")
                        .param("search", "스탠리")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", org.hamcrest.Matchers.containsString("STANLEY")));
    }
}
