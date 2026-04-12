package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 배송 추적 이력
 * - 배송 이력이 계속 쌓이는 구조 → Phase 7 페이지네이션 실험에 최적
 */
@Entity
@Table(name = "delivery_tracking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deliveryId;

    @Column(nullable = false)
    private String status;

    private String location;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
