package shared.adt;

/**
 * Generic Hash Map ADT for O(1) average-case lookup by key.
 */
public interface HashInterface<K, V> {

    /** Associates the value with the key; returns the previous value or null. */
    V put(K key, V value);

    /** Returns the value mapped to the key, or null if absent. */
    V get(K key);

    /** Removes the mapping for the key; returns the removed value or null. */
    V remove(K key);

    /** Returns true if the key has a mapping. */
    boolean containsKey(K key);

    /** Returns true if the map has no entries. */
    boolean isEmpty();

    /** Returns the number of entries in the map. */
    int size();

    /** Removes all entries from the map. */
    void clear();
}
