package kr.co._29cm.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testProduct1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        testProduct2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);
    }

    @Test
    void 상품_목록_조회_성공() throws Exception {
        // given
        List<Product> products = List.of(testProduct1, testProduct2);
        when(productRepository.findAll()).thenReturn(products);

        // when & then
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].productNumber").value(768848))
                .andExpect(jsonPath("$[0].name").value("[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종"))
                .andExpect(jsonPath("$[0].price").value(21000))
                .andExpect(jsonPath("$[0].stockQuantity").value(45))
                .andExpect(jsonPath("$[1].productNumber").value(759928))
                .andExpect(jsonPath("$[1].name").value("마스크 스트랩 분실방지 오염방지 목걸이"))
                .andExpect(jsonPath("$[1].price").value(2800))
                .andExpect(jsonPath("$[1].stockQuantity").value(85));
    }

    @Test
    void 상품_목록_빈_결과() throws Exception {
        // given
        when(productRepository.findAll()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasSize(0)));
    }
}
