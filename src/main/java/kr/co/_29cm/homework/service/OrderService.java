package kr.co._29cm.homework.service;

import org.springframework.transaction.annotation.Transactional;
import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.OrderItem;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.exception.InvalidOrderException;
import kr.co._29cm.homework.exception.ProductNotFoundException;
import kr.co._29cm.homework.repository.OrderRepository;
import kr.co._29cm.homework.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShippingCalculator shippingCalculator;

    public record OrderItemRequest(Long productNumber, Integer quantity) {}

    @Transactional
    public Order placeOrder(List<OrderItemRequest> requests) {
        log.info("주문 시작: 상품 수량 {}", requests.size());
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

        log.info("주문 완료: 주문번호 {}", order.getOrderNumber());
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

    /**
     * 주문번호로 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<Order> findOrderByOrderNumber(String orderNumber) {
        log.debug("주문 조회: 주문번호 {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    /**
     * 주문 목록을 페이징으로 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public Page<Order> findOrders(Pageable pageable) {
        log.debug("주문 목록 조회: 페이지 {}, 크기 {}", pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAllByOrderByOrderedAtDesc(pageable);
    }
    
    /**
     * 특정 기간의 주문을 페이징으로 조회
     */
    @Transactional(readOnly = true)
    public Page<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("기간별 주문 조회: {} ~ {}", startDate, endDate);
        return orderRepository.findOrdersByDateRange(startDate, endDate, pageable);
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString();
    }
}


