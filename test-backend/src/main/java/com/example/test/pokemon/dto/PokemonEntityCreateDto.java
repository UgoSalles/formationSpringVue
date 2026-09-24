package com.example.test.pokemon.dto;

import com.example.test.pokemon.Nature;
import com.example.test.pokemon.PokemonType;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.common.validation.ValidationGroups;

/**
 * DTO de création de {@link PokemonEntity}. Relations aplaties en UUID (cf. Mapper).
 */
public record PokemonEntityCreateDto(

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(max = 100, groups = ValidationGroups.Create.class)
    String nom,

    @NotEmpty(groups = ValidationGroups.Create.class)
    @Size(min = 1, max = 2, groups = ValidationGroups.Create.class)
    Set<PokemonType> types,

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(max = 1000, groups = ValidationGroups.Create.class)
    String description,

    String image,

    String imageChromatique,

    @NotNull(groups = ValidationGroups.Create.class)
    UUID regionId,

    UUID preEvolutionId,

    UUID evolutionId,

    @NotNull(groups = ValidationGroups.Create.class)
    Nature nature,

    @NotEmpty(groups = ValidationGroups.Create.class)
    @Size(min = 1, max = 3, groups = ValidationGroups.Create.class)
    Set<String> talents,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Double poids,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Double taille,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer pv,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer attaque,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer defense,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer attaqueSpeciale,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer defenseSpeciale,

    @NotNull(groups = ValidationGroups.Create.class)
    @Positive(groups = ValidationGroups.Create.class)
    Integer vitesse
) {}
