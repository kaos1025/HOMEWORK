package kr.co._29cm.homework.repository;

import kr.co._29cm.homework.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByKeyValue(String keyValue);

    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :now")
    void deleteExpiredKeys(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ik) > 0 FROM IdempotencyKey ik WHERE ik.keyValue = :keyValue AND ik.expiresAt > :now")
    boolean existsByKeyValueAndNotExpired(@Param("keyValue") String keyValue, @Param("now") LocalDateTime now);
}
