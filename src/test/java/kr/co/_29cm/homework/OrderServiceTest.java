package kr.co._29cm.homework;

import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.ProductRepository;
import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 데이터 생성
        Product product1 = new Product(768848L, "[STANLEY] GO CERAMIVAC 진공 텀블러/보틀 3종", 
                BigDecimal.valueOf(21000), 45);
        Product product2 = new Product(759928L, "마스크 스트랩 분실방지 오염방지 목걸이", 
                BigDecimal.valueOf(2800), 85);
        
        productRepository.save(product1);
        productRepository.save(product2);
    }

    @Test
    void 주문_성공_및_배송비_계산() {
        var order = orderService.placeOrder(List.of(
                new OrderService.OrderItemRequest(768848L, 1),
                new OrderService.OrderItemRequest(759928L, 2)
        ));

        assertThat(order.getOrderNumber()).isNotBlank();
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getPaymentAmount()).isPositive();
        
        // 구체적인 금액 검증 (21000 + 2800*2 + 2500 = 29100)
        assertThat(order.getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(29100));
    }
}


