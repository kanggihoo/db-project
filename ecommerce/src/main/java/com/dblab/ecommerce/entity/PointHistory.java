package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 포인트 이력 테이블
 * - 계속 쌓이는 이력 구조 → Phase 7 페이지네이션 실험 대상
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
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer balanceAfter;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Type { EARN, USE, EXPIRE }
}
