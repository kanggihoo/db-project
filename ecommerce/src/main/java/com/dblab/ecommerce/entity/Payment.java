package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 결제 정보
 * - created_at: 결제 시도 시각
 * - paid_at: 실제 결제 완료 시각 (PENDING 상태에서는 null)
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
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Method method;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private String pgTransactionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    public enum Method { CARD, KAKAO_PAY, NAVER_PAY }
    public enum Status { PENDING, COMPLETED, FAILED, REFUNDED }
}
