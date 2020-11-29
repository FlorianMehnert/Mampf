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


public class MampfCart extends Cart {
	
	private List<MampfCartObject> objects;
	//private Map<Integer, List<CartItem>> events; 
	
	public MampfCart(){
		objects = new ArrayList<>();
		//events = new HashMap<>();
		
		
		//some testing data:
		//create:
		
		Item i1 = new Item("test1",Money.of(12,Monetary.getCurrency(Locale.US)),Item.Domain.EVENTCATERING,Item.Category.FOOD,"asasas");
		Item i2 = new Item("test2",Money.of(1,Monetary.getCurrency(Locale.US)),Item.Domain.PARTYSERVICE,Item.Category.FOOD,"asasas");
		Item i3 = new Item("test3",Money.of(112,Monetary.getCurrency(Locale.US)),Item.Domain.EVENTCATERING,Item.Category.FOOD,"asasas");
		Optional o1 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"HERE"));
		Optional o2 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 3, 2, 5, 0),null,"HERE"));
		Optional o3 = Optional.ofNullable(new Date(LocalDateTime.of(1999, 2, 2, 5, 0),null,"NOT HERE"));
		
		
		
		addItem(i1,Quantity.of(12, Metric.UNIT),o1);	
		addItem(i1,Quantity.of(12, Metric.UNIT),o1);	
		
		addItem(i2,Quantity.of(12, Metric.UNIT),null);	
		
		//removeItem(i1,new Date(i1.getDomain()));
		//removeItem(i2,new Date(i2.getDomain()));
		
		
		//addItem(i2,Quantity.of(12, Metric.UNIT),o1);	
		
		//addItem(i1,Quantity.of(12, Metric.UNIT), null);
		//addItem(i2,Quantity.of(500, Metric.UNIT), null);
		
		//addItem(i3,Quantity.of(500, Metric.UNIT), o2);
		//addItem(i2,Quantity.of(500, Metric.UNIT), o2);
		
		//addItem(i3,Quantity.of(1), o1);
		//addItem(i3,Quantity.of(1), o2);
		//addItem(i2,Quantity.of(112), null);
		//addItem(i3,Quantity.of(1), o3);
		//addItem(i2,Quantity.of(1), o3);
		
		
		
		//TODO:(cannot be cleared right now)
		
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
		if(date != null && date.isPresent()) itemDate = date.get();
		
		if(itemDate != null) itemDate.setDomain(itemDomain);
		
		
		for(MampfCartObject object: objects) {
			Item objectItem = object.getItem();
			Date objectDate = object.getDate();
			
			
			//compare Item
			if(objectItem.equals(item)) {
				
				//compare Date:
				if(itemDate == null) {
					if(itemDomain.equals(objectDate.getDomain()) && objectDate.hasNoDate()) { 
						clear(); //TODO: find better solution
						addOrUpdateItem(objectItem, object.getQuantity());
						object.update(addOrUpdateItem(item,q));
						return true;
					}
				}else { if(itemDate.equals(objectDate)) {
						clear();
						addOrUpdateItem(objectItem, object.getQuantity());
						object.update(addOrUpdateItem(item,q));
						return true;}
				}
			}

		}
		
		//no matches found:
		clear(); //TODO: find better solution
		CartItem newCartItem = addOrUpdateItem(((Product)item),q);
		if(itemDate == null) {
			objects.add(new MampfCartObject(newCartItem, new Date(itemDomain)));
		}else {
			objects.add(new MampfCartObject(newCartItem, itemDate));
		}
		
		//everything can be added:
		return true;
		
	}
	public void removeItem(Item item, Date date) {
		//TODO: nullcheck
		MampfCartObject toDelete = null;
		Item objectItem;
		Date objectDate;
		
		for(MampfCartObject object : objects) {
			objectItem = object.getItem();
			objectDate = object.getDate();
			
			if(item.equals(objectItem) && date.equals(objectDate)) {
				toDelete = object;break;}
		}
		if(toDelete != null) {
			objects.remove(toDelete);
		}
	}
	
	
	public Map<Date,List<CartItem>> getEvents(){
		//grouping cartitems to events
		//TODO
		Map<Date,List<CartItem>> res = new TreeMap<>();
		List<CartItem> list;
		Date key = null;
		for(MampfCartObject object: objects) {
			Item objectItem = object.getItem();
			Date objectDate = object.getDate();
			
			if(res.containsKey(objectDate)) { 
				list = res.get(objectDate);
			}else {
				list = new ArrayList<>();
			}
			list.add(object.getCartItem());
			res.put(objectDate, list);
		}
		
		return res;
		
		
	}
	
	
	public void clearAll() {
		
		//dates.clear();
		//events.clear();
		objects.clear();
		clear();
	}
}
