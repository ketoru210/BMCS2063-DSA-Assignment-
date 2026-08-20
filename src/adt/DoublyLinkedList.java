package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Module 5 implementation of the team ADT, backed by a doubly linked chain.
 * <p>
 * Organizing policy: <b>head first</b>. {@code add} appends at the tail, so the
 * traversal order given by {@code getIterator()} is insertion order.
 *
 * @author Lai Kang Yong
 */
public class DoublyLinkedList<T extends Comparable<T>> implements CollectionInterface<T>{
    private Node firstNode;
    private Node lastNode;
    private int numberOfEntries;
    public DoublyLinkedList() {
        this.firstNode = null;
        this.lastNode = null;
        numberOfEntries = 0;
    }
    @Override
    public boolean add(T newEntry) {
        Node newNote = new Node(newEntry);
        if(isEmpty()) {
            firstNode = lastNode = newNote;
        } else {
            lastNode.next = newNote;
            newNote.previous = lastNode;
            lastNode = newNote;
        }
        numberOfEntries++;
        return true;
    }
    @Override
    public T remove() {
        T result = null;
        if(!isEmpty()) {
            if(firstNode == lastNode) {
                result = firstNode.data;
                firstNode = lastNode = null;
            }
            else {
                result = lastNode.data;
                lastNode = lastNode.previous;
                lastNode.next = null;
            }
            numberOfEntries--;
        }
        return result;
    }
    @Override
    public boolean remove(T newEntry) {
        if(isEmpty()) return false;
        if(lastNode.data.compareTo(newEntry) == 0) {
            remove();
            return true;
        }
        if(firstNode.data.compareTo(newEntry) == 0) {
            firstNode = firstNode.next;
            firstNode.previous = null;
            numberOfEntries--;
            return true;
        }
        Node currentNode = firstNode;
        while(currentNode.next != null) {
            if(currentNode.next.data.compareTo(newEntry) == 0) {
                currentNode.next = currentNode.next.next;
                currentNode.next.previous = currentNode;
                numberOfEntries--;
                return true;
            }
            currentNode = currentNode.next;
        }
        return false;
    }
    @Override
    public void clear() {
        if (!isEmpty()) {
            firstNode = lastNode = null;
            numberOfEntries = 0;
        }
    }
    @Override
    public T getFirst() {
        return isEmpty() ? null : firstNode.data;
    }
    @Override
    public T getLast() {
        return isEmpty() ? null : lastNode.data;
    }
    @Override
    public T search(T probe) {
        Node currentNode = firstNode;
        while(currentNode != null) {
            if(currentNode.data.compareTo(probe) == 0) {
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
        return numberOfEntries == 0;
    }
    @Override
    public Iterator<T> getIterator() {
        return new Cursor();
    }
    public Cursor getCursor() {
        return new Cursor();
    }
    private class Node {
        private T data;
        private Node previous;
        private Node next;
        Node(T data) {
            this.data = data;
            this.previous = null;
            this.next = null;
        }
    }
    public class Cursor implements Iterator<T> {
        Node currentNode = firstNode;
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
        public boolean hasPrevious() {
            if (currentNode == null) {
                return lastNode != null;
            }
            return currentNode.previous != null;
        }
        public T previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            currentNode = (currentNode == null) ? lastNode : currentNode.previous;
            return currentNode.data;
        }
    }
}
