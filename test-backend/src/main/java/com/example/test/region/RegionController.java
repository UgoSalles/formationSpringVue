package com.example.test.region;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.region.dto.RegionEntityCreateDto;
import com.example.test.region.dto.RegionEntityDetailDto;
import com.example.test.region.dto.RegionEntityUpdateDto;
import static platform.common.annotation.ExposeOperation.*;
import static platform.common.annotation.FilterOperator.*;
import platform.common.annotation.Expose;
import platform.common.annotation.Filter;
import platform.common.controller.AbstractCrudController;
import platform.common.service.BaseCrudService;

@RestController
@RequestMapping("/regions")
@Expose(CREATE) 
@Expose(FIND_ONE) 
@Expose(UPDATE) 
@Expose(REMOVE)
@Expose(value = SEARCH, allowedFilters = {          // filtres autorisés portés par le SEARCH lui-même
    @Filter(field = "nom", operators = {LIKE, EQ}),
    @Filter(field = "generation", operators = {EQ, GT, LT}),
})
public class RegionController
    extends AbstractCrudController<RegionEntity, RegionEntityCreateDto, RegionEntityUpdateDto, RegionEntityDetailDto> {

    private final RegionService service;
    public RegionController(RegionService service) { this.service = service; }

    @Override
    protected BaseCrudService<RegionEntity, RegionEntityCreateDto, RegionEntityUpdateDto, RegionEntityDetailDto> getService() {
        return service;
    }
}