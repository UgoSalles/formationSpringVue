package com.example.test.region.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.common.validation.ValidationGroups;

/**
 * DTO de création de {@link RegionEntity}. Relations aplaties en UUID (cf. Mapper).
 */
public record RegionEntityCreateDto(

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(max = 100, groups = ValidationGroups.Create.class)
    String nom,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer generation,

    @Size(max = 1000, groups = ValidationGroups.Create.class)
    String description,

    String image
) {}
