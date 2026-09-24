package com.example.test.region;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import platform.common.entity.BaseEntity;

@Entity
@Table(name = "region")
@Getter
@Setter
public class RegionEntity extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100, unique = true)
    private String nom;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer generation;

    @Size(max = 1000)
    @Column(nullable = true, length = 1000)
    private String description;

    @Column(nullable = true)
    private String image;
}
