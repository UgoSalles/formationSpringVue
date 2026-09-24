package com.example.test.region;

import com.example.test.region.dto.RegionEntityCreateDto;
import com.example.test.region.dto.RegionEntityDetailDto;
import com.example.test.region.dto.RegionEntityUpdateDto;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import platform.common.service.BaseCrudService;

import java.util.UUID;

@Service
public class RegionService
        extends BaseCrudService<RegionEntity, RegionEntityCreateDto, RegionEntityUpdateDto, RegionEntityDetailDto> {

    private final RegionRepository repository;
    private final JPAQueryFactory queryFactory;
    private final RegionEntityMapper mapper;

    public RegionService(RegionRepository repository, JPAQueryFactory queryFactory, RegionEntityMapper mapper) {
        this.repository = repository;
        this.queryFactory = queryFactory;
        this.mapper = mapper;
    }

    @Override
    protected JpaRepository<RegionEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    @Override
    protected EntityPath<RegionEntity> getEntityPath() {
        return QRegionEntity.regionEntity;
    }

    @Override
    protected Class<RegionEntity> getEntityClass() {
        return RegionEntity.class;
    }

    @Override
    public String getCacheName() {
        return "regions";
    }

    @Override
    protected RegionEntity toEntity(RegionEntityCreateDto dto) {
        return mapper.toEntity(dto);
    }

    @Override
    protected void merge(RegionEntity entity, RegionEntityUpdateDto dto) {
        mapper.merge(dto, entity);
    }

    @Override
    protected RegionEntityDetailDto toDto(RegionEntity entity) {
        return mapper.toDto(entity);
    }
}
