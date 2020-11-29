package mampf.cart;


import mampf.catalog.Item;
import mampf.cart.MampfCart;
import mampf.catalog.Item;
import mampf.order.MampfOrder;
import mampf.order.MampfOrderProduct;
import mampf.order.MampfOrderManager;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.salespointframework.core.AbstractEntity;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("cart")
public class CartController {
	
	private final MampfOrderManager oM;
	//private final EmployeeManager eM;
	
	
	public CartController(MampfOrderManager oM/*, EmployeeManager eM*/) {
		//TODO: nullcheck
		//Assert.notNull(orderManagement, "OrderManagement must not be null!");
		this.oM = oM; //this.eM = eM;
	}
	
	
	@ModelAttribute("cart")
	Cart initializeCart() {
		return new MampfCart();
	}
	
	//--------------
	
	//buy all
	/*
	@PostMapping("/checkout")
	String buyAll(@RequestParam(value="dateid", required=false) Date buyDate,@ModelAttribute MampfCart cart, @LoggedIn Optional<UserAccount> userAccount, Model model) {
		return buy(buyDate, cart, userAccount, model);
	}*/
	@PostMapping("/checkoutAll")
	String buyAll(@ModelAttribute MampfCart cart, @LoggedIn Optional<UserAccount> userAccount, Model model) {
		return buy(null, cart, userAccount, model);
	}
	
	/*
	//buy spec item
	@GetMapping("/checkout/{date}")
	String buyIndex(@PathVariable Date date, @ModelAttribute MampfCart cart, @LoggedIn Optional<UserAccount> userAccount, Model model) {
		return buy(date, cart, userAccount, model);
	}*/
	
	//buy in generell:
	String buy(Date buyDate, MampfCart cart, Optional<UserAccount> userAccount, Model model) {
		//TODO: redirect if not logged in
		//TODO: nullcheck
		//model.addAttribute("order", attributeValue)
		
		
		Map<Date, List<CartItem>> events = cart.getEvents();
		boolean hasError = !userAccount.isPresent();
		
		//checking stuff:
		if(buyDate == null)
			for(Date eventDate: events.keySet()) {
				if(!validateEvent(events.get(eventDate))) {
					hasError = true;break;}}
		else if(!validateEvent(events.get(buyDate)))hasError = true;
		
		
		
		//actually buying:
		if(!hasError) {
		
			MampfOrder order = new MampfOrder(userAccount.get(),Cash.CASH);
			//iterate again, but this time add that stuff:
			
			if(buyDate == null)
				for(Date eventDate: events.keySet())
					for(CartItem eventCartItem: events.get(eventDate))  
					order = addCartItemToOrder(order, ((Item)eventCartItem.getProduct()), eventCartItem.getQuantity(),eventDate);
					
			else 
				for(CartItem eventCartItem: events.get(buyDate))
				order = addCartItemToOrder(order, ((Item)eventCartItem.getProduct()), eventCartItem.getQuantity(),buyDate);
			
			
			//order progress:
			
			oM.save(order);
			
			//TODO: complete order if done
			
			
			
			//remove cartevent/clear cart:
			
			
			//view Order & buying:
			model.addAttribute("order", order);
			return "buy_order";
			
		}
		
		//error handling:
		return "redirect:/cart"; //TODO: add some fancy errors
		
	}
	
	private boolean validateEvent(List<CartItem> items) {
		//TODO: nullchck
		//TODO: validating
		//for(CartItem cartitem : items) {

		//}
		return true;
	}
	
	private MampfOrder addCartItemToOrder(MampfOrder order, Item item, Quantity q, Date date) {
		//TODO: nullcjeck
		//TODO: reduce Stock/...
		order.addOrderLine(new MampfOrderProduct(item, date),q);
		return order;
	}

	
	
	
	@PostMapping("/clear")
	String clearCart(@ModelAttribute MampfCart cart){
		cart.clearAll();
		return "redirect:/cart";
	}
	
	@GetMapping("/cart")
	String basket(@ModelAttribute MampfCart cart, Model model) {
		model.addAttribute("events", cart.getEvents());
		return "cart";
	}
	
}
