package shared.adt;

/**
 * Generic Stack ADT (LIFO).
 */
public interface StackInterface<T> {

    /** Pushes an element onto the top of the stack. */
    void push(T element);

    /** Removes and returns the element at the top of the stack. */
    T pop();

    /** Returns (without removing) the element at the top of the stack. */
    T peek();

    /** Returns true if the stack has no elements. */
    boolean isEmpty();

    /** Returns the number of elements in the stack. */
    int size();

    /** Removes all elements from the stack. */
    void clear();
}
