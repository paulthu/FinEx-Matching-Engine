package simex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

public class OrderBook {

    // create a writer field
    private static final BufferedReader fileReader;

    // Static initializer block
    static {
        try {
            fileReader = new BufferedReader(new FileReader("meOutput.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // create enum to join both matching engine limits (bid/ask) into 1 structure of 2 sides
    enum BID_ASK
    {
        BID(1, 0),
        ASK(-1, 1);

        static final int NB_SIDES = 2;

        private int sign;
        private int index;

        // enum constructor
        BID_ASK(int sign, int index) {
            this.sign = sign;
            this.index = index;
        }

        // create methods for signs and indexes of enum, to be used to find bid or ask sides
        public int getSign() {
            return sign;
        }

        public int getIndex() {
            return index;
        }
    }

    // create listener and limits field
    private final LinkedList<Limit>[] limits;

    // create field for Matching Engine object
    public OrderBook() {
        this.limits = new LinkedList[MatchingEngine.BID_ASK.NB_SIDES];
        this.limits[MatchingEngine.BID_ASK.BID.getIndex()] = new LinkedList<Limit>();
        this.limits[MatchingEngine.BID_ASK.ASK.getIndex()] = new LinkedList<Limit>();
    }

    public void inputManager(String currLine) {

        switch (currLine.substring(0, 1)) {
            case "R" -> onReceivedOrder(currLine);
            case "M" -> onMatchOrder(currLine);
            case "C" -> onCancelOrder(currLine);
        }
    }

    public void onReceivedOrder(String inputLine) {
        // make input into an order
        String[] parts = inputLine.split(",");

        String symbol = parts[1];
        boolean isBid = parts[2].equalsIgnoreCase("Bid");
        double price = Double.parseDouble(parts[3]);
        double quantity = Double.parseDouble(parts[4]);
        int time = Integer.parseInt(parts[5]);
        int id = Integer.parseInt(parts[6]);

        Order inputOrder = new Order(isBid, symbol, price, quantity, time, id);

        // create and determine sides to treat order with
        MatchingEngine.BID_ASK side = MatchingEngine.BID_ASK.BID;
        MatchingEngine.BID_ASK otherSide = MatchingEngine.BID_ASK.ASK;
        if (!inputOrder.isBid()) {
            side = MatchingEngine.BID_ASK.ASK;
            otherSide = MatchingEngine.BID_ASK.BID;
        }

        // create reference lists to treat bid and ask sides
        LinkedList<Limit> targetLimits = this.limits[side.getIndex()];
        LinkedList<Limit> otherSideLimits = this.limits[otherSide.getIndex()];

        // set newLimit as true for base case
        boolean newLimit = true;

        // check if the new Order matches any current limits to see if we must add to queue to make a new limit
        for (Limit lb : targetLimits) {
            if (lb.getPrice() == inputOrder.getPrice()) {
                lb.addOrder(inputOrder);
                newLimit = false;
                break;
            }
        }

        // if we must make a new limit
        if (newLimit) {
            int index = 0;
            // iterate through linked list of limits to find position for new limit to be placed
            for (int i = 0; i < targetLimits.size(); i++) {
                if (inputOrder.getPrice() * side.getSign() > targetLimits.get(i).getPrice() * side.getSign()) {
                    index = i;
                    break;
                }
                // If we reach the end of the list, the new value should be added at the end
                if (i == targetLimits.size() - 1) {
                    index = targetLimits.size();
                }
            }

            // Add the new value at the determined index
            Limit createdLimit = new Limit(inputOrder.isBid(), inputOrder.getPrice());
            targetLimits.add(index, createdLimit);
            createdLimit.addOrder(inputOrder);
        }
    }

    public void onMatchOrder(String inputLine) {
        // split input into needed parts
        String[] parts = inputLine.split(",");

        double quantityToMatch =  Double.parseDouble(parts[1]);
        double priceMatched = Double.parseDouble(parts[2]);


        for (LinkedList<Limit> limitLinkedList : this.limits) {
            Iterator<Limit> iterator = limitLinkedList.iterator();
            while (iterator.hasNext()) {
                Limit currLimit = iterator.next();
                if (currLimit.getPrice() == priceMatched) {
                    while (quantityToMatch > 0 && currLimit.getSize() != 0) {
                        if (quantityToMatch >= currLimit.seeOrder().getQuantity()) {
                            quantityToMatch -= currLimit.seeOrder().getQuantity();
                            currLimit.dequeueOrder();
                        } else {
                            currLimit.seeOrder().setQuantity(currLimit.seeOrder().getQuantity() - quantityToMatch);
                            quantityToMatch = 0.0;
                        }
                    }
                    if (currLimit.getSize() == 0) {
                        iterator.remove();
                    }
                    if (quantityToMatch == 0) {
                        break;
                    }
                }
            }
            if (quantityToMatch == 0) {
                break;
            }
        }
    }

    public void onCancelOrder(String inputLine) {
        // store important values of input
        String[] parts = inputLine.split(",");

        boolean isBid = parts[2].equalsIgnoreCase("Bid");
        double price = Double.parseDouble(parts[3]);
        int id = Integer.parseInt(parts[6]);

        // go to appropriate LinkedList, look through limits for match in price, go through matching limit and find id to cancel
        if (isBid) {
            for (Limit currLimit : limits[BID_ASK.BID.getIndex()]) {
                if (currLimit.getPrice() == price) {
                    for (Order o : currLimit.getLimitQueue()){
                        if (o.getId() == id) {
                            currLimit.cancelOrder(o);
                        }
                    }
                }
            }
        } else {
            for (Limit currLimit : limits[BID_ASK.ASK.getIndex()]) {
                if (currLimit.getPrice() == price) {
                    for (Order o : currLimit.getLimitQueue()){
                        if (o.getId() == id) {
                            currLimit.cancelOrder(o);
                        }
                    }
                }
            }
        }
    }

    public LinkedList<Limit>[] getLimits() {
        return limits;
    }

    public static void main (String[] args) {
        OrderBook orderBook = new OrderBook();

        try (BufferedReader fileReader = new BufferedReader(new FileReader("meOutput.txt"))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                orderBook.inputManager(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("done");

        try {
            OrderBookServer server = new OrderBookServer(orderBook, 8080);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

