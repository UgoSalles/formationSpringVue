package platform.common.businessrule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import platform.common.annotation.BusinessRule;
import platform.common.controller.AbstractCrudController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Génère, au démarrage, le registre {@code business-rules.json} des règles métier déclarées via
 * {@link BusinessRule} sur les endpoints custom. Les règles sont <strong>groupées par entité</strong>
 * (l'entité du {@code AbstractCrudController}, sinon le nom du contrôleur sans le suffixe {@code Controller}),
 * triées par code à l'intérieur de chaque entité.
 *
 * <p>Remplace l'ancienne promesse « génération par APT » : le platform fonctionne par réflexion au
 * runtime, ce scan reste cohérent avec ce choix (pas de processeur d'annotations compile-time).
 *
 * <p>Débrayable via {@code platform.business-rules.enabled=false} ; chemin de sortie configurable via
 * {@code platform.business-rules.output} (défaut {@code business-rules.json}). Écriture best-effort :
 * un échec d'écriture (ex. conteneur en lecture seule) journalise un avertissement sans bloquer le boot.
 */
public class BusinessRulesExporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BusinessRulesExporter.class);

    private final ApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String outputPath;

    public BusinessRulesExporter(ApplicationContext context, String outputPath) {
        this.context = context;
        this.outputPath = outputPath;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, List<Rule>> byEntity = collect();
        if (byEntity.isEmpty()) {
            return;
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("generatedAt", Instant.now().toString());
        document.put("rules", byEntity);
        try {
            Path out = Path.of(outputPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), document);
            log.info("business-rules.json généré ({} entité(s)) → {}", byEntity.size(), out.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Écriture de {} impossible : {}", outputPath, e.getMessage());
        }
    }

    /** Scanne les {@code @RestController} et collecte les {@link BusinessRule}, groupés/triés. */
    private Map<String, List<Rule>> collect() {
        Map<String, List<Rule>> byEntity = new TreeMap<>();
        for (Object bean : context.getBeansWithAnnotation(RestController.class).values()) {
            Class<?> clazz = ClassUtils.getUserClass(bean);
            String entity = resolveEntity(clazz);
            String basePath = basePath(clazz);
            for (Method method : clazz.getDeclaredMethods()) {
                BusinessRule rule = AnnotatedElementUtils.findMergedAnnotation(method, BusinessRule.class);
                if (rule == null) {
                    continue;
                }
                byEntity.computeIfAbsent(entity, k -> new ArrayList<>())
                        .add(new Rule(rule.code(), rule.description(), endpoint(basePath, method)));
            }
        }
        byEntity.values().forEach(rules -> rules.sort(Comparator.comparing(Rule::code)));
        return byEntity;
    }

    /** Entité associée : type générique du {@code AbstractCrudController}, sinon nom du contrôleur. */
    private static String resolveEntity(Class<?> clazz) {
        if (AbstractCrudController.class.isAssignableFrom(clazz)) {
            Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(clazz, AbstractCrudController.class);
            if (generics != null && generics.length > 0 && generics[0] != null) {
                return generics[0].getSimpleName();
            }
        }
        return clazz.getSimpleName().replaceAll("Controller$", "");
    }

    private static String basePath(Class<?> clazz) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
        return (mapping != null && mapping.value().length > 0) ? mapping.value()[0] : "";
    }

    /** Reconstitue « VERBE chemin » depuis l'annotation de mapping de la méthode (best-effort). */
    private static String endpoint(String basePath, Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST " + basePath + first(method.getAnnotation(PostMapping.class).value());
        }
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET " + basePath + first(method.getAnnotation(GetMapping.class).value());
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return "PUT " + basePath + first(method.getAnnotation(PutMapping.class).value());
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return "PATCH " + basePath + first(method.getAnnotation(PatchMapping.class).value());
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE " + basePath + first(method.getAnnotation(DeleteMapping.class).value());
        }
        return "";
    }

    private static String first(String[] paths) {
        return paths.length > 0 ? paths[0] : "";
    }

    /** Une règle métier exportée. */
    public record Rule(String code, String description, String endpoint) {}
}
