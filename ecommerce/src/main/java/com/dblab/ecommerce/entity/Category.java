package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 카테고리 엔티티
 * - parent_id 셀프 조인으로 대/중/소 계층 표현 (depth: 0=대, 1=중, 2=소)
 * - Category(Parent) : Category(Child) = 1 : N 관계
 */
@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 카테고리 식별 PK

    // 최상위 카테고리는 null, 상위 카테고리의 id를 참조
    @Column(name = "parent_id")
    private Long parentId; // 상위 카테고리 ID (Self-referencing FK)

    @Column(nullable = false)
    private String name; // 카테고리 명칭

    @Column(nullable = false)
    private Integer depth; // 계층 깊이 (0: 대분류, 1: 중분류, 2: 소분류)
}

