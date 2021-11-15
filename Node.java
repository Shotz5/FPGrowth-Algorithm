import java.util.ArrayList;

public class Node {
    private int count;
    private final int ITEM_ID;
    private ArrayList<Node> NEXT_NODES;
    private Node PREV_NODE;
    private Node nextItemPointer;

    public Node(int itemid) {
        NEXT_NODES = new ArrayList<>();
        ITEM_ID = itemid;
        count = 1;
    }

    public boolean incrementCount(int value) {
        count += value;
        return true;
    }

    public boolean decrementCount(int value) {
        count -= value;
        return true;
    }

    public boolean setCount(int value) {
        count = value;
        return true;
    }

    public int getCount() {
        return count;
    }

    public boolean setNext(Node nextNode) {
        nextNode.setPrevious(this);
        NEXT_NODES.add(nextNode);
        return true;
    }

    public boolean setPrevious(Node nextNode) {
        PREV_NODE = nextNode;
        return true;
    }

    public ArrayList<Node> getNext() {
        return NEXT_NODES;
    }

    public Node getPrevious() {
        return PREV_NODE;
    }

    public int getId() {
        return ITEM_ID;
    }

    public boolean setNextPointer(Node n) {
        nextItemPointer = n;
        return true;
    }

    public Node getNextPointer() {
        return nextItemPointer;
    }
}
