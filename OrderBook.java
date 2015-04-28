package pkg.order;

import pkg.exception.StockMarketExpection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import pkg.market.api.PriceSetter;

import pkg.market.Market;

public class OrderBook {
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order) {
		// Populate the buyOrders and sellOrders data structures, whichever
		// appropriate
		String symbol = order.getStockSymbol();
		if(order.getType() == OrderType.BUY){
			ArrayList<Order> orderList = buyOrders.get(symbol);
			if (orderList == null){
				orderList = new ArrayList<Order>();
			}
			orderList.add(order);
			buyOrders.put(symbol, orderList);
		}
		else{
			ArrayList<Order> orderList = sellOrders.get(symbol);
			if (orderList == null){
				orderList = new ArrayList<Order>();
			}
			orderList.add(order);
			sellOrders.put(symbol, orderList);
		}
	}

	public void trade() {
		// Complete the trading.
		// 1. Follow and create the orderbook data representation (see spec)
		// 2. Find the matching price
		// 3. Update the stocks price in the market using the PriceSetter.
		// Note that PriceSetter follows the Observer pattern. Use the pattern.
		// 4. Remove the traded orders from the orderbook
		// 5. Delegate to trader that the trade has been made, so that the
		// trader's orders can be placed to his possession (a trader's position
		// is the stocks he owns)
		// (Add other methods as necessary)
		
		// Step 1
		HashMap<Double, Integer> buyMap = new HashMap<Double, Integer>();
		HashMap<Double, Integer> sellMap = new HashMap<Double, Integer>();
		Set<String> buySet = new TreeSet<String>();
		buySet = buyOrders.keySet();
		Set<String> sellSet = new TreeSet<String>();
		sellSet = sellOrders.keySet();
		Iterator<String> buyItr = buySet.iterator();
		while (buyItr.hasNext()) {
			String stockSymbol = buyItr.next();
			if (!sellSet.contains(stockSymbol)) {
				continue;
			}
			ArrayList<Order> buyList = new ArrayList<Order>();
			ArrayList<Order> sellList = new ArrayList<Order>();
			buyList = buyOrders.get(stockSymbol);
			sellList = sellOrders.get(stockSymbol);
			
			
			for (int i = 0; i < buyList.size(); i++) {
				Integer sizeReturned = buyMap.get(buyList.get(i).getPrice());
				if (sizeReturned == null) {
					buyMap.put(buyList.get(i).getPrice(), buyList.get(i).getSize());
				}
				else {
					buyMap.put(buyList.get(i).getPrice(), buyList.get(i).getSize() + sizeReturned);
				}
				
			}
			
			
			for (int i = 0; i < sellList.size(); i++) {
				Integer sizeReturned = sellMap.get(sellList.get(i).getPrice());
				if (sizeReturned == null) {
					sellMap.put(sellList.get(i).getPrice(), sellList.get(i).getSize());
				}
				else {
					sellMap.put(sellList.get(i).getPrice(), sellList.get(i).getSize() + sizeReturned);
				}
				
			}
			
			
			// Step 2
			double matchingPrice = findMatchingPrice(buyMap, sellMap);
			
			// Step 3
			PriceSetter priceSetter = new PriceSetter();
			priceSetter.registerObserver(m.getMarketHistory());
			m.getMarketHistory().setSubject(priceSetter);
			priceSetter.setNewPrice(m, stockSymbol, matchingPrice);
			
			// Step 4 & 5
			ArrayList<Order> newBuyOrders = buyOrders.get(stockSymbol);
			Iterator<Order> newBuyOrdersItr = newBuyOrders.iterator();
			while (newBuyOrdersItr.hasNext()) {
				Order nextOrder = newBuyOrdersItr.next();
				try {
					if (nextOrder.isMarketOrder()) {
						nextOrder.getTrader().tradePerformed(nextOrder, matchingPrice); //throws expections
						newBuyOrdersItr.remove();
					}
					else if (nextOrder.getPrice() >= matchingPrice) {
						nextOrder.getTrader().tradePerformed(nextOrder, matchingPrice); //throws expections
						newBuyOrdersItr.remove();
					}
				}
				catch (StockMarketExpection e) {
					//Handle expection
				}
			}
			buyOrders.put(stockSymbol, newBuyOrders);
			
			ArrayList<Order> newSellOrders = sellOrders.get(stockSymbol);
			Iterator<Order> newSellOrdersItr = newSellOrders.iterator();
			while (newSellOrdersItr.hasNext()) {
				Order nextOrder = newSellOrdersItr.next();
				try {
					if (nextOrder.isMarketOrder()) {
						nextOrder.getTrader().tradePerformed(nextOrder, matchingPrice); //throws expections
						newSellOrdersItr.remove();
					}
					else if (nextOrder.getPrice() <= matchingPrice) {
						nextOrder.getTrader().tradePerformed(nextOrder, matchingPrice); //throws expections
						newSellOrdersItr.remove();
					}
				}
				catch (StockMarketExpection e) {
					//Handle expection
				}
			}
			sellOrders.put(stockSymbol, newSellOrders);
		}
		
	}
	
	public double findMatchingPrice(HashMap<Double, Integer> buyMap, HashMap<Double, Integer> sellMap) {
		Set<Double> buySet = new TreeSet<Double>();
		buySet = buyMap.keySet();
		ArrayList<Double> buyArray = new ArrayList<Double>(buySet);
		Collections.sort(buyArray);
		Set<Double> sellSet = new TreeSet<Double>();
		sellSet = sellMap.keySet();
		ArrayList<Double> sellArray = new ArrayList<Double>(sellSet);
		Collections.sort(sellArray);
		int buyTotal = 0;
		int sellTotal = 0;
		HashMap<Double, Integer> totalBuyMap = new HashMap<Double, Integer>();
		HashMap<Double, Integer> totalSellMap = new HashMap<Double, Integer>();
		
		
		int i = buyArray.size() - 1;
		int endVariable = -1; //for safety
		if (buyArray.get(0) == 0.0) {
			buyTotal += buyMap.get(buyArray.get(0));
			totalBuyMap.put(buyArray.get(0), buyTotal);
			endVariable = 0;
		}
		
		while (i > endVariable) {
			buyTotal += buyMap.get(buyArray.get(i));
			totalBuyMap.put(buyArray.get(i), buyTotal);
			i--;
		}
		for (int j = 0; j < sellArray.size(); j++) {
			sellTotal += sellMap.get(sellArray.get(j));
			totalSellMap.put(sellArray.get(j), sellTotal);
		}
		Set<Double> fullSetOfKeys = new TreeSet<Double>(buySet);
		fullSetOfKeys.addAll(sellSet);
		ArrayList<Double> fullListOfKeys = new ArrayList<Double>(fullSetOfKeys);
		
		
		double matchingPrice = 0.0;
		int quantity = 0;
		int maxAmountToBeTraded = 0;
		for (int k = 0; k < fullListOfKeys.size(); k++) {
			if (totalBuyMap.get(fullListOfKeys.get(k)) == null) {
				continue;
			}
			if (totalSellMap.get(fullListOfKeys.get(k)) == null) {
				continue;
			}
			quantity = Math.min(totalBuyMap.get(fullListOfKeys.get(k)), totalSellMap.get(fullListOfKeys.get(k)));
			//Get min of totalBuyMap.get(fullListOfKeys.get(k)) and totalSellMap.get(fullListOfKeys.get(k))
			//Compare this to the maximum value matchingPrice
			//If it is higher, then set the matching price to that new value's corresponding price
			//Also set a variable to the minimum of the two to keep up with it
			//Remove this check for if quantity is equal to 0
			if (quantity > maxAmountToBeTraded) {
				maxAmountToBeTraded = quantity;
				matchingPrice = fullListOfKeys.get(k);
			}
//			if (quantity == 0) {
//				matchingPrice = fullListOfKeys.get(k);
//				break;
//			}
			//matchingPrice = fullListOfKeys.get(k);
		}
		return matchingPrice;
	}
	
	public HashMap<String, ArrayList<Order>> getBuyOrders() {
		return buyOrders;
	}
	
	public HashMap<String, ArrayList<Order>> getSellOrders() {
		return sellOrders;
	}
}
