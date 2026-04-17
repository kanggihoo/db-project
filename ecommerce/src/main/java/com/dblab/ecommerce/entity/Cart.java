package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 장바구니 마스터 엔티티
 * - 특정 회원(Users)에게 할당된 고유한 장바구니를 의미합니다.
 * - 사용자가 로그아웃했다가 들어와도 담아둔 물건이 유지되도록 기능을 제공합니다.
 */
@Entity
@Table(name = "cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 장바구니 식별 PK

    @Column(nullable = false)
    private Long userId; // 장바구니 소유자 식별 번호 (Users 테이블 FK)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 장바구니 생성 일시
}

