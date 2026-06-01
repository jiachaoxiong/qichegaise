package com.qcg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_case")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(length = 512)
    private String beforeUrl;

    @Column(length = 512)
    private String afterUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(length = 64)
    private String carModelName;

    private String description;

    @Builder.Default
    private Integer likes = 0;
}
