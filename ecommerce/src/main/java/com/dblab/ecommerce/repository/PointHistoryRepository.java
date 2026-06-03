package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT ph
            FROM PointHistory ph
            WHERE ph.userId = :userId
            ORDER BY ph.createdAt DESC, ph.id DESC
            """)
    List<PointHistory> findFirstCursorPage(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT ph
            FROM PointHistory ph
            WHERE ph.userId = :userId
              AND (ph.createdAt < :lastCreatedAt
                   OR (ph.createdAt = :lastCreatedAt AND ph.id < :lastId))
            ORDER BY ph.createdAt DESC, ph.id DESC
            """)
    List<PointHistory> findNextCursorPage(
            @Param("userId") Long userId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);
}
