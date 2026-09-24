package platform.common.annotation;

import platform.common.dto.SortOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Définit le tri par défaut appliqué par {@code BaseCrudService} quand {@code SearchRequest.sort} est vide.
 *
 * <p>{@code @Inherited} : {@code BaseEntity} en porte un par défaut ({@code id ASC} — uuid_v7, donc
 * chronologique et stable). Une entité peut le <strong>surcharger</strong> ; sa propre annotation
 * <strong>prime</strong> alors sur celle héritée de {@code BaseEntity}. Le départage final par {@code id}
 * est de toute façon ajouté par {@code BaseCrudService}.
 *
 * <pre>{@code
 * @Entity
 * @DefaultSort(field = "createdAt", order = SortOrder.DESC) // prime sur le `id ASC` de BaseEntity
 * public class User extends BaseEntity { ... }
 * }</pre>
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultSort {
    String field();
    SortOrder order() default SortOrder.ASC;
}
