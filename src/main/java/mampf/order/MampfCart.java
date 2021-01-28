package mampf.order;

import java.time.LocalDateTime;
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
import mampf.catalog.Item.Domain;
/**
 * a shopping-cart to store and manage {@link CartItem}. <br/>
 * 
 *
 */
public class MampfCart{
	private Map<Item.Domain, DomainCart> stuff = new TreeMap<>();
	/**
	 * a {@link Cart}, which can also have a start and end-date ({@link LocalDateTime}).</br>
	 * also manages {@link CartItem} which have a prize for each hour.
	 *
	 */
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
		/**
		 * returns all {@link CartItem} mapped to its total prize of this cart.</br> 
		 * when the cart has a start and end Date, the total prizes can vary.
		 * @return a new instance of {@link Map} of {@link CartItem} and {@link MonetaryAmount}
		 */
		public Map<CartItem, MonetaryAmount> getItems(){
			Map<CartItem, MonetaryAmount> items = new HashMap<>();
			Iterator<CartItem> it = get().iterator();
			while(it.hasNext()) {
				CartItem cartitem = it.next();
				Money price;
				if(startDate == null || endDate == null) {
					price = (Money)cartitem.getPrice();
				}else {
					price = (Money)getPriceOfCartItem(cartitem);
				}
				items.put(cartitem,price);
			}	
			return items;
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
		/**
		 * returns updated total prize of a cartItem
		 * @param cartItem a {@link CartItem}
		 * @return a {@link MonetaryAmount} 
		 */
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
	 * get the mapped {@link DomainCart} of domain
	 * @param domain
	 * @return a instance of {@link DomainCart} or {@literal null}
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
	 * adds a new or updated {@link CartItem} of item and added itemQuantity to MampfCart.</br>
	 * puts {@link CartItem} into {@link DomainCart} depending on the item's {@link Item.Domain}.</br>
	 * <li>there can only be one {@link CartItem} in the {@link DomainCart} mapped to {@link Item.Domain.MOBILE_BREAKFAST}</li>
	 * 
	 * @param item a {@link Item}, does not have to be a existing catalog-item
	 * @param itemQuantity a {@link Quantity} of the amount of item 
	 * @return a instance of the updated or new {@link CartItem}
	 */
	public CartItem addToCart(Item item, Quantity itemQuantity) {
		DomainCart domainCart = getDomainCart(item.getDomain());
		CartItem cartitem = null;
		
		if(domainCart != null) {
		    if(item.getDomain().equals(Domain.MOBILE_BREAKFAST)) {
		       domainCart.clear(); 
		    }
			cartitem = domainCart.addOrUpdateItem(item, itemQuantity);
		}else {
			domainCart = new DomainCart();
			cartitem = domainCart.addOrUpdateItem(item, itemQuantity);
			stuff.put(item.getDomain(),domainCart);
		}
		return cartitem;
	}
	/**
	 * handles updating the {@link Quantity} of a {@link CartItem} or removing the {@link CartItem} in a {@link DomainCart}.</br>
	 * when the given itemAmount is less than {@literal 1}, the {@link CartItem} will be removed from the {@link DomainCart}.</br>
	 * - items with domain names of {@link CheckoutForm.domainsWithoutForm} cannot be updated.
	 * @param cartItem  
	 * @param itemAmount new {@link Quantity} amount
	 */
	public void updateCart(CartItem cartItem, int itemAmount) {
		
		Item.Domain domain = ((Item)cartItem.getProduct()).getDomain();
		Cart domainCart = getDomainCart(domain);
		if(domainCart == null || CheckoutForm.domainsWithoutForm.contains(domain.name())) {
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
	 * sets start and end dates ({@link LocalDateTime}) from the form to the fitting {@link DomainCart}.</br>
	 * - ignores dates with domains of {@link CheckoutForm.domainsWithoutForm}
	 * @param form a {@link CheckoutForm} which should have valid Dates
	 */
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
	/**
	 * sets startDate and endDate to the {@link Item.Domain.MOBILE_BREAKFAST} mapped {@link DomainCart}
	 * @param startDate 
	 * @param endDate
	 */
	public void updateMBCart(LocalDateTime startDate, LocalDateTime endDate){
		DomainCart cart = getDomainCart(Item.Domain.MOBILE_BREAKFAST);
		if(cart != null) {
			cart.setDate(startDate, endDate);
		}
	}
	/**
	 * resets every {@link DomainCart} start and end Dates to {@code null} </br>
	 * - does not reset {@link DomainCart} with domains of {@link CheckoutForm.domainsWithoutForm}
	 */
	public void resetCartDate() {
		stuff.entrySet().forEach(entry->{
			if(!CheckoutForm.domainsWithoutForm.contains(entry.getKey().name()))
				entry.getValue().resetDate();
		});
	}
	/**
	 * handles removing {@link CartItem} of a {@link DomainCart}.</br>
	 * also removes the fitting {@link DomainCart} when there are no {@link CartItem} left. 
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
	public void removeCart(Item.Domain domain) {
		if(stuff.containsKey(domain)) {
			stuff.remove(domain);
		}
	}
	/**
	 * get {@link CartItem} by cartItemId
	 * @param cartItemId
	 * @return CartItem a found {@link CartiItem} or {@code null}
	 */
	public CartItem getCartItem(String cartItemId) {
		for(Map.Entry<Item.Domain, DomainCart> entry : stuff.entrySet()) {

			Optional<CartItem> cartitem = stuff.get(entry.getKey()).
										  getItem(cartItemId);
			if(cartitem.isPresent()) {
				return cartitem.get();
			}
		}
		return null;
	}
	/**
	 * creates and returns mapped {@link DomainCart} to the given domain.</br>
	 * will return every {@link Item.Domain} mapped to {@link DomainCart} when domain is {@code null}.
	 * 
	 * @param domain a {@link Item.Domain} which can be {@code null}
	 * @return a new instance of {@link Map} of {@link Item.Domain} and {@link DomainCart}
	 */
	public Map<Item.Domain, DomainCart> getDomainItems(Item.Domain domain){
		Map<Item.Domain, DomainCart> map = new TreeMap<>();
		for(Map.Entry<Item.Domain, DomainCart> entry : stuff.entrySet()) {
			
			if(entry.getKey().equals(domain)){
				map.put(entry.getKey(), entry.getValue());
				return map;
			}
		}
	
		return stuff;
	}
	/**
	 * get total prize of all {@link DomainCart} of {@link MampfCart.getDomainItems(domain)}
	 * @param domain a {@link Item.Domain} can be {@code null}
	 * @return {@link Money} of the total prize of the {@link DomainCart} of {@link MampfCart.getDomainItems(domain)}
	 */
	public Money getTotal(Item.Domain domain) {
		Money res = Money.of(0, "EUR");
		for(DomainCart cart: getDomainItems(domain).values()){
			res = res.add(cart.getPrice());
		}
		return res;
	}
	public boolean isEmpty() {
		return stuff.isEmpty();
	}
	public void clear() {
		stuff.clear();
	}
	public Map<Item.Domain, DomainCart> getStuff(){
		return stuff;
	}
	
}