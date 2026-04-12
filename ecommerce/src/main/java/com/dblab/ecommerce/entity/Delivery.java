package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 배송 정보 */
@Entity
@Table(name = "delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Status { PREPARING, SHIPPED, DELIVERING, DELIVERED }
}
