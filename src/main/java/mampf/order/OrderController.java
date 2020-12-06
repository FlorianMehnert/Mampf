package mampf.order;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.validation.Valid;

import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Metric;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;

import org.springframework.web.bind.annotation.RequestParam;


@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("cart")
public class OrderController {
	
	private MampfCart cart = new MampfCart();
	public class BreakfastMappedItems extends Item{
		private MobileBreakfastForm form;
		public BreakfastMappedItems(String name,
										  Money price,
										  String description,
										  MobileBreakfastForm form) {
			super(name, price, 
				   Item.Domain.MOBILE_BREAKFAST,
				   Item.Category.FOOD,
				   description);
			this.form=form;
		}
		public MobileBreakfastForm getForm() {
			return form;
		}
	}
	
	private final MampfOrderManager orderManager;
	
	public OrderController(MampfOrderManager orderManager) {
		this.orderManager = orderManager;
	}

/* CART */
	
	/*@ModelAttribute("cart")
	MampfCart initializeCart() {
		//mobileBreakfastForm = null;
		MampfCart cart = new MampfCart(1);
		
		Item i1 = new Item("test1",Money.of(2,"EUR"),Item.Domain.EVENTCATERING,Item.Category.FOOD,"d");
		Item i2 = new Item("test2",Money.of(2,"EUR"),Item.Domain.PARTYSERVICE,Item.Category.FOOD,"d");
		Item i3 = new Item("test3",Money.of(2,"EUR"),Item.Domain.PARTYSERVICE,Item.Category.FOOD,"d");
		Item i4 = new Item("test3",Money.of(2,"EUR"),Item.Domain.MOBILE_BREAKFAST,Item.Category.FOOD,"d");
		
		CartItem c1 = cart.addToCart(i1, Quantity.of(10));
		CartItem c2 = cart.addToCart(i2, Quantity.of(10));
		CartItem c3 = cart.addToCart(i3, Quantity.of(1));
		CartItem c4 = cart.addToCart(i4, Quantity.of(1));
		cart.addToCart(i1, Quantity.of(10));
		
		
		cart.updateCart(c1, -2);
		cart.updateCart(c2, -10);
		cart.updateCart(c3, -10);
		//cart.updateCart(c4, -10);
		cart.updateCart(c4, 10); //should not work
		
		//removeFromCart(cart, c4); //should not be able
		//removeFromCart(cart, c1); 
		
		return cart;
		
		//return new TreeMap<Item.Domain, Cart>();
		return new MampfCart();
	}*/
	/**
	 * adds item to cart
	 */
	@PostMapping("/cart")
	String addItem(@RequestParam("pid") Item item,
				   @RequestParam("number") int number
				   /*@ModelAttribute MampfCart cart*/) {
		cart.addToCart(item, Quantity.of(number));
		return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
	}
	/**
	 * view cart
	 */
	@GetMapping("/cart")
	String basket(/*@ModelAttribute MampfCart cart*/
				  Model model) {
		model.addAttribute("n", cart.getSize());
		model.addAttribute("domains", cart.getStuff());
		return "cart";
	}
	/**
	 * clears cart
	 */
	@PostMapping("cart/clear")
	String clearCart() {
		cart.clear();
		return "redirect:/cart";
	}

	/**
	 * 	handles adding and removing the amount of a cartitem
	 */
	@PostMapping("cart/setNewAmount")
	String addCartItem(@RequestParam String cartitemId,
					   @RequestParam int newAmount
					   /*@ModelAttribute MampfCart cart*/) {
		
		//Optional<CartItem> cartitem = cart.getItem(cartitemId);
		CartItem cartItem = cart.getCartItem(cartitemId);
		if(cartItem != null) {
			cart.updateCart(cartItem, newAmount);
		}
		return "redirect:/cart";
	}
	
