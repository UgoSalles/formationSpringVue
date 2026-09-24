package com.example.test.pokemon.dto;

import com.example.test.pokemon.Nature;
import com.example.test.pokemon.PokemonType;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.common.validation.ValidationGroups;

/**
 * DTO de mise à jour partielle de {@link PokemonEntity}. Relations aplaties en UUID (cf. Mapper).
 * Seuls les champs non nuls sont appliqués. {@code id} est obligatoire.
 */
public record PokemonEntityUpdateDto(

    @NotNull(groups = ValidationGroups.Update.class)
    UUID id,

    @Size(max = 100, groups = ValidationGroups.Update.class)
    String nom,

    @Size(min = 1, max = 2, groups = ValidationGroups.Update.class)
    Set<PokemonType> types,

    @Size(max = 1000, groups = ValidationGroups.Update.class)
    String description,

    String image,

    String imageChromatique,

    UUID regionId,

    UUID preEvolutionId,

    UUID evolutionId,

    Nature nature,

    @Size(min = 1, max = 3, groups = ValidationGroups.Update.class)
    Set<String> talents,

    @Positive(groups = ValidationGroups.Update.class)
    Double poids,

    @Positive(groups = ValidationGroups.Update.class)
    Double taille,

    @Positive(groups = ValidationGroups.Update.class)
    Integer pv,

    @Positive(groups = ValidationGroups.Update.class)
    Integer attaque,

    @Positive(groups = ValidationGroups.Update.class)
    Integer defense,

    @Positive(groups = ValidationGroups.Update.class)
    Integer attaqueSpeciale,

    @Positive(groups = ValidationGroups.Update.class)
    Integer defenseSpeciale,

    @Positive(groups = ValidationGroups.Update.class)
    Integer vitesse
) {}
