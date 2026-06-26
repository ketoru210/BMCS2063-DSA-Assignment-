package shared.adt;

/**
 * Generic Queue ADT (FIFO).
 */
public interface QueueInterface<T> {

    /** Adds an element to the back of the queue. */
    void enqueue(T element);

    /** Removes and returns the element at the front of the queue. */
    T dequeue();

    /** Returns (without removing) the element at the front of the queue. */
    T peek();

    /** Returns true if the queue has no elements. */
    boolean isEmpty();

    /** Returns the number of elements in the queue. */
    int size();

    /** Removes all elements from the queue. */
    void clear();
}
