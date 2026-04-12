package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 상품 리뷰 */
@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long orderItemId;

    // 1 ~ 5 사이 CHECK 제약은 DDL에서 관리
    @Column(nullable = false)
    private Integer rating;

    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
