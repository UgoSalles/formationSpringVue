package com.example.test.region;

import com.example.test.region.dto.RegionEntityCreateDto;
import com.example.test.region.dto.RegionEntityDetailDto;
import com.example.test.region.dto.RegionEntityUpdateDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper MapStruct pour l'entité {@link RegionEntity}.
 */
@Mapper(componentModel = "spring")
public interface RegionEntityMapper {

    RegionEntity toEntity(RegionEntityCreateDto dto);

    /** Update partiel (PATCH) : un champ null du DTO n'écrase pas la valeur existante. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(RegionEntityUpdateDto dto, @MappingTarget RegionEntity entity);

    RegionEntityDetailDto toDto(RegionEntity entity);
}