	/**
	 * adds breakfast choice to cart as one cartitem
	 */
	@PostMapping("/cart/add/mobile-breakfast")
	String orderMobileBreakfast( @LoggedIn Optional<UserAccount> userAccount, @Valid MobileBreakfastForm form){

		if(userAccount.isEmpty()) {
			return "redirect:/login";
		}
		cart.addToCart(new 
				BreakfastMappedItems(
						"Mobile Breakfast Choice: "+form.getBeverage().getName()+", "+form.getDish().getName(),
						BreakfastItem.BREAKFAST_PRICE,
						"Employee Choice of beverage and dish",
						form),
				Quantity.of(1));
		
		return "redirect:/cart";
	}

	
	/**
	 * removes cartitem from cart
	 */
	@PostMapping("cart/remove")
	String removeCartItem(@RequestParam String cartitemId
						  /*@ModelAttribute MampfCart cart*/) {
		CartItem cartitem = cart.getCartItem(cartitemId);
		if(cartitem != null) {
			cart.removeFromCart(cartitem);
		}
		return "redirect:/cart";
	}
	
	//choose to buy:
	/*@PostMapping("/pay")
	String chooseToBuy(Model model, @RequestParam String domain, DateFormular form) {

		
		model.addAttribute("events", getDomainItems(cart,domain));
		model.addAttribute("domainChoosen", domain);
		model.addAttribute("form", form);
		return "buy_cart";
	}	*/
	
	//TODO: replace domain with optionals
	/*Map<Item.Domain,List<CartItem>> getDomainItems(Cart cart,String domain){
		Map<Item.Domain,List<CartItem>> events = new HashMap<>();
		boolean checkForDomain = (!domain.equals("none")); 
		for(CartItem cartitem: cart) {
			Item item = (Item)cartitem.getProduct();
			Item.Domain itemDomain = item.getDomain();
			
			//skip item if not of requested domain
			if(checkForDomain)
				if(!itemDomain.equals(Item.Domain.valueOf(domain)))continue;
			
			if(events.containsKey(itemDomain))
			//add to list
				{events.get(itemDomain).add(cartitem);}
			else
			//just create new list
				{List<CartItem> event=new ArrayList<>();event.add(cartitem);events.put(itemDomain, event);}
		}
		return events;
	}*/
	
	
	
	/*@PostMapping("/checkout")
	String buy(@RequestParam String domainChoosen, @ModelAttribute Cart cart, @Valid DateFormular form, Errors result, @LoggedIn Optional<UserAccount> userAccount, RedirectAttributes redirectAttributes) {
		
		if(userAccount.isEmpty()) 
			{return "redirect:/login";}
		
		
		Map<Item.Domain, List<CartItem>> orders = getDomainItems(cart, domainChoosen);
		//MobileBreakfastForm mbForm;
		//List<MampfOrder> createdOrders = new ArrayList<>();
		for(Item.Domain domain : orders.keySet()) 
			
			//create new cart:
			{Cart orderCart = new Cart();
			for(CartItem cartitem: orders.get(domain)) 
				{orderCart.addOrUpdateItem(cartitem.getProduct(), cartitem.getQuantity());}
			
			//create order:
			MampfOrder order = orderManager.createOrder(orderCart,
														form, 
														userAccount.get());
			
			//remove cartitem and save the created orders:
			if(order != null)
				//remove items from main cart:
				{for(CartItem cartitem: orderCart) 
					{cart.removeItem(cartitem.getProduct().getId().getIdentifier());} 
				}	
			}
		
		//TODO: mark new created order
		return "redirect:/userOrders";
		/*if(order != null) {
			redirectAttributes.addAttribute("id", order.getId().getIdentifier());
			return "redirect:/orders/detail/{id}";
		}
		//order fehler:
		//TODO
		return "redirect:/cart";
		
	}*/

/* CART FUNCTIONS */
	
	
	
	
	
	
/* ORDERS */
	
	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	String orders(Model model) {

		ArrayList<MampfOrder> orders = orderManager.findAll();
		model.addAttribute("orders", orders);
		return "orders";
	}

	@GetMapping("/orders/detail/{order}")
	//@PreAuthorize("hasRole('BOSS')")
	String editOrder(@PathVariable MampfOrder order, Model model) {
		
		
		model.addAttribute("order", order);
		model.addAttribute("orderLines", order.getOrderLines());
		model.addAttribute("employees", order.getEmployees());
		
		return "orders_detail";
	}
	
	@GetMapping("/userOrders")
	String orderUser(Model model, @LoggedIn Optional<UserAccount> userAccount) {
		if(userAccount.isEmpty())return "redirect:/";
		List<MampfOrder> orders = orderManager.findByUserAcc(userAccount.get());
		model.addAttribute("orders", orders);
		return "orders";
	}
	
	
}
