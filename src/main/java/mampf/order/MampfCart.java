package mampf.order;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Quantity;

import mampf.catalog.Item;

public class MampfCart{
	private Map<Item.Domain, Cart> stuff = new TreeMap<>();;
	//@SuppressWarnings("unused")
	MampfCart(){}
	
	public Cart getDomainCart(Item.Domain domain) {
		if(domain == null){
			return null;
		}
		if(stuff.containsKey(domain)) {
			return stuff.get(domain);
		}
		return null;
	}
	
	public CartItem addToCart(Item item, Quantity itemQuantity) {
		Cart domainCart = getDomainCart(item.getDomain());
		CartItem cartitem = null;
		if(domainCart != null) {
			cartitem = domainCart.addOrUpdateItem(item, itemQuantity);
		}else {
			domainCart = new Cart();
			cartitem = domainCart.addOrUpdateItem(item, itemQuantity);
			stuff.put(item.getDomain(),domainCart);
		}
		return cartitem;
	}
	
	public void updateCart(CartItem cartItem, int itemAmount) {
		
		Cart domainCart = getDomainCart(((Item)cartItem.getProduct()).getDomain());
		if(domainCart == null) {
			return;
		}
		//int newAmount = cartItem.getQuantity().getAmount().intValue()+itemAmount;
		if(itemAmount < 1) {
			removeFromCart(cartItem);
		}else {
			domainCart.addOrUpdateItem((Item)cartItem.getProduct(), itemAmount-cartItem.getQuantity().getAmount().intValue());
		}
	
	}
	
	public void removeFromCart(CartItem cartItem) {
		Cart cartDomain = getDomainCart(((Item)cartItem.getProduct()).getDomain());
		if(cartDomain != null) {
			cartDomain.removeItem(cartItem.getId());
			//delete domain too:
			if(cartDomain.isEmpty()) {
				stuff.remove(((Item)cartItem.getProduct()).getDomain());
			}
		}
	}
	
	public CartItem getCartItem(String cartitemId) {
		for(Item.Domain itemDomain: stuff.keySet()) {
			Optional<CartItem> cartitem = stuff.get(itemDomain).getItem(cartitemId);
			if(cartitem.isPresent()) {
				return cartitem.get();
			}
		}
		return null;
	}
	public void clear() {
		stuff.clear();
	}
	
	public int getSize() {
		return stuff.size();
	}
	public Map<Item.Domain, Cart> getStuff(){
		return stuff;
	}
	
}