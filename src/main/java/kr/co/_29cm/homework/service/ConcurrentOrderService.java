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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentOrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShippingCalculator shippingCalculator;
    
    // 상품번호별 락을 위한 ConcurrentHashMap
    private final Map<Long, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    public record OrderItemRequest(Long productNumber, Integer quantity) {}

    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public Order placeOrder(List<OrderItemRequest> requests) {
        log.info("주문 시작: 상품 수량 {}", requests.size());
        validateOrderRequest(requests);

        // 상품번호 순서로 정렬하여 데드락 방지
        List<OrderItemRequest> sortedRequests = requests.stream()
                .sorted(Comparator.comparing(OrderItemRequest::productNumber))
                .toList();

        Order order = new Order(generateOrderNumber(), LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Integer> stockDecreases = new HashMap<>();

        try {
            // 1단계: 재고 확인 및 락 획득
            for (OrderItemRequest req : sortedRequests) {
                ReentrantLock lock = getProductLock(req.productNumber());
                lock.lock();
                try {
                    Product product = productRepository.findByProductNumber(req.productNumber())
                            .orElseThrow(() -> new ProductNotFoundException(req.productNumber()));
                    
                    if (product.getStockQuantity() < req.quantity()) {
                        throw new InvalidOrderException(
                                String.format("재고 부족: 상품 %s, 요청수량 %d, 재고 %d", 
                                        product.getName(), req.quantity(), product.getStockQuantity()));
                    }
                    
                    stockDecreases.put(req.productNumber(), req.quantity());
                } finally {
                    lock.unlock();
                }
            }

            // 2단계: 모든 락을 다시 획득하고 재고 차감
            for (OrderItemRequest req : sortedRequests) {
                ReentrantLock lock = getProductLock(req.productNumber());
                lock.lock();
                try {
                    Product product = productRepository.findByProductNumber(req.productNumber())
                            .orElseThrow(() -> new ProductNotFoundException(req.productNumber()));
                    
                    // 재고 재확인 (다른 트랜잭션에서 변경되었을 수 있음)
                    if (product.getStockQuantity() < req.quantity()) {
                        throw new InvalidOrderException(
                                String.format("재고 부족: 상품 %s, 요청수량 %d, 재고 %d", 
                                        product.getName(), req.quantity(), product.getStockQuantity()));
                    }
                    
                    product.decreaseStock(req.quantity());
                    OrderItem item = new OrderItem(product, req.quantity());
                    order.addItem(item);
                    total = total.add(item.getTotalPrice());
                    
                } finally {
                    lock.unlock();
                }
            }

            BigDecimal paymentAmount = shippingCalculator.calculateTotalPaymentAmount(total);
            order.setPaymentAmount(paymentAmount);

            log.info("주문 완료: 주문번호 {}", order.getOrderNumber());
            return orderRepository.save(order);

        } catch (Exception e) {
            log.error("주문 실패: {}", e.getMessage());
            throw e;
        }
    }

    private ReentrantLock getProductLock(Long productNumber) {
        return productLocks.computeIfAbsent(productNumber, k -> new ReentrantLock());
    }

    private void validateOrderRequest(List<OrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidOrderException("주문 항목이 비어있습니다.");
        }
        
        // 중복 상품번호 체크
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
