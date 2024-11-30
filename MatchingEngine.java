package simex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MatchingEngine {

    /* OBSERVER DESIGN PATTERN - a way to notify external objects of what is happening - way to decouple code */
    public interface Listener {
        void onNewOrderReceived(Order order);
        void onOrderCancelled(Order order);
        void onOrderExecuted(Order order, double quantity);
    }

    static OrderFlowGenerator ofg = new OrderFlowGenerator("BTC", 55000.0, 4.0, 2.0, 1, 0, 10000);

    // create a writer field
    private static final BufferedWriter fileWriter;

    // Static initializer block
    static {
        try {
            fileWriter = new BufferedWriter(new FileWriter("meOutput.txt", true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Create method to be used by writer objects for file output
    private static final MatchingEngine.Listener writeToFileListener = new MatchingEngine.Listener() {
        // create specific methods to use base method in order to write individual outputs
        @Override
        public void onNewOrderReceived(Order order) {
            writeToFile("R," + order);
        }

        @Override
        public void onOrderCancelled(Order order) {
            writeToFile("C," + order);
        }

        @Override
        public void onOrderExecuted(Order order, double quantity) {
            writeToFile("M," + quantity + "," + order.getPrice());
        }

        // create base method to write output, avoiding creating 3 independent writer objects
        private void writeToFile(String message) {
            try {
                fileWriter.write(message + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

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
    private final List<Listener> listeners = new ArrayList<>();

    // create field for Matching Engine object
    public MatchingEngine() {
        this.limits = new LinkedList[BID_ASK.NB_SIDES];
        this.limits[BID_ASK.BID.getIndex()] = new LinkedList<Limit>();
        this.limits[BID_ASK.ASK.getIndex()] = new LinkedList<Limit>();
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void onNewOrderReceived(Order o) {
        // create and determine sides to treat order with
        BID_ASK side = BID_ASK.BID;
        BID_ASK otherSide = BID_ASK.ASK;
        if (!o.isBid()) {
            side = BID_ASK.ASK;
            otherSide = BID_ASK.BID;
        }

        // create reference lists to treat bid and ask sides
        LinkedList<Limit> targetLimits = this.limits[side.getIndex()];
        LinkedList<Limit> otherSideLimits = this.limits[otherSide.getIndex()];

        // if the order is past the frontier, into its opposite limits prices, execute
        if ((side == BID_ASK.BID &&  otherSideLimits.peek() != null && o.getPrice() > otherSideLimits.peek().getPrice())
                || (side == BID_ASK.ASK &&  otherSideLimits.peek() != null && o.getPrice() < otherSideLimits.peek().getPrice())) {
            // create variable to keep track of executed quantity
            double execQuantity = 0.0;

            // loop while the order is not yet fully executed
            while (execQuantity < o.getQuantity()) {
                // make new limit of the first limit to match order with
                Limit mostAggressiveOtherSideLimit = otherSideLimits.peek();
                // if there are no more limits on the other side, break and continue
                if (mostAggressiveOtherSideLimit == null) {
                    break;
                }
                // if the next limit becomes unsuitable price for execution, create new one
                if ((side == BID_ASK.BID && o.getPrice() < otherSideLimits.peek().getPrice())
                        || (side == BID_ASK.ASK && o.getPrice() > otherSideLimits.peek().getPrice())) {
                    int index = 0;
                    // find position for new limit
                    for (int i = 0; i < targetLimits.size(); i++) {
                        if (o.getPrice() * side.getSign() > targetLimits.get(i).getPrice() * side.getSign()) {
                            index = i;
                            break;
                        }
                        // If we reach the end of the list, the new value should be added at the end
                        if (i == targetLimits.size() - 1) {
                            index = targetLimits.size();
                        }
                    }
                    // Add the new value at the determined index
                    Limit createdLimit = new Limit(o.isBid(), o.getPrice() - execQuantity);
                    targetLimits.add(index, createdLimit);
                    createdLimit.addOrder(new Order(o.isBid(), o.getSymbol(), o.getPrice(), o.getQuantity() - execQuantity, o.getTime(), o.getId()));
                    // signal listeners that a new order/limit has been created
                    for (Listener listener : this.listeners) {
                        listener.onNewOrderReceived(new Order(o.isBid(), o.getSymbol(), o.getPrice(), o.getQuantity() - execQuantity, o.getTime(), o.getId()));
                    }
                    break;
                }
                // create and store new object for first order to match for execution
                Order orderToMatch = mostAggressiveOtherSideLimit.seeOrder();
                // while there is still an order to match within the most aggressive limit
                while (orderToMatch != null) {
                    // determine and store the remaining quantity to be executed
                    double remainingQuantity = o.getQuantity() - execQuantity;
                    // determine what quantity to be matched
                    double matchedQuantity = Math.min(remainingQuantity, orderToMatch.getQuantity());
                    execQuantity += matchedQuantity;
                    // signal listeners of match
                    for (Listener listener : this.listeners) {
                        listener.onOrderExecuted(orderToMatch, matchedQuantity);
                    }

                    // check if partial exec needed, do so if it is
                    if (remainingQuantity < orderToMatch.getQuantity()) {
                        otherSideLimits.peek().seeOrder().setQuantity(otherSideLimits.peek().seeOrder().getQuantity() - remainingQuantity);
                    }

                    // break if matched surpasses remainder
                    if (matchedQuantity >= remainingQuantity) {
                        break;
                    }

                    // remove most aggressive order/limit that has been matched
                    mostAggressiveOtherSideLimit.dequeueOrder();
                    if (mostAggressiveOtherSideLimit.seeOrder() == null) {
                        otherSideLimits.poll();
                        break;
                    }

                    // select new most aggressive order for next iteration
                    orderToMatch = mostAggressiveOtherSideLimit.seeOrder();
                }
            }
            return;
        }

        // set newLimit as true for base case
        boolean newLimit = true;

        // if order has not been executed, signal listeners to act
        for (Listener listener : this.listeners) {
            listener.onNewOrderReceived(o);
        }

        // check if the new Order matches any current limits to see if we must add to queue to make a new limit
        for (Limit lb : targetLimits) {
            if (lb.getPrice() == o.getPrice()) {
                lb.addOrder(o);
                newLimit = false;
                break;
            }
        }

        // if we must make a new limit
        if (newLimit) {
            int index = 0;
            // iterate through linked list of limits to find position for new limit to be placed
            for (int i = 0; i < targetLimits.size(); i++) {
                if (o.getPrice() * side.getSign() > targetLimits.get(i).getPrice() * side.getSign()) {
                    index = i;
                    break;
                }
                // If we reach the end of the list, the new value should be added at the end
                if (i == targetLimits.size() - 1) {
                    index = targetLimits.size();
                }
            }

            // Add the new value at the determined index
            Limit createdLimit = new Limit(o.isBid(), o.getPrice());
            targetLimits.add(index, createdLimit);
            createdLimit.addOrder(o);

            return;
        }
    }

    public static void main(String[] args) {
        MatchingEngine me = new MatchingEngine();

        // Print listener to be used for testing purposes
        Listener printListener = new Listener() {
            @Override
            public void onNewOrderReceived(Order order) {
                System.out.println("LISTENER Received New Order: " + order);
            }
            @Override
            public void onOrderCancelled(Order order) {
                System.out.println("LISTENER Cancelled Order: " + order);
            }
            @Override
            public void onOrderExecuted(Order order, double quantity) {
                System.out.println("LISTENER Matched " + quantity + " @ " + order.getPrice());
            }
        };

        // Add Listeners
        me.addListener(printListener);
        me.addListener(writeToFileListener);

        // RANDOM TEST CASE: Create an order flow generator, use it to fill matching engine
        //OrderFlowGenerator ofg = new OrderFlowGenerator("BTC", 55000.0, 500.0, 2.0, 1, 0, 10000);
        for (int i = 0; i < 10000; i++) {
            me.onNewOrderReceived(ofg.generateOrder());
        }
        System.out.println("There are " + me.limits[BID_ASK.BID.getIndex()].size() + " bid limits");
        System.out.println("There are " + me.limits[BID_ASK.ASK.getIndex()].size() + " ask limits");

        /*// TEST CASES 1
        Order[] orders = new Order[7];
        // ASK ORDER ON BID LIMITS CASE
        orders[0] = new Order(true, "BTC", 50010.0, 10.0, 1);
        orders[1] = new Order(true, "BTC", 50010.0, 5.0, 2);
        orders[2] = new Order(true, "BTC", 50010.0, 10.0, 3);
        orders[3] = new Order(true, "BTC", 50009.0, 3.0, 4);
        orders[4] = new Order(true, "BTC", 50009.0, 5.0, 5);
        orders[5] = new Order(true, "BTC", 50008.0, 10.0, 6);
        orders[6] = new Order(false, "BTC", 50009.0, 30.0, 7);

        orders[0] = new Order(false, "BTC", 50010.0, 10.0, 1);
        orders[1] = new Order(false, "BTC", 50010.0, 5.0, 2);
        orders[2] = new Order(false, "BTC", 50010.0, 10.0, 3);
        orders[3] = new Order(false, "BTC", 50011.0, 3.0, 4);
        orders[4] = new Order(false, "BTC", 50012.0, 5.0, 5);
        orders[5] = new Order(false, "BTC", 50012.0, 10.0, 6);
        orders[6] = new Order(true, "BTC", 50011.0, 30.0, 7);

        for (Order o : orders) {
            me.onNewOrderReceived(o);
        }
        */

        // close the fileWriter at the end of main
        try {
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}