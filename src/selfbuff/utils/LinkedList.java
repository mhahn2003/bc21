// from the code of Bowl of Chowder:
// https://github.com/StoneT2000/Battlecode2020

package selfbuff.coms.utils;

public class LinkedList<T> {
    public int size = 0;
    public Node head;
    public Node end;
    public LinkedList() {

    }
    public void add(T obj) {
        if (end != null) {
            Node newNode = new Node(obj);
            newNode.prev = end;
            end.next = newNode;
            end = newNode;
        }
        else {
            head = new Node(obj);
            end = head;
        }
        size++;
    }
    public Node dequeue() {
        if (this.size > 0) {
            Node removed = head;
            remove(head);
            this.size--;
            return removed;
        }
        return null;
    }
    public boolean contains(T obj) {
        Node node = head;
        while (node != null) {
            if (node.val.equals(obj)) {
                return true;
            }
            node = node.next;
        }
        return false;
    }
    public void remove(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        else {
            // deal with head
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        else {
            end = node.prev;
        }
        node = null;
        size--;
    }
    public boolean remove(T obj) {
        Node node = head;
        while (node != null) {
            if (node.val.equals(obj)) {
                remove(node);
                return true;
            }
            node = node.next;
        }
        return false;
    }

}