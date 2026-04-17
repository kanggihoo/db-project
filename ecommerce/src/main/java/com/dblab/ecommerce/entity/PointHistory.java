package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 포인트 이력 엔티티
 * - 특정 유저의 포인트 적립, 사용, 소멸 내역을 로그 형태로 저장합니다.
 * - 계속 쌓이는 이력 구조 → Phase 7 페이지네이션 실험의 핵심 대상입니다.
 * - Users : PointHistory = 1 : N 관계 (PointHistory가 N 측)
 */
@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 이력 식별 PK

    @Column(nullable = false)
    private Long userId; // 회원 식별 번호 (Users 테이블 FK)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type; // 변동 유형 (적립, 사용, 소멸)

    @Column(nullable = false)
    private Integer amount; // 변동 포인트 금액

    @Column(nullable = false)
    private Integer balanceAfter; // 변동 후 최종 잔액 (데이터 정합성 검증용)

    private String description; // 변동 사유 (예: 상품 구매 적립, 이벤트 참여 등)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 발생 일시

    public enum Type { EARN, USE, EXPIRE }
}

