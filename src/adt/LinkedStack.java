package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Module 3 implementation of the team ADT, backed by a singly linked chain.
 * <p>
 * Organizing policy: <b>LIFO (Last-In-First-Out)</b>. The policy-first element
 * returned by {@code remove()} and {@code getFirst()} is the top of the stack.
 *
 * @param <T> the type of elements held in this stack
 * @author Wong Pu Jin
 */
public class LinkedStack<T extends Comparable<T>> implements CollectionInterface<T> {

    private Node topNode;
    private int numberOfEntries;

    public LinkedStack() {
        topNode = null;
        numberOfEntries = 0;
    }

    // --- insertion ---

    @Override
    public boolean add(T newEntry) {
        Node newNode = new Node(newEntry);
        newNode.next = topNode;
        topNode = newNode;
        numberOfEntries++;
        return true;
    }

    // --- removal ---

    @Override
    public T remove() {
        T topData = getFirst();
        if (topData != null) {
            topNode = topNode.next;
            numberOfEntries--;
        }
        return topData;
    }

    @Override
    public boolean remove(T anEntry) {
        if (anEntry == null || isEmpty()) {
            return false;
        }

        if (topNode.data != null && topNode.data.compareTo(anEntry) == 0) {
            remove();
            return true;
        }

        Node currentNode = topNode;
        while (currentNode.next != null) {
            if (currentNode.next.data != null && currentNode.next.data.compareTo(anEntry) == 0) {
                currentNode.next = currentNode.next.next;
                numberOfEntries--;
                return true;
            }
            currentNode = currentNode.next;
        }
        return false;
    }

    @Override
    public void clear() {
        topNode = null;
        numberOfEntries = 0;
    }

    // --- access / query ---

    @Override
    public T getFirst() {
        return isEmpty() ? null : topNode.data;
    }

    @Override
    public T getLast() {
        if (isEmpty()) {
            return null;
        }
        Node currentNode = topNode;
        while (currentNode.next != null) {
            currentNode = currentNode.next;
        }
        return currentNode.data;
    }

    @Override
    public T search(T probe) {
        if (probe == null) {
            return null;
        }
        Node currentNode = topNode;
        while (currentNode != null) {
            if (currentNode.data != null && currentNode.data.compareTo(probe) == 0) {
                return currentNode.data;
            }
            currentNode = currentNode.next;
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return search(anEntry) != null;
    }

    // --- status ---

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return topNode == null;
    }

    // --- traversal ---

    @Override
    public Iterator<T> getIterator() {
        return new StackIterator();
    }

    // --- private inner classes ---

    private class Node {
        private T data;
        private Node next;

        private Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private class StackIterator implements Iterator<T> {
        private Node currentNode = topNode;

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T data = currentNode.data;
            currentNode = currentNode.next;
            return data;
        }
    }
}