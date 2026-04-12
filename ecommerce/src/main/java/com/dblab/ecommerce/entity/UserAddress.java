package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** 회원 배송지 주소 */
@Entity
@Table(name = "user_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String address;

    private String detailAddress;

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhone;
}
