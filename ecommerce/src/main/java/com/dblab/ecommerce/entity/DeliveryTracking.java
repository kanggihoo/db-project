package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 배송 위치 추적 이력(Log) 엔티티
 * - Delivery 테이블이 '최신 상태'만 유지하는 것과 달리, 이 테이블은 시간에 따른 배송 경로의 '타임라인'을 기록합니다.
 * - 택배사(PG사 아님)에서 상태가 변경될 때마다 새로운 로그가 계속 쌓이는 Append-only 구조입니다.
 * - 데이터가 방대해지므로 Phase 7 페이지네이션 실험에 최적화된 대상입니다.
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
    private Long id; // 배송 추적 이력 식별 PK

    @Column(nullable = false)
    private Long deliveryId; // 어떤 배송건에 대한 로깅 정보인지 (Delivery 테이블 FK)

    @Column(nullable = false)
    private String status; // 당시의 상태 (집화처리, 간선상차, 간선하차 등 상세 상태)

    private String location; // 당시 택배의 물리적 위치 (예: "옥천 HUB", "강남 대리점")

    @Column(nullable = false)
    private LocalDateTime createdAt; // 해당 이벤트 발생 일시
}

