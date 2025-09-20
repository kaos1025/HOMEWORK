package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.Order;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.dto.request.OrderItemRequest;
import kr.co._29cm.homework.dto.request.OrderRequest;
import kr.co._29cm.homework.repository.OrderRepository;
import kr.co._29cm.homework.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "order.strategy=pessimistic"
})
class OrderServiceAdvancedConcurrencyTest {

    @Autowired
    private OrderServiceStrategy orderServiceStrategy;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product product;
    private final Long PRODUCT_NUMBER = 768848L;
    private final int INITIAL_STOCK = 100;

    @BeforeEach
    void setUp() {
        // 재고 100개인 상품 생성
        product = new Product(PRODUCT_NUMBER, "동시성 테스트 상품", new BigDecimal("10000"), INITIAL_STOCK);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("재고 100개에서 150개 동시 주문 시 정확히 100개만 성공해야 함")
    void testConcurrentOrder_Stock100Order150_ShouldSucceedExactly100() throws Exception {
        // Given
        int concurrentOrders = 150;
        int orderQuantityPerRequest = 1;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // When - 150개의 동시 주문 요청 (각각 1개씩)
        for (int i = 0; i < concurrentOrders; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, orderQuantityPerRequest);
                    orderServiceStrategy.placeOrder(orderRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 정확히 100개 성공
        assertThat(failureCount.get()).isEqualTo(50); // 50개 실패
        
        // 재고가 정확히 0이 되어야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        
        // 주문이 정확히 100개 저장되어야 함
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(INITIAL_STOCK);
        
        // 모든 성공한 주문의 총 수량이 재고와 일치해야 함
        int totalOrderedQuantity = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToInt(OrderItem -> OrderItem.getQuantity())
                .sum();
        assertThat(totalOrderedQuantity).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("재고 100개에서 100개 동시 주문 시 모두 성공해야 함")
    void testConcurrentOrder_Stock100Order100_ShouldAllSucceed() throws Exception {
        // Given
        int concurrentOrders = 100;
        int orderQuantityPerRequest = 1;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 100개의 동시 주문 요청 (각각 1개씩)
        for (int i = 0; i < concurrentOrders; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, orderQuantityPerRequest);
                    orderServiceStrategy.placeOrder(orderRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 모두 성공
        assertThat(failureCount.get()).isEqualTo(0); // 실패 없음
        
        // 재고가 정확히 0이 되어야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 100개에서 101개 동시 주문 시 정확히 100개만 성공해야 함")
    void testConcurrentOrder_Stock100Order101_ShouldSucceedExactly100() throws Exception {
        // Given
        int concurrentOrders = 101;
        int orderQuantityPerRequest = 1;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 101개의 동시 주문 요청 (각각 1개씩)
        for (int i = 0; i < concurrentOrders; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, orderQuantityPerRequest);
                    orderServiceStrategy.placeOrder(orderRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 정확히 100개 성공
        assertThat(failureCount.get()).isEqualTo(1); // 1개 실패
        
        // 재고가 정확히 0이 되어야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 100개에서 50개씩 2개 동시 주문 시 첫 번째만 성공해야 함")
    void testConcurrentOrder_LargeQuantity_ShouldRespectStockLimit() throws Exception {
        // Given
        int orderQuantityPerRequest = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 50개씩 2개 동시 주문 요청
        for (int i = 0; i < 2; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, orderQuantityPerRequest);
                    orderServiceStrategy.placeOrder(orderRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(1); // 1개 성공
        assertThat(failureCount.get()).isEqualTo(1); // 1개 실패
        
        // 재고가 정확히 50이 남아야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(50);
        
        // 주문이 1개만 저장되어야 함
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderItems().get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고 100개에서 100개 초과 주문 시 실패해야 함")
    void testConcurrentOrder_OverStock_ShouldFail() throws Exception {
        // Given
        int orderQuantityPerRequest = 101; // 재고보다 많은 수량
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 101개 주문 요청
        CompletableFuture.runAsync(() -> {
            try {
                OrderRequest orderRequest = createOrderRequest(PRODUCT_NUMBER, orderQuantityPerRequest);
                orderServiceStrategy.placeOrder(orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }, executor);

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(0); // 성공 없음
        assertThat(failureCount.get()).isEqualTo(1); // 실패
        
        // 재고가 그대로 100이어야 함
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(INITIAL_STOCK);
        
        // 주문이 저장되지 않아야 함
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isEmpty();
    }

    private OrderRequest createOrderRequest(Long productNumber, int quantity) {
        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setProductNumber(productNumber);
        orderItemRequest.setQuantity(quantity);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderItems(List.of(orderItemRequest));

        return orderRequest;
    }
}
