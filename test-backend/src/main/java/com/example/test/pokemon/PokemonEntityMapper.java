package com.example.test.pokemon;

import com.example.test.pokemon.dto.PokemonEntityCreateDto;
import com.example.test.pokemon.dto.PokemonEntityDetailDto;
import com.example.test.pokemon.dto.PokemonEntityUpdateDto;
import com.example.test.region.RegionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper MapStruct pour l'entité {@link PokemonEntity}.
 *
 * <p>Relations aplaties en UUID (pas d'imbrication, REST). Le service charge les entités liées
 * par UUID et les passe déjà résolues à {@code toEntity}/{@code merge} ; le mapper reste pur
 * (aucun accès base). Le détail des relations se récupère via des endpoints dédiés (sous-ressources).
 */
@Mapper(componentModel = "spring")
public interface PokemonEntityMapper {

    @Mapping(target = "nom", source = "dto.nom")
    @Mapping(target = "types", source = "dto.types")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "image", source = "dto.image")
    @Mapping(target = "imageChromatique", source = "dto.imageChromatique")
    @Mapping(target = "region", source = "region")
    @Mapping(target = "preEvolution", source = "preEvolution")
    @Mapping(target = "evolution", source = "evolution")
    @Mapping(target = "nature", source = "dto.nature")
    @Mapping(target = "talents", source = "dto.talents")
    @Mapping(target = "poids", source = "dto.poids")
    @Mapping(target = "taille", source = "dto.taille")
    @Mapping(target = "pv", source = "dto.pv")
    @Mapping(target = "attaque", source = "dto.attaque")
    @Mapping(target = "defense", source = "dto.defense")
    @Mapping(target = "attaqueSpeciale", source = "dto.attaqueSpeciale")
    @Mapping(target = "defenseSpeciale", source = "dto.defenseSpeciale")
    @Mapping(target = "vitesse", source = "dto.vitesse")
    PokemonEntity toEntity(PokemonEntityCreateDto dto, RegionEntity region, PokemonEntity preEvolution, PokemonEntity evolution);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "nom", source = "dto.nom")
    @Mapping(target = "types", source = "dto.types")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "image", source = "dto.image")
    @Mapping(target = "imageChromatique", source = "dto.imageChromatique")
    @Mapping(target = "region", source = "region")
    @Mapping(target = "preEvolution", source = "preEvolution")
    @Mapping(target = "evolution", source = "evolution")
    @Mapping(target = "nature", source = "dto.nature")
    @Mapping(target = "talents", source = "dto.talents")
    @Mapping(target = "poids", source = "dto.poids")
    @Mapping(target = "taille", source = "dto.taille")
    @Mapping(target = "pv", source = "dto.pv")
    @Mapping(target = "attaque", source = "dto.attaque")
    @Mapping(target = "defense", source = "dto.defense")
    @Mapping(target = "attaqueSpeciale", source = "dto.attaqueSpeciale")
    @Mapping(target = "defenseSpeciale", source = "dto.defenseSpeciale")
    @Mapping(target = "vitesse", source = "dto.vitesse")
    void merge(PokemonEntityUpdateDto dto, RegionEntity region, PokemonEntity preEvolution, PokemonEntity evolution,
            @MappingTarget PokemonEntity entity);

    @Mapping(target = "regionId", source = "region.id")
    @Mapping(target = "preEvolutionId", source = "preEvolution.id")
    @Mapping(target = "evolutionId", source = "evolution.id")
    @Mapping(target = "nature", source = "nature")
    @Mapping(target = "talents", source = "talents")
    @Mapping(target = "poids", source = "poids")
    @Mapping(target = "taille", source = "taille")
    @Mapping(target = "pv", source = "pv")
    @Mapping(target = "attaque", source = "attaque")
    @Mapping(target = "defense", source = "defense")
    @Mapping(target = "attaqueSpeciale", source = "attaqueSpeciale")
    @Mapping(target = "defenseSpeciale", source = "defenseSpeciale")
    @Mapping(target = "vitesse", source = "vitesse")
    PokemonEntityDetailDto toDto(PokemonEntity entity);
}
