package kr.co._29cm.homework.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Product;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 상품 API 계약 테스트: API 응답 바디 스냅샷을 고정하여 회귀 방지
 * 200 응답에 대한 정확한 JSON 구조 검증
 */
@SpringBootTest
@AutoConfigureWebMvc
@Transactional
@TestPropertySource(properties = {
        "shipping.policy.free-shipping-threshold=50000",
        "shipping.policy.fee=2500"
})
class ProductApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 상품들 생성
        Product product1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", new BigDecimal("21000"), 45);
        Product product2 = new Product(748394L, "무료배송 상품", new BigDecimal("50000"), 10);
        Product product3 = new Product(123456L, "저가 상품", new BigDecimal("1000"), 100);
        
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
    }

    @Test
    @DisplayName("계약 테스트: 상품 목록 조회 응답 바디 스냅샷 (200 OK)")
    void testProductListResponse_ContractSnapshot_200Ok() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        assertThat(responseBody).contains("\"products\"");
        
        // 상품 정보 구조 검증
        assertThat(responseBody).contains("\"id\"");
        assertThat(responseBody).contains("\"productNumber\"");
        assertThat(responseBody).contains("\"name\"");
        assertThat(responseBody).contains("\"price\"");
        assertThat(responseBody).contains("\"stockQuantity\"");
        
        // 구체적인 상품 정보 검증 (스냅샷)
        assertThat(responseBody).contains("\"productNumber\":768848");
        assertThat(responseBody).contains("\"name\":\"[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종\"");
        assertThat(responseBody).contains("\"price\":21000");
        assertThat(responseBody).contains("\"stockQuantity\":45");
        
        assertThat(responseBody).contains("\"productNumber\":748394");
        assertThat(responseBody).contains("\"name\":\"무료배송 상품\"");
        assertThat(responseBody).contains("\"price\":50000");
        assertThat(responseBody).contains("\"stockQuantity\":10");
        
        assertThat(responseBody).contains("\"productNumber\":123456");
        assertThat(responseBody).contains("\"name\":\"저가 상품\"");
        assertThat(responseBody).contains("\"price\":1000");
        assertThat(responseBody).contains("\"stockQuantity\":100");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 배송비 정책 조회 응답 바디 스냅샷 (200 OK)")
    void testShippingPolicyResponse_ContractSnapshot_200Ok() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/products/shipping-policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        
        // 배송비 정책 구조 검증
        assertThat(responseBody).contains("\"freeShippingThreshold\"");
        assertThat(responseBody).contains("\"fee\"");
        
        // 구체적인 배송비 정책 값 검증 (스냅샷)
        assertThat(responseBody).contains("\"freeShippingThreshold\":50000");
        assertThat(responseBody).contains("\"fee\":2500");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 빈 상품 목록 응답 바디 스냅샷 (200 OK)")
    void testEmptyProductListResponse_ContractSnapshot_200Ok() throws Exception {
        // Given - 모든 상품 삭제
        productRepository.deleteAll();

        // When
        MvcResult result = mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 빈 목록 응답 바디 스냅샷 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // JSON 구조 검증
        assertThat(responseBody).contains("\"success\":true");
        assertThat(responseBody).contains("\"data\"");
        assertThat(responseBody).contains("\"products\"");
        
        // 빈 배열 검증
        assertThat(responseBody).contains("\"products\":[]");
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 테스트: 상품 목록 응답 순서 검증")
    void testProductListOrder_ContractSnapshot() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then - 상품 목록 순서 검증 (상품번호 오름차순)
        String responseBody = result.getResponse().getContentAsString();
        
        // 상품번호가 오름차순으로 정렬되어 있는지 확인
        int index123456 = responseBody.indexOf("123456");
        int index748394 = responseBody.indexOf("748394");
        int index768848 = responseBody.indexOf("768848");
        
        assertThat(index123456).isLessThan(index748394);
        assertThat(index748394).isLessThan(index768848);
    }

    @Test
    @DisplayName("계약 테스트: 상품 응답 필드 타입 검증")
    void testProductResponseFieldTypes_ContractSnapshot() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then - 필드 타입 검증
        String responseBody = result.getResponse().getContentAsString();
        
        // 숫자 필드들이 따옴표 없이 반환되는지 확인
        assertThat(responseBody).contains("\"id\":1,"); // 숫자
        assertThat(responseBody).contains("\"productNumber\":768848,"); // 숫자
        assertThat(responseBody).contains("\"price\":21000,"); // 숫자
        assertThat(responseBody).contains("\"stockQuantity\":45"); // 숫자
        
        // 문자열 필드들이 따옴표로 감싸져 있는지 확인
        assertThat(responseBody).contains("\"name\":\"[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종\","); // 문자열
        
        // 응답 바디 전체 구조 검증 (JSON 파싱 가능)
        assertThatCode(() -> objectMapper.readTree(responseBody)).doesNotThrowAnyException();
    }
}
