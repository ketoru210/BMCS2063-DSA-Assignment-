package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Linked binary search tree. "First" is the smallest key, "last" is the
 * largest key, and the iterator walks in ascending key order.
 * <p>
 * Empty-collection operations return {@code null} and never throw.
 *
 * @author QW
 */
public class BinarySearchTree<T extends Comparable<T>> implements CollectionInterface<T> {

    private Node root;
    private int size;

    private class Node {

        private T data;
        private Node left;
        private Node right;

        private Node(T data) {
            this.data = data;
        }
    }

    @Override
    public boolean add(T newEntry) {
        if (newEntry == null) {
            return false;
        }
        int oldSize = size;
        root = insert(root, newEntry);
        return size > oldSize;
    }

    private Node insert(Node node, T entry) {
        if (node == null) {
            size++;
            return new Node(entry);
        }
        int cmp = entry.compareTo(node.data);
        if (cmp < 0) {
            node.left = insert(node.left, entry);
        } else if (cmp > 0) {
            node.right = insert(node.right, entry);
        }
        // duplicate key: deliberately no branch, so size stays put and add() reports false
        return node;
    }

    @Override
    public T remove() {
        if (root == null) {
            return null;
        }
        T smallest = minNode(root).data;
        root = deleteMin(root);
        size--;
        return smallest;
    }

    @Override
    public boolean remove(T anEntry) {
        if (anEntry == null) {
            return false;
        }
        int oldSize = size;
        root = delete(root, anEntry);
        return size < oldSize;
    }

    private Node delete(Node node, T entry) {
        if (node == null) {
            return null;
        }
        int cmp = entry.compareTo(node.data);
        if (cmp < 0) {
            node.left = delete(node.left, entry);
        } else if (cmp > 0) {
            node.right = delete(node.right, entry);
        } else {
            size--;
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }
            // two children: overwrite with the successor's value, then unlink the
            // successor node, which has no left child and so needs only deleteMin
            node.data = minNode(node.right).data;
            node.right = deleteMin(node.right);
        }
        return node;
    }

    private Node minNode(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private Node deleteMin(Node node) {
        if (node.left == null) {
            return node.right;
        }
        node.left = deleteMin(node.left);
        return node;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public T getFirst() {
        if (root == null) {
            return null;
        }
        return minNode(root).data;
    }

    @Override
    public T getLast() {
        if (root == null) {
            return null;
        }
        Node current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.data;
    }

    @Override
    public T search(T probe) {
        if (probe == null) {
            return null;
        }
        Node current = root;
        while (current != null) {
            int cmp = probe.compareTo(current.data);
            if (cmp == 0) {
                return current.data;
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return search(anEntry) != null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<T> getIterator() {
        return new SnapshotIterator(null, null);
    }

    /** Iterator over the keys in [low, high] only, still in ascending order. */
    public Iterator<T> getRangeIterator(T low, T high) {
        return new SnapshotIterator(low, high);
    }

    private class SnapshotIterator implements Iterator<T> {

        private final T[] snapshot;
        private final T low;
        private final T high;
        private int filled;
        private int cursor;

        @SuppressWarnings("unchecked")
        private SnapshotIterator(T low, T high) {
            this.low = low;
            this.high = high;
            snapshot = (T[]) new Comparable[size];
            fill(root);
        }

        private void fill(Node node) {
            if (node == null) {
                return;
            }
            boolean aboveLow = low == null || low.compareTo(node.data) <= 0;
            boolean belowHigh = high == null || high.compareTo(node.data) >= 0;
            if (aboveLow) {
                fill(node.left);
            }
            if (aboveLow && belowHigh) {
                snapshot[filled++] = node.data;
            }
            if (belowHigh) {
                fill(node.right);
            }
        }

        @Override
        public boolean hasNext() {
            return cursor < filled;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return snapshot[cursor++];
        }
    }

    /** Smallest stored key strictly greater than probe; null if none. */
    public T successorOf(T probe) {
        if (probe == null) {
            return null;
        }
        Node current = root;
        T candidate = null;
        while (current != null) {
            if (probe.compareTo(current.data) < 0) {
                candidate = current.data;
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return candidate;
    }

    /** Largest stored key strictly smaller than probe; null if none. */
    public T predecessorOf(T probe) {
        if (probe == null) {
            return null;
        }
        Node current = root;
        T candidate = null;
        while (current != null) {
            if (probe.compareTo(current.data) > 0) {
                candidate = current.data;
                current = current.right;
            } else {
                current = current.left;
            }
        }
        return candidate;
    }

    public int getHeight() {
        return height(root);
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    /**
     * Level-order slots: index 0 is the root, children of i sit at 2i+1 and 2i+2,
     * and a missing node leaves a null. Lets a caller draw the shape of the tree
     * without being handed the nodes themselves. Object[] rather than T[]: a
     * generic array here would fail the caller's implicit cast at run time.
     */
    public Object[] toLevelOrderArray() {
        Object[] slots = new Object[(1 << getHeight()) - 1];
        place(root, 0, slots);
        return slots;
    }

    private void place(Node node, int index, Object[] slots) {
        if (node == null || index >= slots.length) {
            return;
        }
        slots[index] = node.data;
        place(node.left, 2 * index + 1, slots);
        place(node.right, 2 * index + 2, slots);
    }
}