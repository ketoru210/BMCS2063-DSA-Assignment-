package shared.adt;

/**
 * Generic List ADT supporting random access by index.
 */
public interface ListInterface<T> {

    /** Appends an element to the end of the list. */
    void add(T element);

    /** Inserts an element at the given index. */
    void add(int index, T element);

    /** Returns the element at the given index. */
    T get(int index);

    /** Replaces the element at the given index; returns the old element. */
    T set(int index, T element);

    /** Removes and returns the element at the given index. */
    T remove(int index);

    /** Returns the index of the first matching element, or -1 if absent. */
    int indexOf(T element);

    /** Returns true if the list contains the element. */
    boolean contains(T element);

    /** Returns true if the list has no elements. */
    boolean isEmpty();

    /** Returns the number of elements in the list. */
    int size();

    /** Removes all elements from the list. */
    void clear();
}
