import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Génère les DTOs (Create / Update / Detail) et le Mapper MapStruct d'une entité JPA.
 *
 * <p>Équivalent projet de l'ancienne commande {@code platform dto} — autonome (JDK seul), exécutable
 * sans Maven ni node via le mode source-file de Java :
 *
 * <pre>
 *   java tools/DtoGenerator.java &lt;Entité&gt;            (depuis backend/)
 *   java backend/tools/DtoGenerator.java &lt;Entité&gt;    (depuis la racine du projet)
 * </pre>
 *
 * <p>Lit {@code src/main/java/**}/&lt;Entité&gt;.java, en extrait les champs et leurs contraintes Bean
 * Validation (les annotations doivent être <strong>sans</strong> {@code groups} sur l'entité : le
 * générateur ajoute lui-même {@code groups = Create/Update}), puis écrit les DTOs dans {@code dto/} et le
 * Mapper à côté de l'entité. <strong>Les fichiers existants ne sont jamais écrasés</strong> — ce sont
 * ensuite des fichiers normaux, à adapter librement.
 *
 * <p><strong>Relations JPA aplaties en UUID</strong> (pas d'imbrication, REST) : un {@code @ManyToOne}/
 * {@code @OneToOne} devient {@code UUID xId}, un {@code @OneToMany}/{@code @ManyToMany} devient
 * {@code List<UUID> xIds}. Le Mapper généré reçoit alors les entités liées en paramètres (le service les
 * charge par UUID et les passe déjà résolues) ; le détail des relations se récupère via des endpoints
 * dédiés (sous-ressources).
 */
public class DtoGenerator {

    /** Contraintes Bean Validation recopiées dans les DTOs. */
    private static final Set<String> BV_ANNOTATIONS = Set.of(
        "NotNull", "NotBlank", "NotEmpty",
        "Size", "Min", "Max", "DecimalMin", "DecimalMax", "Digits",
        "Email", "Pattern",
        "Positive", "PositiveOrZero", "Negative", "NegativeOrZero",
        "Past", "PastOrPresent", "Future", "FutureOrPresent",
        "AssertTrue", "AssertFalse");

    /** Annotations de présence — retirées des champs (optionnels) du UpdateDto. */
    private static final Set<String> PRESENCE_ANNOTATIONS = Set.of("NotNull", "NotBlank", "NotEmpty");

    /** Annotations de relation JPA → aplaties en UUID ("toOne" : UUID xId ; "toMany" : List<UUID> xIds). */
    private static final Map<String, String> RELATION_ANNOTATIONS = Map.of(
        "ManyToOne", "toOne",
        "OneToOne", "toOne",
        "OneToMany", "toMany",
        "ManyToMany", "toMany");

    /** Champs hérités de BaseEntity, ignorés à la génération. */
    private static final Set<String> BASE_ENTITY_FIELDS = Set.of(
        "id", "createdAt", "updatedAt", "createdBy", "updatedBy");

    /**
     * @param relation   "none" pour un champ scalaire, sinon "toOne"/"toMany"
     * @param targetType entité liée (élément pour to-many : "User", "Document") — relations seulement
     */
    private record Field(String type, String name, List<String> annotations, String relation, String targetType) {}

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage : java tools/DtoGenerator.java <Entité>   (ex: java tools/DtoGenerator.java Entite)");
            System.exit(1);
        }
        String entity = Character.toUpperCase(args[0].charAt(0)) + args[0].substring(1);

        Path javaRoot = resolveJavaRoot();
        if (javaRoot == null) {
            System.err.println("Dossier src/main/java introuvable — lance la commande depuis backend/ ou la racine du projet.");
            System.exit(1);
        }
        Path entityFile = findEntityFile(javaRoot, entity + ".java");
        if (entityFile == null) {
            System.err.println("Entité \"" + entity + ".java\" introuvable sous " + javaRoot);
            System.exit(1);
        }

        String source = Files.readString(entityFile);
        String pkg = parsePackage(source);
        Map<String, String> imports = parseImports(source);
        List<Field> fields = parseFields(source);
        if (fields.isEmpty()) {
            System.err.println("Aucun champ trouvé dans " + entity + ".java (hors BaseEntity).");
            System.exit(1);
        }

        Path dir = entityFile.getParent();
        Path dtoDir = dir.resolve("dto");
        Files.createDirectories(dtoDir);
        String dtoPkg = pkg + ".dto";

        Map<Path, String> files = new LinkedHashMap<>();
        files.put(dtoDir.resolve(entity + "CreateDto.java"), renderCreateDto(dtoPkg, entity, fields, imports, pkg));
        files.put(dtoDir.resolve(entity + "UpdateDto.java"), renderUpdateDto(dtoPkg, entity, fields, imports, pkg));
        files.put(dtoDir.resolve(entity + "DetailDto.java"), renderDetailDto(dtoPkg, entity, fields, imports, pkg));
        files.put(dir.resolve(entity + "Mapper.java"), renderMapper(pkg, entity, fields, imports));

        List<String> written = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Map.Entry<Path, String> e : files.entrySet()) {
            if (Files.exists(e.getKey())) {
                skipped.add(e.getKey().getFileName().toString());
            } else {
                Files.writeString(e.getKey(), e.getValue());
                written.add(e.getKey().getFileName().toString());
            }
        }

        if (!written.isEmpty()) {
            System.out.println("Fichiers créés :");
            written.forEach(f -> System.out.println("  + " + f));
        }
        if (!skipped.isEmpty()) {
            System.out.println("Ignorés (existent déjà) :");
            skipped.forEach(f -> System.out.println("  ~ " + f));
        }
        System.out.println("Terminé. Ce sont désormais des fichiers normaux — adapte-les si besoin.");
    }

    // ── Résolution des chemins ───────────────────────────────────────────────────

    private static Path resolveJavaRoot() {
        for (Path candidate : new Path[]{ Path.of("src/main/java"), Path.of("backend/src/main/java") }) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path findEntityFile(Path root, String fileName) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(pp -> pp.getFileName().toString().equals(fileName)).findFirst().orElse(null);
        }
    }

    // ── Parsing ──────────────────────────────────────────────────────────────────

    private static String parsePackage(String source) {
        Matcher m = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE).matcher(source);
        return m.find() ? m.group(1) : "";
    }

    /** Imports de l'entité, indexés par nom simple — sert à reporter les types de relation au Mapper. */
    private static Map<String, String> parseImports(String source) {
        Map<String, String> imports = new LinkedHashMap<>();
        Matcher m = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);", Pattern.MULTILINE).matcher(source);
        while (m.find()) {
            String fqn = m.group(1);
            imports.put(fqn.substring(fqn.lastIndexOf('.') + 1), "import " + fqn + ";");
        }
        return imports;
    }

    private static String annotationName(String raw) {
        return raw.replaceFirst("^@", "").replaceFirst("\\(.*$", "");
    }

    private static List<Field> parseFields(String source) {
        List<Field> fields = new ArrayList<>();
        List<String> pending = new ArrayList<>();
        Pattern fieldPattern = Pattern.compile("^private\\s+(?!static|final)(\\S+)\\s+(\\w+)\\s*;");
        for (String rawLine : source.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("@")) {
                pending.add(line);
                continue;
            }
            Matcher fm = fieldPattern.matcher(line);
            if (fm.find()) {
                String type = fm.group(1);
                String name = fm.group(2);
                if (!BASE_ENTITY_FIELDS.contains(name)) {
                    List<String> bv = pending.stream()
                        .filter(a -> BV_ANNOTATIONS.contains(annotationName(a)))
                        .collect(Collectors.toList());
                    String relation = "none";
                    for (String a : pending) {
                        String kind = RELATION_ANNOTATIONS.get(annotationName(a));
                        if (kind != null) {
                            relation = kind;
                            break;
                        }
                    }
                    String targetType = "toMany".equals(relation) ? elementType(type) : type;
                    fields.add(new Field(type, name, bv, relation, targetType));
                }
                pending.clear();
                continue;
            }
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("*")) {
                pending.clear();
            }
        }
        return fields;
    }

    // ── Aplatissement des relations en UUID ───────────────────────────────────────

    /** Singularisation naïve : retire un "s" final ("documents" → "document"). */
    private static String singularize(String name) {
        return name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
    }

    /** Extrait le type d'élément d'une collection ("List<Document>" → "Document"). */
    private static String elementType(String listType) {
        Matcher m = Pattern.compile("<\\s*([\\w.]+)\\s*>").matcher(listType);
        return m.find() ? m.group(1) : listType;
    }

    /** Type exposé dans le DTO : UUID(s) pour une relation, type d'origine sinon. */
    private static String dtoType(Field f) {
        if ("toOne".equals(f.relation())) return "UUID";
        if ("toMany".equals(f.relation())) return "List<UUID>";
        return f.type();
    }

    /** Nom exposé dans le DTO : "userId" / "documentIds" pour une relation, nom d'origine sinon. */
    private static String dtoName(Field f) {
        if ("toOne".equals(f.relation())) return f.name() + "Id";
        if ("toMany".equals(f.relation())) return singularize(f.name()) + "Ids";
        return f.name();
    }

    private static Field plain(String type, String name) {
        return plain(type, name, List.of());
    }

    private static Field plain(String type, String name, List<String> annotations) {
        return new Field(type, name, annotations, "none", type);
    }

    /** Types java.lang (jamais d'import) et conteneurs java.util synthétisés/usuels. */
    private static final Set<String> JAVA_LANG = Set.of(
        "String", "Integer", "Long", "Double", "Float", "Short", "Byte", "Boolean", "Character",
        "Object", "Number", "CharSequence", "Void", "Math", "Enum", "Class", "Comparable",
        "Iterable", "Runnable", "Thread", "Exception", "RuntimeException", "Error", "Throwable",
        "StringBuilder");
    private static final Set<String> JAVA_UTIL = Set.of(
        "List", "Set", "Map", "Collection", "Queue", "Deque", "Optional", "UUID");

    private static final Pattern TYPE_TOKEN = Pattern.compile("(?<![\\w.])[A-Z]\\w*");

    /**
     * Imports nécessaires aux types de DTO. Pour chaque type <strong>simple</strong> référencé (pas un nom
     * qualifié), en ordre : java.lang ignoré · {@code Instant}/conteneurs java.util synthétisés en dur ·
     * sinon <strong>report de l'import depuis l'entité source</strong> (LocalDate, BigDecimal, enums/types
     * d'autres packages…) · sinon repli « même package que l'entité » (le DTO vit dans {@code <pkg>.dto}).
     */
    private static String collectImports(List<Field> fields, Map<String, String> entityImports, String entityPkg) {
        Set<String> set = new TreeSet<>();
        for (Field f : fields) {
            Matcher m = TYPE_TOKEN.matcher(dtoType(f));
            while (m.find()) {
                String token = m.group();
                if (JAVA_LANG.contains(token)) {
                    continue;
                }
                if ("Instant".equals(token)) {
                    set.add("java.time.Instant");
                    continue;
                }
                if (JAVA_UTIL.contains(token)) {
                    set.add("java.util." + token);
                    continue;
                }
                String imp = entityImports.get(token);
                if (imp != null) {
                    set.add(imp.replaceFirst("^import\\s+", "").replaceFirst(";$", ""));
                    continue;
                }
                if (!entityPkg.isEmpty()) {
                    set.add(entityPkg + "." + token); // type du même package que l'entité
                }
            }
        }
        return set.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n"));
    }

    // ── Imports Bean Validation ───────────────────────────────────────────────────

    private static String bvImports(List<Field> fields) {
        Set<String> used = new TreeSet<>();
        for (Field f : fields) {
            for (String a : f.annotations()) {
                used.add(annotationName(a));
            }
        }
        if (used.isEmpty()) {
            return "";
        }
        return used.stream()
            .map(n -> "import jakarta.validation.constraints." + n + ";")
            .collect(Collectors.joining("\n")) + "\n";
    }

    private static String withGroup(String annotation, String group) {
        String name = annotationName(annotation);
        int paren = annotation.indexOf('(');
        String args = paren >= 0 ? annotation.substring(paren) : "";
        String argsWithGroup = args.isEmpty()
            ? "(groups = ValidationGroups." + group + ".class)"
            : args.replaceFirst("\\)$", ", groups = ValidationGroups." + group + ".class)");
        return "    @" + name + argsWithGroup;
    }

    private static String renderParams(List<Field> fields, String group) {
        List<String> blocks = new ArrayList<>();
        for (Field f : fields) {
            String anns = f.annotations().stream()
                .map(a -> withGroup(a, group))
                .collect(Collectors.joining("\n"));
            blocks.add((anns.isEmpty() ? "" : anns + "\n") + "    " + dtoType(f) + " " + dtoName(f));
        }
        return String.join(",\n\n", blocks);
    }

    // ── Rendu des fichiers ────────────────────────────────────────────────────────

    private static String renderCreateDto(String pkg, String entity, List<Field> fields, Map<String, String> entityImports, String entityPkg) {
        String ji = collectImports(fields, entityImports, entityPkg);
        return "package " + pkg + ";\n\n"
            + (ji.isEmpty() ? "" : ji + "\n")
            + bvImports(fields)
            + "import platform.common.validation.ValidationGroups;\n\n"
            + "/**\n * DTO de création de {@link " + entity + "}. Relations aplaties en UUID (cf. Mapper).\n */\n"
            + "public record " + entity + "CreateDto(\n\n"
            + renderParams(fields, "Create") + "\n) {}\n";
    }

    private static String renderUpdateDto(String pkg, String entity, List<Field> fields, Map<String, String> entityImports, String entityPkg) {
        List<Field> optional = new ArrayList<>();
        optional.add(plain("UUID", "id", List.of("@NotNull")));
        for (Field f : fields) {
            List<String> kept = f.annotations().stream()
                .filter(a -> !PRESENCE_ANNOTATIONS.contains(annotationName(a)))
                .collect(Collectors.toList());
            optional.add(new Field(f.type(), f.name(), kept, f.relation(), f.targetType()));
        }
        return "package " + pkg + ";\n\n"
            + collectImports(optional, entityImports, entityPkg) + "\n"
            + bvImports(optional)
            + "import platform.common.validation.ValidationGroups;\n\n"
            + "/**\n * DTO de mise à jour partielle de {@link " + entity + "}. Relations aplaties en UUID (cf. Mapper).\n"
            + " * Seuls les champs non nuls sont appliqués. {@code id} est obligatoire.\n */\n"
            + "public record " + entity + "UpdateDto(\n\n"
            + renderParams(optional, "Update") + "\n) {}\n";
    }

    private static String renderDetailDto(String pkg, String entity, List<Field> fields, Map<String, String> entityImports, String entityPkg) {
        List<Field> all = new ArrayList<>();
        all.add(plain("UUID", "id"));
        all.addAll(fields);
        all.add(plain("Instant", "createdAt"));
        all.add(plain("Instant", "updatedAt"));
        String params = all.stream()
            .map(f -> "    " + dtoType(f) + " " + dtoName(f))
            .collect(Collectors.joining(",\n"));
        return "package " + pkg + ";\n\n"
            + collectImports(all, entityImports, entityPkg) + "\n\n"
            + "/**\n * DTO de réponse complet de {@link " + entity + "}. Relations exposées en UUID (cf. Mapper) ;\n"
            + " * le détail des entités liées se récupère via des endpoints dédiés (sous-ressources).\n */\n"
            + "public record " + entity + "DetailDto(\n\n"
            + params + "\n) {}\n";
    }

    private static String renderMapper(String pkg, String entity, List<Field> fields, Map<String, String> entityImports) {
        List<Field> relations = fields.stream()
            .filter(f -> !"none".equals(f.relation()))
            .collect(Collectors.toList());

        // Aucune relation → mapper « plat ».
        if (relations.isEmpty()) {
            return "package " + pkg + ";\n\n"
                + "import " + pkg + ".dto." + entity + "CreateDto;\n"
                + "import " + pkg + ".dto." + entity + "DetailDto;\n"
                + "import " + pkg + ".dto." + entity + "UpdateDto;\n"
                + "import org.mapstruct.BeanMapping;\n"
                + "import org.mapstruct.Mapper;\n"
                + "import org.mapstruct.MappingTarget;\n"
                + "import org.mapstruct.NullValuePropertyMappingStrategy;\n\n"
                + "/**\n * Mapper MapStruct pour l'entité {@link " + entity + "}.\n */\n"
                + "@Mapper(componentModel = \"spring\")\n"
                + "public interface " + entity + "Mapper {\n\n"
                + "    " + entity + " toEntity(" + entity + "CreateDto dto);\n\n"
                + "    /** Update partiel (PATCH) : un champ null du DTO n'écrase pas la valeur existante. */\n"
                + "    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)\n"
                + "    void merge(" + entity + "UpdateDto dto, @MappingTarget " + entity + " entity);\n\n"
                + "    " + entity + "DetailDto toDto(" + entity + " entity);\n"
                + "}\n";
        }

        List<Field> toMany = relations.stream()
            .filter(r -> "toMany".equals(r.relation()))
            .collect(Collectors.toList());

        // ── Imports (conditionnels pour ne pas déclencher UnusedImports) ──
        Set<String> fqns = new TreeSet<>();
        fqns.add(pkg + ".dto." + entity + "CreateDto");
        fqns.add(pkg + ".dto." + entity + "DetailDto");
        fqns.add(pkg + ".dto." + entity + "UpdateDto");
        fqns.add("org.mapstruct.BeanMapping");
        fqns.add("org.mapstruct.Mapper");
        fqns.add("org.mapstruct.Mapping");
        fqns.add("org.mapstruct.MappingTarget");
        fqns.add("org.mapstruct.NullValuePropertyMappingStrategy");
        if (!toMany.isEmpty()) {
            fqns.add("java.util.List"); // type des paramètres collection
            fqns.add("java.util.UUID"); // type de retour des helpers toId(...)
        }
        // Reporte l'import du type d'entité liée depuis l'entité source (sinon : même package, pas d'import).
        for (Field r : relations) {
            String imp = entityImports.get(r.targetType());
            if (imp != null) {
                fqns.add(imp.replaceFirst("^import\\s+", "").replaceFirst(";$", ""));
            }
        }
        String importBlock = fqns.stream().map(f -> "import " + f + ";").collect(Collectors.joining("\n"));

        // ── Mappings ──
        StringBuilder createParams = new StringBuilder(entity + "CreateDto dto");
        StringBuilder updateParams = new StringBuilder(entity + "UpdateDto dto");
        for (Field r : relations) {
            createParams.append(", ").append(r.type()).append(" ").append(r.name());
            updateParams.append(", ").append(r.type()).append(" ").append(r.name());
        }
        String writeMappings = relations.stream()
            .map(r -> "    @Mapping(target = \"" + r.name() + "\", source = \"" + r.name() + "\")")
            .collect(Collectors.joining("\n"));
        String readMappings = relations.stream()
            .map(r -> "toOne".equals(r.relation())
                ? "    @Mapping(target = \"" + dtoName(r) + "\", source = \"" + r.name() + ".id\")"
                : "    @Mapping(target = \"" + dtoName(r) + "\", source = \"" + r.name() + "\")")
            .collect(Collectors.joining("\n"));

        // Un helper toId(...) par type d'entité de collection (résout List<X> → List<UUID>).
        Set<String> helperTypes = new LinkedHashSet<>();
        for (Field r : toMany) {
            helperTypes.add(r.targetType());
        }
        String helpers = helperTypes.stream()
            .map(t -> {
                String p = Character.toLowerCase(t.charAt(0)) + t.substring(1);
                return "    default UUID toId(" + t + " " + p + ") {\n"
                    + "        return " + p + " == null ? null : " + p + ".getId();\n"
                    + "    }";
            })
            .collect(Collectors.joining("\n\n"));

        return "package " + pkg + ";\n\n"
            + importBlock + "\n\n"
            + "/**\n * Mapper MapStruct pour l'entité {@link " + entity + "}.\n *\n"
            + " * <p>Relations aplaties en UUID (pas d'imbrication, REST). Le service charge les entités liées\n"
            + " * par UUID et les passe déjà résolues à {@code toEntity}/{@code merge} ; le mapper reste pur\n"
            + " * (aucun accès base). Le détail des relations se récupère via des endpoints dédiés (sous-ressources).\n */\n"
            + "@Mapper(componentModel = \"spring\")\n"
            + "public interface " + entity + "Mapper {\n\n"
            + writeMappings + "\n"
            + "    " + entity + " toEntity(" + createParams + ");\n\n"
            + "    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)\n"
            + writeMappings + "\n"
            + "    void merge(" + updateParams + ", @MappingTarget " + entity + " entity);\n\n"
            + readMappings + "\n"
            + "    " + entity + "DetailDto toDto(" + entity + " entity);\n\n"
            + helpers + "\n"
            + "}\n";
    }
}
