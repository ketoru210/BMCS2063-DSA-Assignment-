package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Team ADT Implementation: Linked Stack for Module 3 (Housekeeping).
 * Organizing Policy: LIFO (Last-In-First-Out).
 * The "first" element is always the top of the stack.
 * 
 * @param <T> the type of elements held in this stack. Must implement Comparable.
 * @author Pujin
 */
public class LinkedStack<T extends Comparable<T>> implements CollectionInterface<T> {

    private Node topNode;
    private int numberOfEntries;

    public LinkedStack() {
        topNode = null;
        numberOfEntries = 0;
    }

    // ==========================================================
    // 1. ADD-ON ALIASES (Your control layer uses these!)
    // ==========================================================
    
    public void push(T newEntry) {
        add(newEntry);
    }

    public T pop() {
        return remove();
    }

    public T peek() {
        return getFirst();
    }

    // ==========================================================
    // 2. COLLECTION INTERFACE IMPLEMENTATION
    // ==========================================================

    @Override
    public boolean add(T newEntry) {
        Node newNode = new Node(newEntry);
        newNode.next = topNode;
        topNode = newNode;
        numberOfEntries++;
        return true; 
    }

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
        if (anEntry == null || isEmpty()) return false;

        // If the element to remove is at the top
        if (topNode.data != null && topNode.data.compareTo(anEntry) == 0) {
            remove();
            return true;
        }

        // Traverse to find the element
        Node currentNode = topNode;
        while (currentNode.next != null) {
            if (currentNode.next.data != null && currentNode.next.data.compareTo(anEntry) == 0) {
                currentNode.next = currentNode.next.next; // Bypass the node
                numberOfEntries--;
                return true;
            }
            currentNode = currentNode.next;
        }
        return false; // Not found
    }

    @Override
    public void clear() {
        topNode = null;
        numberOfEntries = 0;
    }

    @Override
    public T getFirst() {
        return (isEmpty()) ? null : topNode.data;
    }

    @Override
    public T getLast() {
        if (isEmpty()) return null;
        Node currentNode = topNode;
        while (currentNode.next != null) {
            currentNode = currentNode.next;
        }
        return currentNode.data;
    }

    @Override
    public T search(T probe) {
        if (probe == null) return null;
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

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return topNode == null;
    }

    @Override
    public Iterator<T> getIterator() {
        return new StackIterator();
    }

    // ==========================================================
    // 3. INNER CLASSES (Nodes & Iterator)
    // ==========================================================

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