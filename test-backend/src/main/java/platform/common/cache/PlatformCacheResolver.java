package platform.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import platform.common.service.BaseCrudService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CacheResolver qui délègue le nom du cache à {@link BaseCrudService#getCacheName()}.
 * <ul>
 *   <li>{@code search}   → cache {@code <name>:search}</li>
 *   <li>write methods    → les deux caches (findOne + search) pour tout invalider</li>
 *   <li>autres reads     → cache {@code <name>} (findOne)</li>
 * </ul>
 */
public class PlatformCacheResolver implements CacheResolver {

    private static final String SEARCH_SUFFIX = ":search";
    private static final Set<String> WRITE_METHODS = Set.of("create", "update", "remove");

    private final CacheManager cacheManager;

    public PlatformCacheResolver(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        if (!(context.getTarget() instanceof BaseCrudService<?, ?, ?, ?> service)) {
            return List.of();
        }

        String name   = service.getCacheName();
        String method = context.getMethod().getName();

        if ("search".equals(method)) {
            return resolve(name + SEARCH_SUFFIX);
        }
        if (WRITE_METHODS.contains(method)) {
            return resolve(name, name + SEARCH_SUFFIX);
        }
        return resolve(name);
    }

    private List<Cache> resolve(String... names) {
        return Arrays.stream(names)
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
