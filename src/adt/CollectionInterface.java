package adt;

import java.util.Iterator;

/**
 * Team ADT — a policy-ordered collection.
 * <p>
 * Each implementation defines its own organizing policy, which determines
 * the meaning of "first"/"last" and the traversal order
 * (e.g. highest priority for a heap, most recently pushed for a stack,
 * smallest key for a binary search tree, head for a linked list).
 * <p>
 * Empty-collection convention: {@code remove()}, {@code getFirst()},
 * {@code getLast()} and {@code search()} return {@code null}; they never throw.
 *
 * @author Lam Yong Zhe
 * @author Fong Qin Wen
 * @author Lai Kang Yong
 * @author Wong Pu Jin
 */
public interface CollectionInterface<T extends Comparable<T>> {

    // ---- insertion ----

    /** Inserts newEntry under this implementation's organizing policy. */
    boolean add(T newEntry);

    // ---- removal ----

    /** Removes and returns the POLICY-FIRST element; null if empty. */
    T remove();

    /** Locates and removes a specific element; false if absent. */
    boolean remove(T anEntry);

    /** Removes all elements. */
    void clear();

    // ---- access / query ----

    /** Returns the policy-first element without removing it; null if empty. */
    T getFirst();

    /** Returns the policy-LAST element without removing it; null if empty. */
    T getLast();

    /** Returns the stored element with compareTo(probe) == 0; null if absent. */
    T search(T probe);

    /** Returns true if an element with compareTo(anEntry) == 0 is stored. */
    boolean contains(T anEntry);

    // ---- status ----

    /** Returns the number of stored elements. */
    int size();

    /** Returns true if no elements are stored. */
    boolean isEmpty();

    // ---- traversal ----

    /** Returns an iterator over the elements in this implementation's policy order. */
    Iterator<T> getIterator();
}
