package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Déclare les query params autorisés sur un endpoint GET.
 * Tout paramètre non listé entraîne un 400 Bad Request.
 *
 * <p>Exemple : {@code @AllowedQueryParams({"format", "locale"})}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedQueryParams {
    String[] value();
}
