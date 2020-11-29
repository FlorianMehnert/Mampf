package mampf.order;

import mampf.cart.Date;
import mampf.catalog.Item;

import org.salespointframework.catalog.Product;

public class MampfOrderProduct extends Product {

	private final Item item;
	private final Date date;
	
	public MampfOrderProduct(Item item, Date date){
		this.item = item;this.date = date;
	}
	public Item getItem() {return item;}
	public Date getDate() {return date;}
}