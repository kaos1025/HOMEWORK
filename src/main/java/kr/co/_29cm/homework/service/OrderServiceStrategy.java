package kr.co._29cm.homework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderServiceStrategy {

    private final OrderService pessimisticOrderService;
    private final ConcurrentOrderService concurrentOrderService;
    private final OptimisticOrderService optimisticOrderService;

    @Value("${order.strategy:optimistic}")
    private String strategy;

    @Autowired
    public OrderServiceStrategy(
            OrderService pessimisticOrderService,
            ConcurrentOrderService concurrentOrderService,
            OptimisticOrderService optimisticOrderService) {
        this.pessimisticOrderService = pessimisticOrderService;
        this.concurrentOrderService = concurrentOrderService;
        this.optimisticOrderService = optimisticOrderService;
    }

    public OrderService.OrderServiceInterface getOrderService() {
        return switch (strategy.toLowerCase()) {
            case "pessimistic" -> new OrderServiceAdapter(pessimisticOrderService);
            case "concurrent" -> new OrderServiceAdapter(concurrentOrderService);
            case "optimistic" -> new OrderServiceAdapter(optimisticOrderService);
            default -> new OrderServiceAdapter(optimisticOrderService);
        };
    }

    public kr.co._29cm.homework.domain.Order placeOrder(List<OrderService.OrderItemRequest> requests) {
        return getOrderService().placeOrder(requests);
    }

    // 어댑터 클래스
    private static class OrderServiceAdapter implements OrderService.OrderServiceInterface {
        private final Object orderService;

        public OrderServiceAdapter(Object orderService) {
            this.orderService = orderService;
        }

        @Override
        public kr.co._29cm.homework.domain.Order placeOrder(List<OrderService.OrderItemRequest> requests) {
            if (orderService instanceof OrderService) {
                return ((OrderService) orderService).placeOrder(requests);
            } else if (orderService instanceof ConcurrentOrderService) {
                return ((ConcurrentOrderService) orderService).placeOrder(requests);
            } else if (orderService instanceof OptimisticOrderService) {
                return ((OptimisticOrderService) orderService).placeOrder(requests);
            }
            throw new IllegalArgumentException("지원하지 않는 주문 서비스 타입입니다");
        }
    }
}
