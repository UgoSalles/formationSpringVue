package com.example.test.pokemon;

import com.example.test.region.RegionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import platform.common.entity.BaseEntity;

import java.util.Set;

@Entity
@Table(name = "pokemon")
@Getter
@Setter
public class PokemonEntity extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100, unique = true)
    private String nom;

    @NotEmpty
    @Size(min = 1, max = 2)
    @ElementCollection
    @CollectionTable(name = "pokemon_type", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Set<PokemonType> types;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = true)
    private String image;

    @Column(nullable = true)
    private String imageChromatique;

    @NotNull
    @ManyToOne(optional = false)
    private RegionEntity region;

    @ManyToOne(optional = true)
    private PokemonEntity preEvolution;

    @ManyToOne(optional = true)
    private PokemonEntity evolution;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nature nature;

    @NotEmpty
    @Size(min = 1, max = 3)
    @ElementCollection
    @CollectionTable(name = "pokemon_talent", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "talent", nullable = false, length = 100)
    private Set<String> talents;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double poids;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double taille;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer pv;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer attaque;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer defense;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer attaqueSpeciale;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer defenseSpeciale;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer vitesse;
}
