package mampf.cart;

//intern
//import mampf.catalog.Item;

//me
import mampf.cart.Date;
//java
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.hibernate.cache.spi.support.AbstractReadWriteAccess.Item;
import org.salespointframework.catalog.Product;
//sp
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;

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
	
	class Item extends Product{
		//public Item() {}
	}
	public Item createItem() {return new Item();}
	
	private List<Date> dates;
	private Map<Integer, List<CartItem>> events; 
	
	public MampfCart(){
		dates = new ArrayList<>();
		events = new HashMap<>();
		
		//some testing data:
		//create:
		addItem(new Item("test1",Domain.A),Quantity.of(34), Optional.of(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"HERE")));	
		addItem(new Item("test2",Domain.B),Quantity.of(1), null);
		//add:
		addItem(new Item("test2",Domain.A),Quantity.of(1), Optional.of(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"HERE")));
		addItem(new Item("test3",Domain.B),Quantity.of(112), null);
		
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
		
		Domain itemDomain = item.getDomain();
		Date itemDate = null;
		
		Date eventDate = null; //running var
		Product cartProduct = null;
		List<CartItem> eventList = null; //state var
		int eventCartProductIndex = -1; //state var
		
		//check for item:
		if(date.isPresent())itemDate = date.get();
				
		for(Integer index: events.keySet()) {
			eventDate = dates.get(index.intValue());
			
			if(eventDate.getDomain().equals(itemDomain)){

				//(s,e,D)
				if(itemDate != null) {
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
			{CartItem cartitem = addOrUpdateItem(item, q); eventList.add(cartitem);} //necessary??
			
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
	
}
