package simex;
import java.util.Queue;
import java.util.LinkedList;

public class Limit {
    private boolean isBid;
    private double price;
    private Queue<Order> ordersQueue;

    public Limit(boolean isBid, double price) {
        this.isBid = isBid;
        this.price = price;
        this.ordersQueue = new LinkedList<Order>();
    }

    public boolean isBid() {
        return isBid;
    }

    public double getPrice() {
        return price;
    }

    public Queue<Order> getLimitQueue() {
        return ordersQueue;
    }

    public void addOrder(Order o) {
        ordersQueue.add(o);
    }

    public Order seeOrder() {
        return ordersQueue.peek();
    }

    public Order dequeueOrder() {
        return ordersQueue.poll();
    }

    public void cancelOrder(Order o) {
        if (ordersQueue.contains(o)){
                ordersQueue.remove(o);
        } else {
            return;
        }
    }

    public int getSize() { return ordersQueue.size(); }

}
