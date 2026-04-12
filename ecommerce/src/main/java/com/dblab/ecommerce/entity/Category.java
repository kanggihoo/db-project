package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 카테고리 엔티티
 * - parent_id 셀프 조인으로 대/중/소 계층 표현 (depth: 0=대, 1=중, 2=소)
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
    private Long id;

    // 최상위 카테고리는 null
    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer depth;
}
