package com.example.test.region;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import platform.seed.Seed;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Seed des 9 régions principales de la saga Pokémon (catalogue fixe, pas de données aléatoires).
 *
 * <p>Le {@code PlatformSeeder} appelle {@link #one(Faker)} {@code count} fois. Les 9 premiers appels
 * renvoient chacun une région distincte du catalogue ; au-delà, on renvoie une entité déjà persistée
 * dans cette même transaction (JPA ignore un {@code persist} sur une entité déjà managée) plutôt que de
 * dupliquer un {@code nom} (colonne {@code unique = true}).
 */
@Component
public class RegionSeed implements Seed<RegionEntity> {

    private record Data(String nom, int generation, String description) {
    }

    private static final List<Data> REGIONS = List.of(
            new Data("Kanto", 1,
                    "Région d'origine de la toute première aventure Pokémon, où le professeur Chen confie "
                            + "leur partenaire aux jeunes dresseurs."),
            new Data("Johto", 2,
                    "Région voisine de Kanto, marquée par ses temples anciens et les légendes autour des "
                            + "Pokémon de la tour de Verchamps."),
            new Data("Hoenn", 3,
                    "Région largement entourée d'eau, partagée entre volcans et récifs, théâtre de "
                            + "l'affrontement entre Groudon et Kyogre."),
            new Data("Sinnoh", 4,
                    "Région montagneuse dominée par le mont Couronne, berceau du mythe de la création "
                            + "porté par Arceus et l'Arbre-Monde."),
            new Data("Unys", 5,
                    "Région portuaire inspirée de New York, marquée par l'opposition entre tradition et "
                            + "modernité qu'incarne la Team Plasma."),
            new Data("Kalos", 6,
                    "Région en forme d'étoile inspirée de la France, où la Méga-Évolution a été révélée "
                            + "au monde pour la première fois."),
            new Data("Alola", 7,
                    "Archipel tropical organisé en épreuves insulaires plutôt qu'en Arènes, patrie des "
                            + "formes régionales dites d'Alola."),
            new Data("Galar", 8,
                    "Région industrielle inspirée du Royaume-Uni, connue pour le phénomène Dynamax et ses "
                            + "stades de combat bondés."),
            new Data("Paldea", 9,
                    "Vaste région à monde ouvert inspirée de la péninsule ibérique, premier cadre en "
                            + "monde ouvert de la saga."));

    private final List<RegionEntity> created = new CopyOnWriteArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Class<RegionEntity> type() {
        return RegionEntity.class;
    }

    @Override
    public RegionEntity one(Faker faker) {
        int i = index.getAndIncrement();
        if (i < REGIONS.size()) {
            Data data = REGIONS.get(i);
            RegionEntity region = new RegionEntity();
            region.setNom(data.nom());
            region.setGeneration(data.generation());
            region.setDescription(data.description());
            created.add(region);
            return region;
        }
        return created.get(i % created.size());
    }
}
