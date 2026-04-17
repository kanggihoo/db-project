package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 상품 리뷰 엔티티
 * - 실제 구매한 사용자만이 리뷰를 남길 수 있도록 설계되었습니다.
 */
@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 리뷰 식별 PK

    @Column(nullable = false)
    private Long userId; // 작성자 식별 번호 (Users 테이블 FK)

    @Column(nullable = false)
    private Long productId; // 대상 상품 식별 번호 (Product 테이블 FK)

    @Column(nullable = false)
    private Long orderItemId; // 실제 주문 항목 식별 번호 (OrderItem 테이블 FK, 구매 인증용)

    @Column(nullable = false)
    private Integer rating; // 별점 (1~5점 사이)

    @Column(columnDefinition = "TEXT")
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private LocalDateTime createdAt; // 작성 일시
}

