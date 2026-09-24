package platform.seed;

import jakarta.persistence.EntityManager;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Exécute le seeding au démarrage, <strong>uniquement</strong> quand {@code platform.seed.run} est
 * renseigné (cf. {@link SeedAutoConfiguration}) — donc jamais en run normal ni en prod.
 *
 * <p>Pour chaque {@link Seed} déclaré (trié par {@link Seed#order()}), persiste {@code n} agrégats :
 * {@code n = 1} en mode {@code basic}, {@code platform.seed.count} en {@code dev}/{@code full}. Le
 * « drop » se fait en amont via le schéma recréé à neuf (commande lancée avec
 * {@code spring.jpa.hibernate.ddl-auto=create}) : le seeder se contente d'insérer.
 */
public class PlatformSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformSeeder.class);

    private final List<Seed<?>> seeds;
    private final EntityManager entityManager;
    private final TransactionTemplate tx;
    private final PlatformSeedProperties props;
    private final Faker faker = new Faker(Locale.FRENCH);

    public PlatformSeeder(
            List<Seed<?>> seeds,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            PlatformSeedProperties props) {
        this.seeds = seeds;
        this.entityManager = entityManager;
        this.tx = new TransactionTemplate(transactionManager);
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String mode = props.getRun();
        if (mode == null || mode.isBlank()) {
            return;
        }
        int count = "basic".equalsIgnoreCase(mode) ? 1 : Math.max(1, props.getCount());
        if (seeds.isEmpty()) {
            log.warn("Seeding demandé (mode={}) mais aucun bean Seed<?> déclaré.", mode);
            return;
        }
        List<Seed<?>> ordered = seeds.stream()
                .sorted(Comparator.comparingInt(Seed::order))
                .toList();
        log.info("Seeding (mode={}) : {} entité(s), {} instance(s)/entité…", mode, ordered.size(), count);
        for (Seed<?> seed : ordered) {
            persistSeed(seed, count);
        }
        log.info("Seeding terminé.");
    }

    private <T> void persistSeed(Seed<T> seed, int count) {
        tx.executeWithoutResult(status -> {
            for (int i = 0; i < count; i++) {
                entityManager.persist(seed.one(faker));
            }
        });
        log.info("  + {} × {}", count, seed.type().getSimpleName());
    }
}
