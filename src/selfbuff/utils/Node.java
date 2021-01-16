// from the code of Bowl of Chowder:
// https://github.com/StoneT2000/Battlecode2020

package selfbuff.utils;

public class Node<T> {
    public Node next;
    public Node prev;
    public T val;
    public Node(T obj) {
        val = obj;
    }
}