package kr.co._29cm.homework.exception;

import kr.co._29cm.homework.controller.OrderController;
import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void 상품_없음_예외_처리() throws Exception {
        // given
        when(orderService.placeOrder(any()))
                .thenThrow(new ProductNotFoundException(999999L));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productNumber\":999999,\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("상품을 찾을 수 없습니다")));
    }

    @Test
    void 재고_부족_예외_처리() throws Exception {
        // given
        when(orderService.placeOrder(any()))
                .thenThrow(new InsufficientStockException(768848L, "테스트상품", 100, 10));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productNumber\":768848,\"quantity\":100}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("재고가 부족합니다")));
    }

    @Test
    void 유효성_검증_예외_처리() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productNumber\":null,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
