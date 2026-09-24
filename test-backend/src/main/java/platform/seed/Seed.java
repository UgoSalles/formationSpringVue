package platform.seed;

import net.datafaker.Faker;

/**
 * Définit comment peupler une entité avec des données réalistes. Un <strong>bean Spring</strong> par
 * entité à semer (implémentation côté projet).
 *
 * <p>{@link #one(Faker)} construit <strong>un agrégat réaliste</strong> — l'entité et, le cas échéant,
 * ses sous-ressources (créées en cascade JPA). Le {@link PlatformSeeder} l'appelle autant de fois que le
 * mode l'exige (cf. {@code platform.seed.run} / {@code platform.seed.count}). Tout est du Java pur : pour
 * un champ « métier » à valeurs imposées, choisis dans une liste ({@code faker.options().option(...)}) ;
 * pour une valeur forcée, écris-la en dur ; pour une cardinalité de sous-ressource, boucle sur un tirage
 * (éventuellement pondéré).
 *
 * <p>La même méthode sert de <strong>factory en test d'intégration</strong> :
 * {@code persist(new MySeed().one(new Faker()))}.
 *
 * @param <T> type de l'entité semée
 */
public interface Seed<T> {

    /**
     * Type de l'entité semée (sert au log et au regroupement).
     *
     * @return la classe de l'entité
     */
    Class<T> type();

    /**
     * Construit un agrégat réaliste (entité + sous-ressources en cascade).
     *
     * @param faker générateur de données aléatoires réalistes
     * @return une nouvelle instance non persistée
     */
    T one(Faker faker);

    /**
     * Ordre d'insertion entre seeds (croissant). À surcharger quand un seed dépend d'un autre
     * (clé étrangère vers un autre agrégat racine déjà semé). Défaut : 0.
     *
     * @return la priorité d'insertion (plus petit = inséré en premier)
     */
    default int order() {
        return 0;
    }
}
