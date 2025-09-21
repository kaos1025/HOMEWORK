package kr.co._29cm.homework.repository;

import kr.co._29cm.homework.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductNumber(Long productNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    Optional<Product> findWithLockByProductNumber(Long productNumber);
    
    /**
     * 상품명으로 검색하여 페이징 조회
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * 재고가 있는 상품만 페이징 조회
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    Page<Product> findAvailableProducts(Pageable pageable);
}


