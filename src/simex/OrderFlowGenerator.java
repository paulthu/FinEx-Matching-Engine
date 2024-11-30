package simex;

import java.util.Random;

public final class OrderFlowGenerator {

    // instance variables
    private final String symbol;
    private final double priceMean;
    private final double priceStdDev;
    private final double quantityMean;
    private final double quantityStdDev;
    private final Random random = new Random();
    private int time;
    private int id;

    // OrderFlow Generator constructor
    public OrderFlowGenerator(String symbol, double priceMean, double priceStdDev, double quantityMean, double quantityStdDev, int time, int id) {
        this.symbol = symbol;
        this.priceMean = priceMean;
        this.priceStdDev = priceStdDev;
        this.quantityMean = quantityMean;
        this.quantityStdDev = quantityStdDev;
        this.time = time;
        this.id = id;
    }

    // access modifiers
    public String getSymbol() {
        return symbol;
    }

    public double getPriceMean() {
        return priceMean;
    }

    public double getPriceStdDev() {
        return priceStdDev;
    }

    public double getQuantityMean() {
        return quantityMean;
    }

    public double getQuantityStdDev() {
        return quantityStdDev;
    }

    public int getTime() {
        return time;
    }

    public int getId() {
        return id;
    }

    public void incrementTime() {
        time += 100;
    }

    public void incrementId() {
        id += 1;
    }

    public Order generateOrder() {
        // randomly determine buy or sell
        double determineBid = Math.random();
        boolean isBid = determineBid > 0.5;


        // generate price using normal distribution
        double pGaussian = random.nextGaussian();
        double price =  Math.round((priceMean + priceStdDev*pGaussian) * 100.0) / 100.0;

        // generate quantity using normal distribution
        double qGaussian = random.nextGaussian();
        double quantity = Math.abs(quantityMean + quantityStdDev*qGaussian);

        time += 100;
        id += 1;

        // generate order object
        return new Order(isBid, symbol, price, quantity, time, id);
    }

    public static void main(String[] args) {
        OrderFlowGenerator ofg = new OrderFlowGenerator(
                "BTC",
                55000.0,
                3.0,
                2.0,
                1, 0, 10000);


        for (int i = 0; i < 100; i++) {
            Order order = ofg.generateOrder();
            System.out.println(order);
        }
    }
}
