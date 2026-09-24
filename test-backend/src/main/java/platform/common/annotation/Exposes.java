package platform.common.annotation;

import java.lang.annotation.*;

/** Conteneur pour {@link Expose} répétable. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Exposes {
    Expose[] value();
}
