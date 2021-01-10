package mampf.order;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.money.MonetaryAmount;

import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Quantity;

import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.Item.Domain;

public class MampfCart{
	private Map<Item.Domain, DomainCart> stuff = new TreeMap<>();
	
	public class DomainCart extends Cart{
		
		private LocalDateTime startDate = null;
		private LocalDateTime endDate = null;
		
		public DomainCart() {
			super();
		}
		
		public void setDate(LocalDateTime startDate, LocalDateTime endDate) {
			this.startDate = startDate;
			this.endDate = endDate;
		}
		public void resetDate() {
			this.startDate = null;
			this.endDate = null;
		}
		public LocalDateTime getStartDate() {
			return startDate;
		}
		public LocalDateTime getEndDate() {
			return endDate;
		}
		
		public Map<CartItem, MonetaryAmount> getItems(){
			Map<CartItem, MonetaryAmount> stuff = new HashMap<>();
			Iterator<CartItem> it = get().iterator();
			while(it.hasNext()) {
				CartItem cartitem = it.next();
				Money price;
				if(startDate == null || endDate == null) {
					price = (Money)cartitem.getPrice();
				}else {
					price = (Money)getPriceOfCartItem(cartitem);
				}
				stuff.put(cartitem,price);
			}	
			return stuff;
		}
		
		@Override
		public MonetaryAmount getPrice() {
			if(startDate == null || endDate == null) {
				return super.getPrice();
			}
			Money price = Money.of(0, "EUR"); 
			Iterator<CartItem> it = get().iterator();
			while(it.hasNext()) {	
				price = price.add(getPriceOfCartItem(it.next()));
			}	
			
			return price;
		}
		
		private MonetaryAmount getPriceOfCartItem(CartItem cartItem) {
			if(EventOrder.productHasPrizePerHour.test(cartItem.getProduct())) {
				return
				EventOrder.calcPrizePerHour(startDate, endDate,
				cartItem.getPrice());
			}
			else{
				return cartItem.getPrice();
			}
			
		}
	}
	
	
	MampfCart(){}
	/**
	 * get cart of domain
	 * @param domain
	 * @return Cart or null
	 */
	public DomainCart getDomainCart(Item.Domain domain) {
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
		DomainCart domainCart = getDomainCart(item.getDomain());
		CartItem cartitem = null;
		if(domainCart != null) {
			cartitem = domainCart.addOrUpdateItem(item, itemQuantity);
		}else {
			domainCart = new DomainCart();
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
	public void updateCart(CheckoutForm form) {
		//update all but no Mobile Breakfast
		Map<String, String> allStartDates = form.getAllStartDates();
		Map<String, String> allEndTimes = form.getAllStartDates();
		for(Item.Domain domain: Item.Domain.values()) {
			if(allStartDates.containsKey(domain.name()) && allEndTimes.containsKey(domain.name())) {
				if(CheckoutForm.domainsWithoutForm.contains(domain.name()))continue;
				DomainCart cart = getDomainCart(domain);
				if(cart != null) {
					cart.setDate(form.getStartDateTime(domain), form.getEndDateTime(domain));
				}
			}
		}
	}
	public void updateMBCart(LocalDateTime startDate, LocalDateTime endDate){
		//only update Mobile Breakfast
		DomainCart cart = getDomainCart(Item.Domain.MOBILE_BREAKFAST);
		if(cart != null) {
			cart.setDate(startDate, endDate);
		}
	}
	
	public void resetCartDate() {
		stuff.entrySet().forEach(entry->{
			if(!CheckoutForm.domainsWithoutForm.contains(entry.getKey().name()))
				entry.getValue().resetDate();
		});
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
		for(Map.Entry<Item.Domain, DomainCart> entry : stuff.entrySet()) {
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
	 *
	 * @param domain
	 * @return Map<Item.Domain, Cart>, never null
	 */
	public Map<Item.Domain, DomainCart> getDomainItems(String domain){
		Map<Item.Domain, DomainCart> map = new TreeMap<>();
		for(Map.Entry<Item.Domain, DomainCart> entry : stuff.entrySet()) {
			
			if(entry.getKey().name().equals(domain)){
				map.put(entry.getKey(), entry.getValue());
				return map;
			}
		}
	
		return stuff;
	}
	/**
	 * get total prize of all carts
	 * @param carts
	 * @return Money
	 */
	public Money getTotal(Collection<DomainCart> carts) {
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
	public Map<Item.Domain, DomainCart> getStuff(){
		return stuff;
	}
	
}