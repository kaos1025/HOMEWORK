package kr.co._29cm.homework.service;

import jakarta.transaction.Transactional;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.OrderRepository;
import kr.co._29cm.homework.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public record OrderItemRequest(Long productNumber, Integer quantity) {}

    @Transactional
    public Order placeOrder(List<OrderItemRequest> requests) {
        // 제품을 productNumber로 조회하여 잠금 없이 순차적으로 차감 → 낙관적 처리 대신 동시성 고려로 비관적 잠금 적용 고려 가능
        // 여기서는 간단히 트랜잭션 내 조회-차감-저장으로 일관성 보장 (H2에서도 동작)

        Order order = new Order(generateOrderNumber(), LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest req : requests) {
            Product product = productRepository.findWithLockByProductNumber(req.productNumber)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + req.productNumber));
            product.decreaseStock(req.quantity);

            OrderItem item = new OrderItem(product, req.quantity);
            order.addItem(item);
            total = total.add(item.getTotalPrice());
        }

        BigDecimal shipping = total.compareTo(BigDecimal.valueOf(50000)) < 0 ? BigDecimal.valueOf(2500) : BigDecimal.ZERO;
        order.setPaymentAmount(total.add(shipping));

        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString();
    }
}


