package adt;

/**
 * @author Pujin
 */
public class LinkedStack<T> implements StackInterface<T> {
    private Node topNode; 

    public LinkedStack() {
        topNode = null;
    }

    @Override
    public void push(T newEntry) {
        Node newNode = new Node(newEntry, topNode);
        topNode = newNode;
    }

    @Override
    public T pop() {
        T top = peek();
        if (topNode != null) {
            topNode = topNode.next;
        }
        return top;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        } else {
            return topNode.data;
        }
    }

    @Override
    public boolean isEmpty() {
        return topNode == null;
    }

    @Override
    public void clear() {
        topNode = null;
    }

    private class Node {
        private T data;
        private Node next;

        private Node(T data, Node next) {
            this.data = data;
            this.next = next;
        }
    }
}