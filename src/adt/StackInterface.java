package adt;

/**
 * @author Pujin
 * Source: Adapted from Chapter 1 & 5 of Data Structures and Algorithms syllabus
 */
public interface StackInterface<T> {
    void push(T newEntry);
    T pop();
    T peek();
    boolean isEmpty();
    void clear();
}