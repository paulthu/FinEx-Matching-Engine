package simex;

public final class Order {
    private final boolean isBid;
    private final String symbol;
    private final double price;
    private double quantity;
    private final int time;
    private final int id;

    public Order(boolean isBid, String symbol, double price, double quantity, int time, int id) {
        this.isBid = isBid;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.time = time;
        this.id = id;
    }

    public boolean isBid() {
        return isBid;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public int getId() {return id; }

    public int getTime() {
        return time;
    }

    public void setQuantity(double q) {
        quantity = q;
    }

    public String toString() {
        return (symbol + "," + (isBid? "BID":"ASK") + "," + price + "," + quantity + "," + time + "," + id);
    }
}
