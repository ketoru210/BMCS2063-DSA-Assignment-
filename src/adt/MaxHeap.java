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
 * @author Lam Yong Zhe
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

    // --- add-ons beyond the team interface ---

    /**
     * The entries in level order, which for this representation is the backing
     * array itself. A copy is handed out so that a caller cannot reach in and
     * break the heap property.
     * <p>
     * The element type is {@code Object} rather than {@code T}: the store is
     * really a {@code Comparable[]}, so returning it as {@code T[]} would throw
     * the moment the caller assigned it to an array of the concrete type.
     */
    public Object[] toLevelOrderArray() {
        Object[] slots = new Object[size];
        for (int i = 0; i < size; i++) {
            slots[i] = heap[i];
        }
        return slots;
    }

    /**
     * The number of levels the entries span. Level h holds 2^h nodes and the
     * tree is complete by construction, so the answer follows from the size
     * alone - one step per level, where a linked tree has to visit every node.
     */
    public int getHeight() {
        int height = 0;
        int covered = 0;
        while (covered < size) {
            covered += 1 << height;
            height++;
        }
        return height;
    }

    /**
     * How many stored entries outrank the probe, which is how far down the serve
     * order it sits. Heap order makes most of the walk unnecessary: a subtree
     * whose root does not outrank the probe cannot hold anything that does, so
     * it is skipped whole and only the entries actually counted are ever
     * touched - where reading the same answer off a sorted copy costs n log n.
     */
    public int rankOf(T probe) {
        return probe == null ? -1 : outranking(probe, 0);
    }

    private int outranking(T probe, int index) {
        if (index >= size || heap[index].compareTo(probe) <= 0) {
            return 0;
        }
        return 1 + outranking(probe, 2 * index + 1) + outranking(probe, 2 * index + 2);
    }

    /**
     * Adds every entry of another collection, restoring the heap property once
     * over the whole array instead of once per entry.
     * <p>
     * Repeated {@code add} costs n sift-ups of up to log n steps each. Appending
     * first and then sifting down from the last parent backwards is O(n)
     * instead: half the nodes are leaves and cannot move, a quarter move at most
     * one level, an eighth at most two, and that weighted sum converges to a
     * constant multiple of n rather than growing with log n.
     * <p>
     * Only the shape of the array matters to the rebuild, never the order it
     * arrived in, so the source may be any implementation of the team ADT and
     * its own policy is simply discarded.
     */
    public boolean addAll(CollectionInterface<T> other) {
        if (other == null || other.isEmpty()) {
            return false;
        }
        ensureCapacity(size + other.size());

        Iterator<T> walker = other.getIterator();
        while (walker.hasNext()) {
            T entry = walker.next();
            if (entry != null) {
                heap[size] = entry;
                size++;
            }
        }

        // the last parent sits at size/2 - 1; every index after it is a leaf
        for (int i = size / 2 - 1; i >= 0; i--) {
            siftDown(i);
        }
        return true;
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
        int cmp = heap[index].compareTo(probe);
        if (cmp < 0) {
            return -1;
        }
        if (cmp == 0) {
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
    private void ensureCapacity() {
        ensureCapacity(size + 1);
    }

    /**
     * Grows to hold at least the given number of entries, doubling until it
     * fits so that a bulk add still copies the old contents only once.
     */
    @SuppressWarnings({"unchecked"})
    private void ensureCapacity(int required) {
        if (required <= heap.length) {
            return;
        }
        int capacity = heap.length;
        while (capacity < required) {
            capacity *= 2;
        }
        T[] bigger = (T[]) new Comparable[capacity];
        System.arraycopy(heap, 0, bigger, 0, size);
        heap = bigger;
    }
}
