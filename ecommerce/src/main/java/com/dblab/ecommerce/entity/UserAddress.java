package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 회원 배송지 주소 엔티티
 * 회원이 등록한 여러 배송지 정보를 관리하며, 기본 배송지 설정 기능을 포함합니다.
 */
@Entity
@Table(name = "user_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 배송지 식별 PK

    @Column(nullable = false)
    private Long userId; // 회원 식별 번호 (FK 역할)

    @Column(nullable = false)
    private String address; // 기본 주소 (도로명/지번)

    private String detailAddress; // 상세 주소 (동, 호수 등)

    @Column(nullable = false)
    private Boolean isDefault; // 기본 배송지 여부 (true: 기본)

    @Column(nullable = false)
    private String receiverName; // 수령인 이름

    @Column(nullable = false)
    private String receiverPhone; // 수령인 연락처
}

