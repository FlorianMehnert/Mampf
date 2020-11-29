package mampf.cart;

import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Quantity;

import mampf.catalog.Item;

public class MampfCartObject {
	private CartItem item;
	private Date date;
	
	public MampfCartObject(CartItem item, Date date) {
		this.date = date; this.item = item;
	}
	
	public void update(CartItem cartitem) {
		item = cartitem;
		//item.getQuantity().add(addq); //not working
	}
	
	public Date getDate() {return date;}
	public Item getItem() {return ((Item)item.getProduct());}
	public Quantity getQuantity() {return item.getQuantity();}
	
	public CartItem getCartItem() {return item;}
	
}
