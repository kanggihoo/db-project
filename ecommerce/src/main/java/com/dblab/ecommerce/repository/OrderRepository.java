package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserId(Long userId);

    @Query("""
            select distinct o
            from Orders o
            join fetch o.orderItems oi
            join fetch oi.productSku sku
            join fetch sku.product p
            where o.userId = :userId
            """)
    List<Orders> findByUserIdWithFetchJoin(@Param("userId") Long userId);
}
