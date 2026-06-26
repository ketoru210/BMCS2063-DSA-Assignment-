package shared.adt;

/**
 * Generic Heap ADT (priority queue).
 * The highest-priority element is always exposed at the root.
 */
public interface HeapInterface<T> {

    /** Inserts an element, reorganizing the heap to maintain heap order. */
    void insert(T element);

    /** Removes and returns the root (highest-priority) element. */
    T remove();

    /** Returns (without removing) the root element. */
    T peek();

    /** Returns true if the heap has no elements. */
    boolean isEmpty();

    /** Returns the number of elements in the heap. */
    int size();

    /** Removes all elements from the heap. */
    void clear();
}
