package com.example.test.pokemon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PokemonRepository extends JpaRepository<PokemonEntity, UUID> {
}
