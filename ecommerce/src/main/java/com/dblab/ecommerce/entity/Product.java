package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 상품 엔티티
 * - status: ON_SALE(80%), SOLD_OUT(15%), DISCONTINUED(5%) 분포로 시딩
 * - is_deleted: Soft Delete용 (Phase 2 부분 인덱스 실험 대상)
 * - Category : Product = 1 : N 관계 (Product가 N 측)
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
    private Long id; // 상품 식별 PK

    @Column(nullable = false)
    private Long categoryId; // 카테고리 식별 번호 (Category 테이블 FK)

    @Column(nullable = false)
    private String name; // 상품명

    private String description; // 상품 설명

    @Column(nullable = false)
    private Integer basePrice; // 기본 판매 가격

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status; // 상품 판매 상태 (판매중, 품절, 단종)

    @Column(nullable = false)
    private Boolean isDeleted; // 삭제 여부 (Soft Delete)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 등록 일시

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정 일시

    public enum Status { ON_SALE, SOLD_OUT, DISCONTINUED }
}

