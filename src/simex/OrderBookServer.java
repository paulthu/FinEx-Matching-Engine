package simex;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderBookServer {
    private final OrderBook orderBook;
    private final HttpServer server;
    private final ObjectMapper objectMapper;

    public OrderBookServer(OrderBook orderBook, int port) throws IOException {
        this.orderBook = orderBook;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.objectMapper = new ObjectMapper();

        // Create context for getting orderbook data
        server.createContext("/api/orderbook", new OrderBookHandler());
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    class OrderBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Enable CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json");

                // Convert OrderBook data to JSON format
                OrderBookDTO dto = convertToDTO();
                String response = objectMapper.writeValueAsString(dto);

                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        private OrderBookDTO convertToDTO() {
            OrderBookDTO dto = new OrderBookDTO();

            // Convert bids
            List<OrderLevelDTO> bids = new ArrayList<>();
            LinkedList<Limit> bidLimits = orderBook.getLimits()[OrderBook.BID_ASK.BID.getIndex()];
            for (Limit limit : bidLimits) {
                double totalQuantity = 0;
                for (Order order : limit.getLimitQueue()) {
                    totalQuantity += order.getQuantity();
                }
                bids.add(new OrderLevelDTO(limit.getPrice(), totalQuantity));
            }

            // Convert asks
            List<OrderLevelDTO> asks = new ArrayList<>();
            LinkedList<Limit> askLimits = orderBook.getLimits()[OrderBook.BID_ASK.ASK.getIndex()];
            for (Limit limit : askLimits) {
                double totalQuantity = 0;
                for (Order order : limit.getLimitQueue()) {
                    totalQuantity += order.getQuantity();
                }
                asks.add(new OrderLevelDTO(limit.getPrice(), totalQuantity));
            }

            dto.setBids(bids);
            dto.setAsks(asks);
            return dto;
        }
    }

    // DTO classes for JSON conversion
    static class OrderBookDTO {
        private List<OrderLevelDTO> bids;
        private List<OrderLevelDTO> asks;

        public List<OrderLevelDTO> getBids() { return bids; }
        public void setBids(List<OrderLevelDTO> bids) { this.bids = bids; }
        public List<OrderLevelDTO> getAsks() { return asks; }
        public void setAsks(List<OrderLevelDTO> asks) { this.asks = asks; }
    }

    static class OrderLevelDTO {
        private double price;
        private double quantity;

        public OrderLevelDTO(double price, double quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
    }
}
