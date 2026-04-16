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
    private String name; // 쿠폰의 이름 (예: '신규 가입 10% 할인권')

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // 할인 방식 (RATE: 비율%, FIXED: 금액)

    @Column(nullable = false)
    private Integer discountValue; // 할인 값 (10%면 10, 5000원이면 5000)

    @Column(nullable = false)
    private Integer minOrderAmount; // 최소 주문 금액 (이 금액 이상 결제 시 사용 가능)

    @Column(nullable = false)
    private LocalDateTime startedAt; // 쿠폰 발급/사용 가능 시작 일시

    @Column(nullable = false)
    private LocalDateTime expiredAt; // 쿠폰 발급/사용 가능 종료 일시

    @Column(nullable = false)
    private Integer maxIssueCount; // 총 발급 가능 최대 수량 (선착순 쿠폰 등에 사용)

    @Column(nullable = false)
    private Integer issuedCount; // 현재까지 발급된 수량

    public enum DiscountType { RATE, FIXED }
}
