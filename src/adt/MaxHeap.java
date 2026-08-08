package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Module 2 implementation of the team ADT, backed by an array.
 * <p>
 * Organizing policy: <b>maximum first</b>. The policy-first element returned by
 * {@code remove()} and {@code getFirst()} is the greatest element by
 * {@code compareTo}; the policy-last element returned by {@code getLast()} is
 * the smallest.
 * <p>
 * Physically the entries live in one array; logically they form a complete
 * binary tree stored level by level from <b>index 0</b>. For the node at index
 * {@code i} the children are at {@code 2i + 1} and {@code 2i + 2} and the
 * parent is at {@code (i - 1) / 2}, so no links are stored.
 * <p>
 * {@code getIterator()} walks the array in level order, which is NOT sorted
 * order — the heap property only orders a parent against its own descendants.
 *
 * @author YZ
 */
public class MaxHeap<T extends Comparable<T>> implements CollectionInterface<T> {
    private static final int DEFAULT_CAPACITY = 20;

    /** Entries in level order; slots 0 .. size-1 are in use. */
    private T[] heap;

    /** Number of entries currently stored. */
    private int size;

    public MaxHeap() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings({"unchecked"})
    public MaxHeap(int initialCapacity) {
        if (initialCapacity < 1) {
            initialCapacity = DEFAULT_CAPACITY;
        }
        // a generic array cannot be created directly (new T[] is illegal),
        // so create the Comparable[] that the type bound guarantees and narrow it
        heap = (T[]) new Comparable[initialCapacity];
        size = 0;
    }

    // --- insertion ---

    @Override
    public boolean add(T newEntry) {
        if (newEntry == null) {
            return false;
        }
        ensureCapacity();
        heap[size] = newEntry;
        size++;
        siftUp(size - 1);
        return true;
    }

    // --- removal ---

    @Override
    public T remove() {
        if (size == 0) {
            return null;
        }
        T first = heap[0];
        // only the last slot can be dropped without leaving a gap
        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;
        if (size > 0) {
            siftDown(0);
        }
        return first;
    }

    @Override
    public boolean remove(T anEntry) {
        int index = indexOf(anEntry);
        if (index < 0) {
            return false;
        }
        heap[index] = heap[size - 1];
        heap[size - 1] = null;
        size--;
        if (index < size) {
            // the replacement goes up or down, never both
            if (index > 0 && heap[index].compareTo(heap[(index - 1) / 2]) > 0) {
                siftUp(index);
            } else {
                siftDown(index);
            }
        }
        return true;
    }

    @Override
    public void clear() {
        // drop the references so the entries can be garbage collected
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    // --- access / query ---

    @Override
    public T getFirst() {
        return size == 0 ? null : heap[0];
    }

    @Override
    public T getLast() {
        // the minimum is always a leaf; leaves start at size/2 (empty when size is 0)
        T smallest = null;
        for (int i = size / 2; i < size; i++) {
            if (smallest == null || heap[i].compareTo(smallest) < 0) {
                smallest = heap[i];
            }
        }
        return smallest;
    }

    @Override
    public T search(T probe) {
        int index = indexOf(probe);
        return index < 0 ? null : heap[index];
    }

    @Override
    public boolean contains(T anEntry) {
        return indexOf(anEntry) >= 0;
    }

    // --- status ---

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    // --- traversal ---

    @Override
    public Iterator<T> getIterator() {
        return new LevelOrderIterator();
    }

    /**
     * Walks slots 0 .. size-1, which is level order for this representation.
     */
    private class LevelOrderIterator implements Iterator<T> {

        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return heap[cursor++];
        }
    }

    // --- private helpers ---

    /**
     * Returns the index of the entry equal to probe, or -1 if it is absent.
     * Shared by search, contains and remove(T).
     */
    private int indexOf(T probe) {
        return probe == null ? -1 : indexOf(probe, 0);
    }

    /**
     * Searches the subtree rooted at the given index. A subtree whose root
     * ranks below probe cannot contain it, so it is skipped whole.
     */
    private int indexOf(T probe, int index) {
        if (index >= size) {
            return -1;
        }
        int order = heap[index].compareTo(probe);
        if (order < 0) {
            return -1;
        }
        if (order == 0) {
            return index;
        }
        int found = indexOf(probe, 2 * index + 1);
        return found >= 0 ? found : indexOf(probe, 2 * index + 2);
    }

    /**
     * Moves the entry at the given index up until its parent is not smaller.
     */
    private void siftUp(int index) {
        // hold the entry aside and shift parents down: one assignment per level
        T moving = heap[index];
        int child = index;
        int parent = (child - 1) / 2;
        while (child > 0 && moving.compareTo(heap[parent]) > 0) {
            heap[child] = heap[parent];
            child = parent;
            parent = (child - 1) / 2;
        }
        heap[child] = moving;
    }

    /**
     * Moves the entry at the given index down until neither child is greater.
     */
    private void siftDown(int index) {
        T moving = heap[index];
        int parent = index;
        int child = 2 * parent + 1;
        while (child < size) {               // bound is size, not heap.length
            int right = child + 1;
            if (right < size && heap[right].compareTo(heap[child]) > 0) {
                child = right;
            }
            if (moving.compareTo(heap[child]) >= 0) {
                break;
            }
            heap[parent] = heap[child];
            parent = child;
            child = 2 * parent + 1;
        }
        heap[parent] = moving;
    }

    /**
     * Doubles the array when it is full.
     */
    @SuppressWarnings({"unchecked"})
    private void ensureCapacity() {
        if (size < heap.length) {
            return;
        }
        T[] bigger = (T[]) new Comparable[heap.length * 2];
        System.arraycopy(heap, 0, bigger, 0, size);
        heap = bigger;
    }
}
