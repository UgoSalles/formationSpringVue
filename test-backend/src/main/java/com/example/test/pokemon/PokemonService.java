package com.example.test.pokemon;

import com.example.test.pokemon.dto.PokemonEntityCreateDto;
import com.example.test.pokemon.dto.PokemonEntityDetailDto;
import com.example.test.pokemon.dto.PokemonEntityUpdateDto;
import com.example.test.region.RegionEntity;
import com.example.test.region.RegionRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import platform.common.exception.ResourceNotFoundException;
import platform.common.service.BaseCrudService;

import java.util.UUID;

@Service
public class PokemonService
        extends BaseCrudService<PokemonEntity, PokemonEntityCreateDto, PokemonEntityUpdateDto, PokemonEntityDetailDto> {

    private final PokemonRepository repository;
    private final RegionRepository regionRepository;
    private final JPAQueryFactory queryFactory;
    private final PokemonEntityMapper mapper;

    public PokemonService(PokemonRepository repository, RegionRepository regionRepository,
            JPAQueryFactory queryFactory, PokemonEntityMapper mapper) {
        this.repository = repository;
        this.regionRepository = regionRepository;
        this.queryFactory = queryFactory;
        this.mapper = mapper;
    }

    @Override
    protected JpaRepository<PokemonEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    @Override
    protected EntityPath<PokemonEntity> getEntityPath() {
        return QPokemonEntity.pokemonEntity;
    }

    @Override
    protected Class<PokemonEntity> getEntityClass() {
        return PokemonEntity.class;
    }

    @Override
    public String getCacheName() {
        return "pokemons";
    }

    @Override
    protected PokemonEntity toEntity(PokemonEntityCreateDto dto) {
        RegionEntity region = findRegion(dto.regionId());
        PokemonEntity preEvolution = findPokemon(dto.preEvolutionId());
        PokemonEntity evolution = findPokemon(dto.evolutionId());
        return mapper.toEntity(dto, region, preEvolution, evolution);
    }

    @Override
    protected void merge(PokemonEntity entity, PokemonEntityUpdateDto dto) {
        RegionEntity region = dto.regionId() != null ? findRegion(dto.regionId()) : null;
        PokemonEntity preEvolution = findPokemon(dto.preEvolutionId());
        PokemonEntity evolution = findPokemon(dto.evolutionId());
        mapper.merge(dto, region, preEvolution, evolution, entity);
    }

    @Override
    protected PokemonEntityDetailDto toDto(PokemonEntity entity) {
        return mapper.toDto(entity);
    }

    private RegionEntity findRegion(UUID regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException(regionId.toString()));
    }

    private PokemonEntity findPokemon(UUID id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id.toString()));
    }
}
