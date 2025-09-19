package kr.co._29cm.homework;

import kr.co._29cm.homework.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void 주문_성공_및_배송비_계산() {
        var order = orderService.placeOrder(List.of(
                new OrderService.OrderItemRequest(768848L, 1),
                new OrderService.OrderItemRequest(759928L, 2)
        ));

        assertThat(order.getOrderNumber()).isNotBlank();
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getPaymentAmount()).isPositive();
    }
}


