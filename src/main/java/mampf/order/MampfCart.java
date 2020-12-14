package mampf.order;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Quantity;

import mampf.catalog.Item;

public class MampfCart{
	private Map<Item.Domain, Cart> stuff = new TreeMap<>();
	MampfCart(){}
	/**
	 * get cart of domain
	 * @param domain
	 * @return Cart or null
	 */
	public Cart getDomainCart(Item.Domain domain) {
		if(domain == null){
			return null;
		}
		if(stuff.containsKey(domain)) {
			return stuff.get(domain);
		}
		return null;
	}
	/**
	 * adds new Item to MampfCart
	 * @param item
	 * @param itemQuantity
	 * @return CartItem
	 */
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
	/**
	 * sets new amount of cartitem, also removes the item
	 * @param cartItem
	 * @param itemAmount
	 */
	public void updateCart(CartItem cartItem, int itemAmount) {
		
		Cart domainCart = getDomainCart(((Item)cartItem.getProduct()).getDomain());
		if(domainCart == null) {
			return;
		}
		if(itemAmount < 1) {
			removeFromCart(cartItem);
		}else {
			domainCart.addOrUpdateItem(cartItem.getProduct(),
					(long) itemAmount-cartItem.getQuantity().getAmount().intValue());
		}
	
	}
	/**
	 * removes cartitem
	 * @param cartItem
	 */
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
	/**
	 * removes cart
	 * @param domain
	 */
	public void removeCart(Item.Domain domain) {
		if(stuff.containsKey(domain)) {
			stuff.remove(domain);
		}
	}
	/**
	 * get cartitem by cartitemId
	 * @param cartitemId
	 * @return CartItem or null
	 */
	public CartItem getCartItem(String cartitemId) {
		for(Map.Entry<Item.Domain, Cart> entry : stuff.entrySet()) {
			Optional<CartItem> cartitem = stuff.get(entry.getKey()).
										  getItem(cartitemId);
			if(cartitem.isPresent()) {
				return cartitem.get();
			}
		}
		return null;
	}
	/**
	 * get mapped carts to domain
	 * "_": return everything
	 * @param domain
	 * @return Map<Item.Domain, Cart>, never null
	 */
	public Map<Item.Domain, Cart> getDomainItems(String domain){
		Map<Item.Domain, Cart> map = new TreeMap<>();
		if(!domain.equals("_")) {
			for(Item.Domain type: Item.Domain.values()) {
				if(type.name().equals(domain)) {
					map.put(type, getDomainCart(type));
					return map;
				}
			}
		}
		return stuff;
	}
	/**
	 * get total prize of all carts
	 * @param carts
	 * @return Money
	 */
	public Money getTotal(Collection<Cart> carts) {
		Money res = Money.of(0, "EUR");
		if(carts != null) {
			for(Cart cart: carts) {
				res = res.add(cart.getPrice());
			}
		}
		return res;
	}
	/**
	 * clears stock
	 */
	public void clear() {
		stuff.clear();
	}
	public Map<Item.Domain, Cart> getStuff(){
		return stuff;
	}
	
}