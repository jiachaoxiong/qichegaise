package com.qcg.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "car_model")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String brandName;

    @Column(nullable = false, length = 64)
    private String modelName;

    @Column(length = 8)
    private String year;

    @Column(length = 16)
    private String bodyType;

    @Column(length = 512)
    private String imageUrl;

    @Builder.Default
    private Boolean isActive = true;
}
