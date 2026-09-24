package com.example.test.pokemon;

import com.example.test.region.RegionEntity;
import com.example.test.region.RegionRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import platform.seed.Seed;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// Seed des 3 starters de chaque région principale (27 au total, catalogue fixe).
@Component
public class PokemonSeed implements Seed<PokemonEntity> {

    // Un starter du catalogue ; `region` = nom (résolu en entité plus tard).
    private record Data(
            String region, String nom, Set<PokemonType> types, String talent, Nature nature,
            String description, double poids, double taille,
            int pv, int attaque, int defense, int attaqueSpeciale, int defenseSpeciale, int vitesse) {
    }

    // 3 starters (Plante/Feu/Eau) par région, dans l'ordre des jeux.
    private static final List<Data> STARTERS = List.of(
            // Kanto
            new Data("Kanto", "Bulbizarre", Set.of(PokemonType.PLANTE), "Engrais", Nature.DOCILE,
                    "Une graine étrange fut plantée sur son dos à sa naissance ; elle grandit avec lui.",
                    6.9, 0.7, 45, 49, 49, 65, 65, 45),
            new Data("Kanto", "Salamèche", Set.of(PokemonType.FEU), "Brasier", Nature.BRAVE,
                    "Il agite la flamme au bout de sa queue pour exprimer sa vitalité ; elle vacille quand il faiblit.",
                    8.5, 0.6, 39, 52, 43, 60, 50, 65),
            new Data("Kanto", "Carapuce", Set.of(PokemonType.EAU), "Torrent", Nature.CALME,
                    "Peu après sa naissance, son dos durcit pour former une carapace qui le protège des chocs.",
                    9.0, 0.5, 44, 48, 65, 50, 64, 43),

            // Johto
            new Data("Johto", "Germignon", Set.of(PokemonType.PLANTE), "Engrais", Nature.DOUX,
                    "La feuille sur sa tête indique son état de santé : plus elle est fraîche, mieux il se porte.",
                    6.4, 0.9, 45, 49, 65, 49, 65, 45),
            new Data("Johto", "Héricendre", Set.of(PokemonType.FEU), "Brasier", Nature.JOVIAL,
                    "Il enflamme le gaz produit par son estomac pour projeter des flammes le long de son dos.",
                    7.9, 0.5, 39, 52, 43, 60, 50, 65),
            new Data("Johto", "Kaiminus", Set.of(PokemonType.EAU), "Torrent", Nature.MALIN,
                    "Ses mâchoires puissantes ne relâchent jamais leur prise une fois solidement refermées.",
                    9.5, 0.6, 50, 65, 64, 44, 48, 43),

            // Hoenn
            new Data("Hoenn", "Arcko", Set.of(PokemonType.PLANTE), "Engrais", Nature.PRESSE,
                    "Les coussinets de ses pieds lui permettent de grimper sur les surfaces les plus lisses.",
                    5.0, 0.5, 40, 45, 35, 65, 55, 70),
            new Data("Hoenn", "Poussifeu", Set.of(PokemonType.FEU), "Brasier", Nature.FOUFOU,
                    "Une poche de feu dans son ventre produit la chaleur qui s'échappe en permanence de son bec.",
                    2.5, 0.4, 45, 60, 40, 70, 50, 45),
            new Data("Hoenn", "Gobou", Set.of(PokemonType.EAU), "Torrent", Nature.RELAX,
                    "La nageoire posée sur sa tête agit comme un radar détectant les mouvements dans l'eau.",
                    7.6, 0.4, 50, 70, 50, 50, 50, 40),

            // Sinnoh
            new Data("Sinnoh", "Tortipouss", Set.of(PokemonType.PLANTE), "Engrais", Nature.SERIEUX,
                    "Un jeune arbre pousse sur son dos ; ses racines puisent leur nourriture dans son propre corps.",
                    10.2, 0.4, 55, 68, 64, 45, 55, 31),
            new Data("Sinnoh", "Ouisticram", Set.of(PokemonType.FEU), "Brasier", Nature.MAUVAIS,
                    "La flamme au bout de sa queue reflète fidèlement son humeur et son niveau d'énergie.",
                    6.2, 0.5, 44, 58, 44, 58, 44, 61),
            new Data("Sinnoh", "Tiplouf", Set.of(PokemonType.EAU), "Torrent", Nature.HARDI,
                    "Trop fier pour l'admettre, il peine encore à nager correctement malgré son assurance.",
                    5.2, 0.4, 53, 51, 53, 61, 56, 40),

            // Unys
            new Data("Unys", "Vipélierre", Set.of(PokemonType.PLANTE), "Engrais", Nature.DISCRET,
                    "Il se nourrit par la queue en photosynthèse, absorbant la lumière du soleil sans effort.",
                    8.1, 0.6, 45, 45, 55, 45, 55, 63),
            new Data("Unys", "Gruikui", Set.of(PokemonType.FEU), "Brasier", Nature.NAIF,
                    "Le feu qui jaillit de son groin s'intensifie soudainement quand il s'excite ou s'énerve.",
                    9.9, 0.5, 65, 63, 45, 45, 45, 45),
            new Data("Unys", "Moustillon", Set.of(PokemonType.EAU), "Torrent", Nature.PRUDENT,
                    "Il combat en dégainant la coquillette fixée à son ventre comme une lame improvisée.",
                    5.9, 0.5, 55, 55, 45, 63, 45, 45),

            // Kalos
            new Data("Kalos", "Marisson", Set.of(PokemonType.PLANTE), "Engrais", Nature.RIGIDE,
                    "Sa carapace de piquants, dure comme du bois, amortit les chocs lorsqu'il charge tête baissée.",
                    9.0, 0.4, 56, 61, 65, 48, 45, 38),
            new Data("Kalos", "Feunnec", Set.of(PokemonType.FEU), "Brasier", Nature.TIMIDE,
                    "L'air chaud qu'il expire par les oreilles peut atteindre plusieurs centaines de degrés.",
                    3.9, 0.4, 40, 45, 40, 62, 60, 60),
            new Data("Kalos", "Grenousse", Set.of(PokemonType.EAU), "Torrent", Nature.MALPOLI,
                    "Les bulles qui recouvrent son dos amortissent les chocs et étouffent le bruit de ses sauts.",
                    7.0, 0.3, 41, 56, 40, 62, 44, 71),

            // Alola
            new Data("Alola", "Brindibou", Set.of(PokemonType.PLANTE, PokemonType.VOL), "Engrais", Nature.SOLO,
                    "Discret et silencieux, il observe ses adversaires avant de frapper avec des plumes tranchantes.",
                    1.5, 0.3, 68, 55, 55, 50, 50, 42),
            new Data("Alola", "Flamiaou", Set.of(PokemonType.FEU), "Brasier", Nature.ASSURE,
                    "Capricieux et sûr de lui, il n'obéit qu'aux dresseurs qu'il juge dignes de son respect.",
                    4.3, 0.4, 45, 65, 40, 60, 40, 70),
            new Data("Alola", "Otaquin", Set.of(PokemonType.EAU), "Torrent", Nature.GENTIL,
                    "Il applaudit avec ses nageoires pour projeter des ballons d'eau capables de percer l'acier.",
                    7.5, 0.4, 50, 54, 54, 66, 56, 40),

            // Galar
            new Data("Galar", "Ouistempo", Set.of(PokemonType.PLANTE), "Engrais", Nature.BIZARRE,
                    "Il frappe une brindille contre son corps comme un métronome pour garder le rythme au combat.",
                    5.0, 0.3, 50, 65, 50, 40, 40, 65),
            new Data("Galar", "Flambino", Set.of(PokemonType.FEU), "Brasier", Nature.PUDIQUE,
                    "Ses jambes musclées lui permettent des accélérations fulgurantes sur de courtes distances.",
                    4.5, 0.3, 50, 71, 40, 40, 40, 69),
            new Data("Galar", "Larméléon", Set.of(PokemonType.EAU), "Torrent", Nature.MODESTE,
                    "Timide à l'excès, il libère des larmes acides lorsqu'il est surpris ou effrayé.",
                    4.0, 0.3, 50, 40, 40, 70, 40, 70),

            // Paldea
            new Data("Paldea", "Poussacha", Set.of(PokemonType.PLANTE), "Engrais", Nature.DOCILE,
                    "L'odeur sucrée qui émane de son corps masque des griffes acérées prêtes à jaillir.",
                    4.1, 0.4, 40, 61, 54, 45, 45, 65),
            new Data("Paldea", "Chochodile", Set.of(PokemonType.FEU), "Brasier", Nature.BRAVE,
                    "La poche sur son ventre atteint 100°C ; il y cuit volontiers les baies avant de les manger.",
                    9.8, 0.4, 67, 45, 59, 63, 40, 36),
            new Data("Paldea", "Coiffeton", Set.of(PokemonType.EAU), "Torrent", Nature.CALME,
                    "Très soigneux de son plumage, il lisse ses plumes de tête avec de l'huile avant chaque combat.",
                    6.1, 0.5, 55, 65, 45, 50, 45, 50));

