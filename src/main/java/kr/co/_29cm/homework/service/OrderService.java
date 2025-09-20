package kr.co._29cm.homework.service;

import jakarta.transaction.Transactional;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.exception.InvalidOrderException;
import kr.co._29cm.homework.exception.ProductNotFoundException;
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
    private final ShippingCalculator shippingCalculator;

    public record OrderItemRequest(Long productNumber, Integer quantity) {}

    @Transactional
    public Order placeOrder(List<OrderItemRequest> requests) {
        validateOrderRequest(requests);

        Order order = new Order(generateOrderNumber(), LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest req : requests) {
            Product product = productRepository.findWithLockByProductNumber(req.productNumber)
                    .orElseThrow(() -> new ProductNotFoundException(req.productNumber));
            
            try {
                product.decreaseStock(req.quantity);
            } catch (kr.co._29cm.homework.exception.InsufficientStockException e) {
                throw new InvalidOrderException(e.getMessage(), e);
            }

            OrderItem item = new OrderItem(product, req.quantity);
            order.addItem(item);
            total = total.add(item.getTotalPrice());
        }

        BigDecimal paymentAmount = shippingCalculator.calculateTotalPaymentAmount(total);
        order.setPaymentAmount(paymentAmount);

        return orderRepository.save(order);
    }

    private void validateOrderRequest(List<OrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidOrderException("주문 항목이 비어있습니다.");
        }
        
        for (OrderItemRequest request : requests) {
            if (request.productNumber() == null) {
                throw new InvalidOrderException("상품번호는 필수입니다.");
            }
            if (request.quantity() == null || request.quantity() <= 0) {
                throw new InvalidOrderException("주문 수량은 1 이상이어야 합니다.");
            }
        }
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString();
    }
}


