package com.example.test.region.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.common.validation.ValidationGroups;

/**
 * DTO de mise à jour partielle de {@link RegionEntity}. Relations aplaties en UUID (cf. Mapper).
 * Seuls les champs non nuls sont appliqués. {@code id} est obligatoire.
 */
public record RegionEntityUpdateDto(

    @NotNull(groups = ValidationGroups.Update.class)
    UUID id,

    @Size(max = 100, groups = ValidationGroups.Update.class)
    String nom,

    @Positive(groups = ValidationGroups.Update.class)
    Integer generation,

    @Size(max = 1000, groups = ValidationGroups.Update.class)
    String description,

    String image
) {}
