package kr.co._29cm.homework.service;

import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ConcurrencyComparisonTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ConcurrentOrderService concurrentOrderService;

    @Autowired
    private OptimisticOrderService optimisticOrderService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product(999999L, "동시성 테스트 상품", BigDecimal.valueOf(10000), 100);
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("기본 주문 서비스 동시성 테스트")
    void 기본_주문_서비스_동시성_테스트() throws Exception {
        runConcurrencyTest(orderService, "기본 주문 서비스");
    }

    @Test
    @DisplayName("동시성 개선 주문 서비스 테스트")
    void 동시성_개선_주문_서비스_테스트() throws Exception {
        runConcurrencyTest(concurrentOrderService, "동시성 개선 주문 서비스");
    }

    @Test
    @DisplayName("낙관적 락 주문 서비스 테스트")
    void 낙관적_락_주문_서비스_테스트() throws Exception {
        runConcurrencyTest(optimisticOrderService, "낙관적 락 주문 서비스");
    }

    private void runConcurrencyTest(OrderServiceInterface orderService, String serviceName) throws Exception {
        // given
        int threadCount = 20;
        int orderQuantityPerThread = 5; // 각 스레드당 5개씩 주문 (총 100개 요청, 재고는 100개)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalProcessedQuantity = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<OrderService.OrderItemRequest> requests = List.of(
                            new OrderService.OrderItemRequest(999999L, orderQuantityPerThread)
                    );
                    orderService.placeOrder(requests);
                    successCount.incrementAndGet();
                    totalProcessedQuantity.addAndGet(orderQuantityPerThread);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        long endTime = System.currentTimeMillis();

        // then
        Product updatedProduct = productRepository.findByProductNumber(999999L).orElseThrow();
        int expectedRemainingStock = 100 - totalProcessedQuantity.get();

        System.out.printf("\n=== %s 결과 ===\n", serviceName);
        System.out.printf("성공한 주문: %d개\n", successCount.get());
        System.out.printf("실패한 주문: %d개\n", failureCount.get());
        System.out.printf("처리된 총 수량: %d개\n", totalProcessedQuantity.get());
        System.out.printf("남은 재고: %d개\n", updatedProduct.getStockQuantity());
        System.out.printf("실행 시간: %dms\n", endTime - startTime);
        System.out.println("==================\n");

        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedRemainingStock);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        assertThat(updatedProduct.getStockQuantity()).isGreaterThanOrEqualTo(0);
    }

    // 공통 인터페이스
    interface OrderServiceInterface {
        kr.co._29cm.homework.domain.Order placeOrder(List<OrderService.OrderItemRequest> requests);
    }
}
