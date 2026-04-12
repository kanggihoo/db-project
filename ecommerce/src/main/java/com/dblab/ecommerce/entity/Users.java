package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 엔티티
 * - grade: BRONZE(60%), SILVER(25%), GOLD(10%), VIP(5%) 분포로 시딩
 * - point_balance: point_history와 별도로 캐싱된 현재 포인트 잔액
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Grade grade;

    @Column(nullable = false)
    private Integer pointBalance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Gender { MALE, FEMALE, OTHER }
    public enum Grade { BRONZE, SILVER, GOLD, VIP }
}
