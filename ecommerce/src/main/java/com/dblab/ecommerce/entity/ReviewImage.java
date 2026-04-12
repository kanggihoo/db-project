package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** 리뷰 첨부 이미지 */
@Entity
@Table(name = "review_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewId;

    @Column(nullable = false)
    private String imageUrl;
}
