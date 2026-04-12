package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 상품 엔티티
 * - status: ON_SALE(80%), SOLD_OUT(15%), DISCONTINUED(5%) 분포로 시딩
 * - is_deleted: Soft Delete용 (Phase 2 부분 인덱스 실험 대상)
 */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer basePrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Status { ON_SALE, SOLD_OUT, DISCONTINUED }
}
