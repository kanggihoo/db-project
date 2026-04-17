package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 회원-쿠폰 발급 내역 엔티티
 * 특정 회원이 어떤 쿠폰을 보유하고 있는지, 그리고 사용했는지를 관리합니다.
 */
@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 발급 식별 PK

    @Column(nullable = false)
    private Long userId; // 회원 식별 번호 (Users 테이블 FK)

    @Column(nullable = false)
    private Long couponId; // 쿠폰 식별 번호 (Coupon 테이블 FK)

    @Column(nullable = false)
    private Boolean isUsed; // 쿠폰 사용 여부 (true: 사용됨)

    private LocalDateTime usedAt; // 쿠폰 사용 일시 (미사용 시 null)
}