    private final RegionRepository regionRepository;
    // Entités déjà persistées, réutilisées par one() une fois le catalogue épuisé.
    private final List<PokemonEntity> created = new CopyOnWriteArrayList<>();
    // Index courant dans STARTERS.
    private final AtomicInteger index = new AtomicInteger(0);

    public PokemonSeed(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public Class<PokemonEntity> type() {
        return PokemonEntity.class;
    }

    // Après RegionSeed (order 0) : un starter a besoin de sa région déjà en base.
    @Override
    public int order() {
        return 1;
    }

    @Override
    public PokemonEntity one(Faker faker) {
        int i = index.getAndIncrement();
        if (i < STARTERS.size()) {
            Data data = STARTERS.get(i);
            RegionEntity region = regionRepository.findByNom(data.region())
                    .orElseThrow(() -> new IllegalStateException(
                            "Région introuvable pour le seed pokemon : " + data.region()));

            PokemonEntity pokemon = new PokemonEntity();
            pokemon.setNom(data.nom());
            pokemon.setTypes(data.types());
            pokemon.setDescription(data.description());
            pokemon.setRegion(region);
            pokemon.setNature(data.nature());
            pokemon.setTalents(Set.of(data.talent()));
            pokemon.setPoids(data.poids());
            pokemon.setTaille(data.taille());
            pokemon.setPv(data.pv());
            pokemon.setAttaque(data.attaque());
            pokemon.setDefense(data.defense());
            pokemon.setAttaqueSpeciale(data.attaqueSpeciale());
            pokemon.setDefenseSpeciale(data.defenseSpeciale());
            pokemon.setVitesse(data.vitesse());
            created.add(pokemon);
            return pokemon;
        }
        // Catalogue épuisé : ré-émet une entité déjà persistée (no-op JPA, pas de doublon).
        return created.get(i % created.size());
    }
}
