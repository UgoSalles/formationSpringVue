package com.example.test.region.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de réponse complet de {@link RegionEntity}. Relations exposées en UUID (cf. Mapper) ;
 * le détail des entités liées se récupère via des endpoints dédiés (sous-ressources).
 */
public record RegionEntityDetailDto(

    UUID id,
    String nom,
    Integer generation,
    String description,
    String image,
    Instant createdAt,
    Instant updatedAt
) {}
