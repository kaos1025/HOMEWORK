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
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 재고 10개인 테스트 상품 생성
        testProduct = new Product(999999L, "동시성 테스트 상품", BigDecimal.valueOf(10000), 10);
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("동시 주문 시 재고 일관성 유지")
    void 동시_주문_시_재고_일관성_유지() throws Exception {
        // given
        int threadCount = 5;
        int orderQuantityPerThread = 2; // 각 스레드당 2개씩 주문
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // when - 5개 스레드가 동시에 각각 2개씩 주문 (총 10개)
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<OrderService.OrderItemRequest> requests = List.of(
                            new OrderService.OrderItemRequest(999999L, orderQuantityPerThread)
                    );
                    orderService.placeOrder(requests);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // 모든 스레드 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findByProductNumber(999999L).orElseThrow();
        
        // 성공한 주문 수 * 주문 수량 + 실패한 주문 수 * 0 = 남은 재고
        int expectedRemainingStock = 10 - (successCount.get() * orderQuantityPerThread);
        
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(expectedRemainingStock);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        
        // 재고가 음수가 되지 않았는지 확인
        assertThat(updatedProduct.getStockQuantity()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("재고 부족 시 일부 주문만 성공")
    void 재고_부족_시_일부_주문만_성공() throws Exception {
        // given
        int threadCount = 10; // 10개 스레드
        int orderQuantityPerThread = 2; // 각 스레드당 2개씩 주문 (총 20개 요청, 재고는 10개)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<OrderService.OrderItemRequest> requests = List.of(
                            new OrderService.OrderItemRequest(999999L, orderQuantityPerThread)
                    );
                    orderService.placeOrder(requests);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findByProductNumber(999999L).orElseThrow();
        
        // 재고가 모두 소진되어야 함 (10개)
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        
        // 성공한 주문은 최대 5개 (10개 재고 / 2개 주문수량)
        assertThat(successCount.get()).isLessThanOrEqualTo(5);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("단일 상품에 대한 동시 주문 시 데이터 일관성")
    void 단일_상품에_대한_동시_주문_시_데이터_일관성() throws Exception {
        // given
        int threadCount = 3;
        int totalOrderQuantity = 0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        // when - 각 스레드가 다른 수량으로 주문
        int[] orderQuantities = {1, 3, 2}; // 총 6개 주문
        for (int quantity : orderQuantities) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<OrderService.OrderItemRequest> requests = List.of(
                            new OrderService.OrderItemRequest(999999L, quantity)
                    );
                    orderService.placeOrder(requests);
                    return quantity;
                } catch (Exception e) {
                    return 0;
                }
            }, executor);
            futures.add(future);
            totalOrderQuantity += quantity;
        }

        // 모든 주문 완료 대기
        List<Integer> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        executor.shutdown();

        // then
        Product updatedProduct = productRepository.findByProductNumber(999999L).orElseThrow();
        int successfulOrderQuantity = results.stream().mapToInt(Integer::intValue).sum();
        
        // 성공한 주문 수량만큼 재고가 차감되어야 함
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(10 - successfulOrderQuantity);
        assertThat(successfulOrderQuantity).isGreaterThan(0);
        assertThat(successfulOrderQuantity).isLessThanOrEqualTo(10); // 재고 초과 주문 불가
    }
}
