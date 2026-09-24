package platform.seed;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Auto-configuration du seeder ({@link PlatformSeeder}).
 *
 * <p><strong>Inactive par défaut</strong> : le bean n'est créé que si {@code platform.seed.run} est
 * renseigné (pas de {@code matchIfMissing}). Le seeding ne tourne donc jamais en run normal ni en prod —
 * uniquement quand on lance explicitement une commande {@code seed:*}.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.seed", name = "run")
@EnableConfigurationProperties(PlatformSeedProperties.class)
public class SeedAutoConfiguration {

    /**
     * Exécuteur du seeding au démarrage.
     *
     * @param seeds              tous les beans {@link Seed} déclarés par le projet (éventuellement vide)
     * @param entityManager      contexte de persistance (insertion en cascade)
     * @param transactionManager gestionnaire de transactions
     * @param props              configuration {@code platform.seed}
     * @return l'exécuteur de seeding
     */
    @Bean
    public PlatformSeeder platformSeeder(
            List<Seed<?>> seeds,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            PlatformSeedProperties props) {
        return new PlatformSeeder(seeds, entityManager, transactionManager, props);
    }
}
