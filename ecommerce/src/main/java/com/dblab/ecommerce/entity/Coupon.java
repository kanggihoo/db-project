package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 쿠폰 마스터 테이블
 * - discount_type: RATE(비율 할인), FIXED(정액 할인)
 * - issued_count: 발급된 쿠폰 수 — max_issue_count 초과 발급 방지용
 */
@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    @Column(nullable = false)
    private Integer minOrderAmount;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private Integer maxIssueCount;

    @Column(nullable = false)
    private Integer issuedCount;

    public enum DiscountType { RATE, FIXED }
}
