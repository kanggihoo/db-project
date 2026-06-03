package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.Orders;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserId(Long userId);

    List<Orders> findByStatus(Orders.Status status);

    @Query("""
            select distinct o
            from Orders o
            join fetch o.orderItems oi
            join fetch oi.productSku sku
            join fetch sku.product p
            where o.userId = :userId
            """)
    List<Orders> findByUserIdWithFetchJoin(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.productSku",
            "orderItems.productSku.product"
    })
    List<Orders> findGraphByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Orders o set o.status = :newStatus where o.status = :oldStatus")
    int bulkUpdateStatus(@Param("oldStatus") Orders.Status oldStatus,
                         @Param("newStatus") Orders.Status newStatus);
}
