package platform.common.validation;

/** Groupes de validation Bean Validation pour les DTOs platform. */
public final class ValidationGroups {

    private ValidationGroups() {}

    /** Groupe appliqué lors de la création (POST). */
    public interface Create {}

    /** Groupe appliqué lors de la mise à jour (PATCH). */
    public interface Update {}
}
