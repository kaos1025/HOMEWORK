package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.response.ProductResponse;
import kr.co._29cm.homework.mapper.ProductMapper;
import kr.co._29cm.homework.repository.ProductRepository;
import kr.co._29cm.homework.service.ShippingCalculator;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController의 페이징 기능 테스트
 */
@WebMvcTest(ProductController.class)
@DisplayName("상품 컨트롤러 페이징 테스트")
class ProductControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;
    
    @MockBean
    private ProductMapper productMapper;
    
    @MockBean
    private ShippingCalculator shippingCalculator;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct1;
    private Product testProduct2;
    private ProductResponse productResponse1;
    private ProductResponse productResponse2;

    @BeforeEach
    void setUp() {
        testProduct1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        testProduct2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);

        productResponse1 = ProductResponse.builder()
                .id(1L)
                .productNumber(768848L)
                .name("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종")
                .price(BigDecimal.valueOf(21000))
                .stockQuantity(45)
                .build();
                
        productResponse2 = ProductResponse.builder()
                .id(2L)
                .productNumber(759928L)
                .name("마스크 스트랩 분실방지 오염방지 목걸이")
                .price(BigDecimal.valueOf(2800))
                .stockQuantity(85)
                .build();
    }

    @Test
    @DisplayName("상품 목록 페이징 조회 성공")
    void 상품_목록_페이징_조회_성공() throws Exception {
        // given
        List<Product> products = List.of(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toResponse(testProduct1)).thenReturn(productResponse1);
        when(productMapper.toResponse(testProduct2)).thenReturn(productResponse2);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("상품명 검색 기능 테스트")
    void 상품명_검색_기능_테스트() throws Exception {
        // given
        List<Product> products = List.of(testProduct1);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(testProduct1)).thenReturn(productResponse1);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("search", "스탠리")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("재고 있는 상품만 필터링 테스트")
    void 재고_있는_상품만_필터링_테스트() throws Exception {
        // given
        List<Product> products = List.of(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAvailableProducts(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse1);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("availableOnly", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("정렬 기능 테스트 - 가격 내림차순")
    void 정렬_기능_테스트_가격_내림차순() throws Exception {
        // given
        List<Product> products = List.of(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse1);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("sort", "price")
                        .param("direction", "desc")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("배송비 정책 조회 성공")
    void 배송비_정책_조회_성공() throws Exception {
        // given
        ShippingCalculator.ShippingPolicy policy = 
            new ShippingCalculator.ShippingPolicy(BigDecimal.valueOf(50000), BigDecimal.valueOf(2500));
        when(shippingCalculator.getShippingPolicy()).thenReturn(policy);

        // when & then
        mockMvc.perform(get("/api/products/shipping-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.freeShippingThreshold").value(50000))
                .andExpect(jsonPath("$.data.shippingFee").value(2500));
    }
}
