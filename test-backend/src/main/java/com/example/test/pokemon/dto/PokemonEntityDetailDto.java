package com.example.test.pokemon.dto;

import com.example.test.pokemon.Nature;
import com.example.test.pokemon.PokemonType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de réponse complet de {@link PokemonEntity}. Relations exposées en UUID (cf. Mapper) ;
 * le détail des entités liées se récupère via des endpoints dédiés (sous-ressources).
 */
public record PokemonEntityDetailDto(

    UUID id,
    String nom,
    Set<PokemonType> types,
    String description,
    String image,
    String imageChromatique,
    UUID regionId,
    UUID preEvolutionId,
    UUID evolutionId,
    Nature nature,
    Set<String> talents,
    Double poids,
    Double taille,
    Integer pv,
    Integer attaque,
    Integer defense,
    Integer attaqueSpeciale,
    Integer defenseSpeciale,
    Integer vitesse,
    Instant createdAt,
    Instant updatedAt
) {}
