package housekeeping.entity;

import shared.adt.StackInterface;

/**
 * Entity: LIFO stack of tasks used for undo / redo (Module 3 data structure).
 */
public class TaskStack<T> implements StackInterface<T> {

    @Override
    public void push(T element) {
        // TODO
    }

    @Override
    public T pop() {
        // TODO
        return null;
    }

    @Override
    public T peek() {
        // TODO
        return null;
    }

    @Override
    public boolean isEmpty() {
        // TODO
        return true;
    }

    @Override
    public int size() {
        // TODO
        return 0;
    }

    @Override
    public void clear() {
        // TODO
    }
}
