package mampf.cart;

//intern
import mampf.catalog.Item;

//me
import mampf.cart.Date;
//java
import java.util.Optional;
import java.util.TreeMap;

import javax.money.Monetary;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import javax.money.CurrencyUnit;
import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.Monetary;
import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
//sp
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Metric;
import org.salespointframework.quantity.Quantity;

//test:
/*
import org.salespointframework.catalog.Product;
//testing purpose:
	//----------------
	
	class Item extends Product{
		//public Item() {}
	}
	public Item createItem() {return new Item();}
	
	//----------------
*/
public class MampfCart extends Cart {
	
	private List<Date> dates;
	private Map<Integer, List<CartItem>> events; 
	
	public MampfCart(){
		dates = new ArrayList<>();
		events = new HashMap<>();
		
		//some testing data:
		//create:
		
		Item i1 = new Item("test1",Money.of(12,Monetary.getCurrency(Locale.US)),Item.Domain.EVENTCATERING,Item.Category.FOOD,"asasas");
		Item i2 = new Item("test2",Money.of(1,Monetary.getCurrency(Locale.US)),Item.Domain.PARTYSERVICE,Item.Category.FOOD,"asasas");
		Item i3 = new Item("test3",Money.of(112,Monetary.getCurrency(Locale.US)),Item.Domain.EVENTCATERING,Item.Category.FOOD,"asasas");
		Optional o1 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"HERE"));
		Optional o2 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 3, 2, 5, 0),null,"HERE"));
		Optional o3 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"NOT HERE"));
		
		
		addItem(i1,Quantity.of(12, Metric.UNIT),o1);	
		addItem(i2,Quantity.of(50, Metric.UNIT), null);
		
		//add:
		addItem(i3,Quantity.of(1), o1);
		addItem(i3,Quantity.of(1), o2);
		addItem(i2,Quantity.of(112), null);
		addItem(i3,Quantity.of(1), o3);
		addItem(i2,Quantity.of(1), o3);
		
		
		
		//TODO:(cannot be cleared right now)
		//excpected:
		//E: x 2, P: x 1 (112+50) 
	}
	
	
	public boolean addItem(Item item, Quantity q, Optional<Date> date) {
		
		//TODO: angebot 'n personal' at date x
		//TODO: null check
		//zb
		//event, dinner,basic x4 , no date
		// add to 'undef event'
		
		//cook, ronny, x1, 21.12. 13:00
		// ronny is not booked
		// add to '<date>'
		
		Item.Domain itemDomain = item.getDomain();
		Date itemDate = null;
		
		Date eventDate = null; //running var
		Product cartProduct = null;
		List<CartItem> eventList = null; //state var
		int eventCartProductIndex = -1; //state var
		
		//check for item:
		if(date != null && date.isPresent())itemDate = date.get();
				
		for(Integer index: events.keySet()) {
			eventDate = dates.get(index.intValue());
			
			
			if(itemDomain.equals(eventDate.getDomain())){
				//(s,e,D)
				if(itemDate != null) {
					if(itemDate.getDomain() == null)itemDate.setDomain(itemDomain); //correct date
					
					if(itemDate.equals(eventDate)) {
						eventList = events.get(index);
						for(int listIndex = 0; listIndex < eventList.size(); listIndex++) { 
							cartProduct = eventList.get(listIndex).getProduct();
							if(item.equals(   ((Item)cartProduct ) )   )
								{eventCartProductIndex = listIndex; break;}}}
				}else 
				//(-,-,D)
					if(!eventDate.hasDate())
						eventList = events.get(eventDate);
					
				//skip if clear:
				if(eventCartProductIndex > -1)break;
			}
		}
		
		//just add to cart
		//TODO check stock/personal first
		
		//quantity only:
		if(eventCartProductIndex > -1 && eventList != null)
			{Product eventCartProduct = eventList.get(eventCartProductIndex).getProduct(); CartItem cartitem = addOrUpdateItem(eventCartProduct, q); eventList.set(eventCartProductIndex,cartitem);} //necessary??
			
		//add to list:
		if(eventCartProductIndex == -1 && eventList != null)
			{CartItem cartitem = addOrUpdateItem(item, q);
			eventList.add(cartitem);} //necessary??
			
		//add new event:
		if(eventCartProductIndex == -1 && eventList == null)
			{CartItem cartitem = addOrUpdateItem(item, q); 
			
			if(itemDate == null) dates.add(new Date(itemDomain));
			else{itemDate.setDomain(itemDomain); dates.add(itemDate);}
			
			List<CartItem> newList = new ArrayList<>();newList.add(cartitem);
			events.put(Integer.valueOf(dates.size()-1),newList);
			}
		//everything can be added:
		return true;
		
	}
	public void removeItem(Item item, Quantity q, Date date) {
		//TODO
	}
	
	public Map<Date,List<CartItem>> getEvents(){
		//grouping cartitems to events
		Map<Date,List<CartItem>> res = new TreeMap<>();
		for(Integer event: events.keySet()) {
			res.put(dates.get(event.intValue()),events.get(event));
		}
		return res;
		
	}
	
	
	public void clearAll() {
		
		dates.clear();
		events.clear();
		
		clear();
	}
}
