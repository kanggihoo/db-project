package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 배송 정보 엔티티
 * - 해당 주문(Orders)의 현재 배송 현황을 관리합니다.
 * - Orders : Delivery = 1 : 1 (또는 주문 분할 시 1 : N) 관계
 */
@Entity
@Table(name = "delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 배송 식별 PK

    @Column(nullable = false)
    private Long orderId; // 연관된 주문 식별 번호 (Orders 테이블 FK)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status; // 배송 상태 (상품준비중, 발송완료, 배송중, 배송완료)

    // PREPARING 상태에서는 null, SHIPPED 상태로 넘어갈 때 운송장 번호 부여
    private String trackingNumber; // 택배사 운송장 번호 (배송 추적 API 연동용)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 배송 데이터 생성 일시

    public enum Status { PREPARING, SHIPPED, DELIVERING, DELIVERED }
}

