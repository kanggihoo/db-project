package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 환불 정보 엔티티
 * - 결제 취소 및 반품으로 인한 환불 내역을 관리합니다.
 * - 특히, 전체 주문이 아닌 '부분 환불(특정 상품만 취소)'을 지원하기 위해 설계되었습니다.
 */
@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 환불 식별 PK

    @Column(nullable = false)
    private Long paymentId; // 어떤 결제 건에 대한 환불인지 (Payment 테이블 FK)

    @Column(nullable = false)
    private Long orderItemId; // 실제로 환불 처리되는 '개별 상품' (OrderItem 테이블 FK)

    @Column(nullable = false)
    private Integer amount; // 환불되는 금액

    private String reason; // 환불 사유 (예: 단순 변심, 상품 파손 등)

    @Column(nullable = false)
    private String status; // 환불 상태 (예: REQUESTED, PROCESSING, COMPLETED, REJECTED)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 환불 접수(생성) 일시
}

