package com.example.test.pokemon;

import com.example.test.pokemon.dto.PokemonEntityCreateDto;
import com.example.test.pokemon.dto.PokemonEntityDetailDto;
import com.example.test.pokemon.dto.PokemonEntityUpdateDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import platform.common.annotation.Expose;
import platform.common.annotation.Filter;
import platform.common.controller.AbstractCrudController;
import platform.common.service.BaseCrudService;

import static platform.common.annotation.ExposeOperation.CREATE;
import static platform.common.annotation.ExposeOperation.FIND_ONE;
import static platform.common.annotation.ExposeOperation.REMOVE;
import static platform.common.annotation.ExposeOperation.SEARCH;
import static platform.common.annotation.ExposeOperation.UPDATE;        
import static platform.common.annotation.FilterOperator.EQ;
import static platform.common.annotation.FilterOperator.LIKE;

@RestController 
@RequestMapping("/pokemons")
@Expose(CREATE) @Expose(FIND_ONE) @Expose(UPDATE) @Expose(REMOVE)
@Expose(value = SEARCH, allowedFilters = {          // "types" exclu : collection non supportée par FilterPredicateBuilder
    @Filter(field = "nom", operators = {LIKE, EQ}),
    @Filter(field = "region.generation", operators = {EQ}),
    @Filter(field = "region.nom", operators = {EQ, LIKE}),
})
public class PokemonController
        extends AbstractCrudController<PokemonEntity, PokemonEntityCreateDto, PokemonEntityUpdateDto, PokemonEntityDetailDto> {

    private final PokemonService service;

    public PokemonController(PokemonService service) {
        this.service = service;
    }

    @Override
    protected BaseCrudService<PokemonEntity, PokemonEntityCreateDto, PokemonEntityUpdateDto, PokemonEntityDetailDto> getService() {
        return service;
    }
}
