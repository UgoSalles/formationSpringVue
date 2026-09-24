package platform.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du seeder ({@code platform.seed}).
 *
 * <p>Le seeder ne s'exécute <strong>que</strong> si {@code platform.seed.run} est renseigné — jamais en
 * run normal ni en prod. Les modes :
 * <ul>
 *   <li>{@code basic} : <strong>1</strong> instance par entité (structure minimale réaliste) ;</li>
 *   <li>{@code dev} / {@code full} : {@code count} instances par entité (volume passé en commande).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "platform.seed")
public class PlatformSeedProperties {

    /** Mode de seed : {@code basic} | {@code dev} | {@code full}. Absent/vide = pas de seed. */
    private String run;

    /** Nombre d'instances par entité pour {@code dev}/{@code full} ({@code basic} force 1). */
    private int count = 20;

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
