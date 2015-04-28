package pkg.trader;

import java.util.ArrayList;
import java.util.Iterator;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.BuyOrder;
import pkg.order.SellOrder;



public class Trader {
	// Name of the trader
	String name;
	// Cash left in the trader's hand
	double cashInHand;
	// Stocks owned by the trader
	ArrayList<Order> position;
	// Orders placed by the trader
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		// Buy stock straight from the bank
		// Need not place the stock in the order list
		// Add it straight to the user's position
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// Adjust cash possessed since the trader spent money to purchase a
		// stock.
		double stockPrice = m.getStockForSymbol(symbol).getPrice();
		double totalPrice = stockPrice * volume;
		if (totalPrice > cashInHand) {
			throw new StockMarketExpection("Cannot place order for stock: " + symbol 
					+ " since there is not enough money. Trader: " + name);
		}
		BuyOrder stockOrder = new BuyOrder(symbol, volume, stockPrice, this);
		position.add(stockOrder);
		cashInHand = cashInHand - totalPrice;
	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Place a new order and add to the orderlist
		// Also enter the order into the orderbook of the market.
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		//
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// A trader cannot place two orders for the same stock, throw an
		// exception if there are multiple orders for the same stock.
		// Also a person cannot place a sell order for a stock that he does not
		// own. Or he cannot sell more stocks than he possedsses. Throw an
		// exception in these cases.
		if(orderType == OrderType.BUY) {
			double totalPrice = price * volume;
			if (totalPrice > cashInHand) {
				throw new StockMarketExpection("Cannot place order for stock: " + symbol 
						+ " since there is not enough money. Trader: " + name);
			}
			Iterator<Order> itr = ordersPlaced.iterator();
			while (itr.hasNext()) {
				if (itr.next().getStockSymbol().equals(symbol)) {
					throw new StockMarketExpection("Cannot place more than one order for the same stock");
				}
			}
			BuyOrder buyOrder = new BuyOrder(symbol, volume, price, this); //stock price??
			ordersPlaced.add(buyOrder);
			m.addOrder(buyOrder);
		}
		else {
			Iterator<Order> itr = position.iterator();
			boolean containsStock = false;
			int ownedSize = 0;
			while (itr.hasNext()) {
				Order currentOrder = itr.next();
				if (currentOrder.getStockSymbol().equals(symbol)) {
					containsStock = true;
					ownedSize = currentOrder.getSize();
					break;
				}
			}
			if (containsStock == false) {
				throw new StockMarketExpection("Cannot sell stock that one does not own");
			}
			if (ownedSize < volume) {
				throw new StockMarketExpection("Cannot sell more stocks than one owns");
			}
			SellOrder sellOrder = new SellOrder(symbol, volume, price, this);
			ordersPlaced.add(sellOrder);
			m.addOrder(sellOrder);
		}
		
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Similar to the other method, except the order is a market order
		double marketPrice = m.getStockForSymbol(symbol).getPrice();
		if (orderType == OrderType.BUY){
			double totalPrice = marketPrice * volume;
			if (totalPrice > cashInHand) {
				throw new StockMarketExpection("Cannot place order for stock: " + symbol 
						+ " since there is not enough money. Trader: " + name);
			}
			Iterator<Order> itr = ordersPlaced.iterator();
			while (itr.hasNext()) {
				if (itr.next().getStockSymbol().equals(symbol)) {
					throw new StockMarketExpection("Cannot place more than one order for the same stock");
				}
			}
			BuyOrder buyOrder = new BuyOrder(symbol, volume, true, this);
			ordersPlaced.add(buyOrder);
			m.addOrder(buyOrder);
		}
		else{
			Iterator<Order> itr = position.iterator();
			boolean containsStock = false;
			int ownedSize = 0;
			while (itr.hasNext()) {
				Order currentOrder = itr.next();
				if (currentOrder.getStockSymbol().equals(symbol)) {
					containsStock = true;
					ownedSize = currentOrder.getSize();
					break;
				}
			}
			if (containsStock == false) {
				throw new StockMarketExpection("Cannot sell stock that one does not own");
			}
			if (ownedSize < volume) {
				throw new StockMarketExpection("Cannot sell more stocks than one owns");
			}
			SellOrder sellOrder = new SellOrder(symbol, volume, true, this);
			ordersPlaced.add(sellOrder);
			m.addOrder(sellOrder);
		}
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection {
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. Note than an order can sell some of the stocks he
		// bought, etc. Or add more stocks of a kind to his position. Handle
		// these situations.

		// Update the trader's orderPlaced, position, and cashInHand members
		// based on the notification.
		double totalPrice = matchPrice * (double) o.getSize();
		if (o.getType() == OrderType.BUY){
			if (totalPrice > cashInHand){
				throw new StockMarketExpection("Cannot buy order for stock: " + o.getStockSymbol() 
						+ " since there is not enough money. Trader: " + name);
			}
			cashInHand = cashInHand - totalPrice;
			ordersPlaced.remove(o);
			position.add(o);
		}
		else{
			Iterator<Order> itr = position.iterator();
			boolean containsStock = false;
			int ownedSize = 0;
			while (itr.hasNext()) {
				Order currentOrder = itr.next();
				if (currentOrder.getStockSymbol().equals(o.getStockSymbol())) {
					containsStock = true;
					ownedSize = currentOrder.getSize();
					if (ownedSize == o.getSize()) {
						itr.remove();
					}
					else{
						currentOrder.setSize(ownedSize - o.getSize());
					}
					break;
				}
			}
			if (containsStock == false) {
				throw new StockMarketExpection("Cannot sell stock that one does not own");
			}
			if (ownedSize < o.getSize()) {
				throw new StockMarketExpection("Cannot sell more stocks than one owns");
			}
			cashInHand = cashInHand + totalPrice;
			ordersPlaced.remove(o);
		}
	}
	
	public double getCashInHand() {
		return cashInHand;
	}
	
	public ArrayList<Order> getPosition() {
		return position;
	}
	
	public ArrayList<Order> getOrdersPlaced() {
		return ordersPlaced;
	}
	
	public String getName(){
		return name;
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
