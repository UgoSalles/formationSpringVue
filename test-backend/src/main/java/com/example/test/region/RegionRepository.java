package com.example.test.region;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<RegionEntity, UUID> {

    Optional<RegionEntity> findByNom(String nom);
}
