package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 결제 정보 엔티티
 * - 결제 Gateway(PG)사로부터 받은 승인 정보를 기록합니다.
 * - Orders.finalPrice와 이 테이블의 amount를 대조하여 결제 정합성을 검증합니다.
 */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 결제 식별 PK

    @Column(nullable = false)
    private Long orderId; // 연관된 주문 식별 번호 (Orders 테이블 FK)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Method method; // 결제 수단 (신용카드, 카카오페이 등)

    @Column(nullable = false)
    private Integer amount; // PG사를 통해 실제로 결제된 금액 (Orders.finalPrice와 일치해야 함)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status; // 결제 상태 (진행중, 완료, 실패, 환불됨)

    private String pgTransactionId; // PG사에서 발급한 고유 거래 번호 (취소/정산 시 필요)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 결제 시도 시각

    private LocalDateTime paidAt; // 실제 PG 승인이 완료된 시각 (PENDING 상태에서는 null)

    public enum Method {
        CARD, KAKAO_PAY, NAVER_PAY
    }

    public enum Status {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
