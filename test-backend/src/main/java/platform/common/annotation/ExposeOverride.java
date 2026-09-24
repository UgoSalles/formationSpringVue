package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Marque une méthode de contrôleur qui surcharge une opération CRUD standard.
 * La méthode hérite du mapping Spring MVC du parent ; appeler {@code super.create(dto)}
 * pour déléguer avant/après la logique custom.
 *
 * <p>Exemple :
 * <pre>{@code
 * @ExposeOverride(CREATE)
 * public ResponseEntity<UserDetailDto> create(@RequestBody UserCreateDto dto) {
 *     // logique custom avant
 *     return super.create(dto);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeOverride {
    ExposeOperation value();
}
