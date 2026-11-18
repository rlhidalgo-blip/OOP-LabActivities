package bookbridge.util;

import java.util.Map;
import java.util.NoSuchElementException;

/** Generic lookup shared by the member, book, and loan registries. */
public final class Lookup {
    private Lookup() { }

    public static <K, V> V require(Map<K, V> registry, K id, String entity) {
        V value = registry.get(id);
        if (value == null) {
            throw new NoSuchElementException(entity + " not found: " + id);
        }
        return value;
    }
}
