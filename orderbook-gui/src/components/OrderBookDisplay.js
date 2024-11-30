import React, { useEffect, useState } from 'react';
import './OrderBookDisplay.css';

const OrderBookDisplay = () => {
    const [orderbook, setOrderbook] = useState({
        bids: [],
        asks: []
    });

    useEffect(() => {
        const fetchOrderBook = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/orderbook');
                const data = await response.json();
                setOrderbook(data);
            } catch (error) {
                console.error('Error fetching orderbook:', error);
            }
        };

        fetchOrderBook();
        const interval = setInterval(fetchOrderBook, 1000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="orderbook-container">
            <div className="orderbook-header">
                <h1>OrderBook Display</h1>
            </div>

            <div className="orderbook-grid">
                {/* Bids Side */}
                <div className="side-container">
                    <h3 className="bid-header">Bids</h3>
                    <div className="book-table">
                        <div className="table-header">
                            <div>Price</div>
                            <div>Quantity</div>
                        </div>
                        {orderbook.bids.map((bid, index) => (
                            <div key={index} className="table-row bid-row">
                                <div className="bid-price">{bid.price.toFixed(2)}</div>
                                <div>{bid.quantity.toFixed(1)}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Asks Side */}
                <div className="side-container">
                    <h3 className="ask-header">Asks</h3>
                    <div className="book-table">
                        <div className="table-header">
                            <div>Price</div>
                            <div>Quantity</div>
                        </div>
                        {orderbook.asks.map((ask, index) => (
                            <div key={index} className="table-row ask-row">
                                <div className="ask-price">{ask.price.toFixed(2)}</div>
                                <div>{ask.quantity.toFixed(1)}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Market Statistics */}
                <div className="stats-container">
                    <div className="stats-box">
                        <h3>Market Statistics</h3>
                        <div className="stats-grid">
                            <div className="stat-item">
                                <p className="stat-label">Best Bid</p>
                                <p className="bid-price">{orderbook.bids[0]?.price.toFixed(2) || 'N/A'}</p>
                            </div>
                            <div className="stat-item">
                                <p className="stat-label">Best Ask</p>
                                <p className="ask-price">{orderbook.asks[0]?.price.toFixed(2) || 'N/A'}</p>
                            </div>
                            <div className="stat-item">
                                <p className="stat-label">Spread</p>
                                <p>{(orderbook.asks[0]?.price - orderbook.bids[0]?.price).toFixed(2) || 'N/A'}</p>
                            </div>
                            <div className="stat-item">
                                <p className="stat-label">Mid Price</p>
                                <p>{((orderbook.asks[0]?.price + orderbook.bids[0]?.price) / 2).toFixed(2) || 'N/A'}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default OrderBookDisplay;