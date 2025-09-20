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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisticOrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShippingCalculator shippingCalculator;

    public record OrderItemRequest(Long productNumber, Integer quantity) {}

    @Transactional
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 5, backoff = @Backoff(delay = 50, multiplier = 2))
    public Order placeOrder(List<OrderItemRequest> requests) {
        log.info("낙관적 락 주문 시작: 상품 수량 {}", requests.size());
        validateOrderRequest(requests);

        Order order = new Order(generateOrderNumber(), LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;

        // 상품번호 순서로 정렬하여 일관된 락 순서 보장
        List<OrderItemRequest> sortedRequests = requests.stream()
                .sorted(Comparator.comparing(OrderItemRequest::productNumber))
                .toList();

        for (OrderItemRequest req : sortedRequests) {
            Product product = productRepository.findByProductNumber(req.productNumber())
                    .orElseThrow(() -> new ProductNotFoundException(req.productNumber()));
            
            // 낙관적 락을 사용하여 재고 차감
            if (product.getStockQuantity() < req.quantity()) {
                throw new InvalidOrderException(
                        String.format("재고 부족: 상품 %s, 요청수량 %d, 재고 %d", 
                                product.getName(), req.quantity(), product.getStockQuantity()));
            }
            
            product.decreaseStock(req.quantity());
            OrderItem item = new OrderItem(product, req.quantity());
            order.addItem(item);
            total = total.add(item.getTotalPrice());
        }

        BigDecimal paymentAmount = shippingCalculator.calculateTotalPaymentAmount(total);
        order.setPaymentAmount(paymentAmount);

        log.info("낙관적 락 주문 완료: 주문번호 {}", order.getOrderNumber());
        return orderRepository.save(order);
    }

    private void validateOrderRequest(List<OrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidOrderException("주문 항목이 비어있습니다.");
        }
        
        Set<Long> productNumbers = new HashSet<>();
        for (OrderItemRequest request : requests) {
            if (request.productNumber() == null) {
                throw new InvalidOrderException("상품번호는 필수입니다.");
            }
            if (request.quantity() == null || request.quantity() <= 0) {
                throw new InvalidOrderException("주문 수량은 1 이상이어야 합니다.");
            }
            if (!productNumbers.add(request.productNumber())) {
                throw new InvalidOrderException("중복된 상품번호가 있습니다: " + request.productNumber());
            }
        }
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString();
    }
}
