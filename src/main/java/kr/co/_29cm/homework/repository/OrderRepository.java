package kr.co._29cm.homework.repository;

import kr.co._29cm.homework.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * 주문 목록을 최신순으로 페이징 조회
     */
    Page<Order> findAllByOrderByOrderedAtDesc(Pageable pageable);
    
    /**
     * 특정 기간의 주문을 페이징 조회
     */
    @Query("SELECT o FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate ORDER BY o.orderedAt DESC")
    Page<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}


