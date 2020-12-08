package mampf.order;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;

import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.order.MampfOrderManager.ValidationState;

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
		Map<Item.Domain, Cart> domains = cart.getStuff();
		model.addAttribute("domains", domains);
		model.addAttribute("total", cart.getTotal(domains.values()));
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
	
	/**
	 * view buying site
	 */
	@GetMapping("/pay/{domain}")
	String chooseToBuy(Model model, 
					   @PathVariable String domain, DateFormular form) {

		Map<Item.Domain, Cart> domains = cart.getDomainItems(domain);
		model.addAttribute("domains", domains);
		model.addAttribute("domainChoosen", domain);
		model.addAttribute("total", cart.getTotal(domains.values()));
		model.addAttribute("form", form);
		return "buy_cart";
	}	
	
	/**
	 * buy cart(s)
	 */
	@PostMapping("/checkout")
	String buy(@RequestParam String domainChoosen,
			   @Valid DateFormular form,
			   Errors result,
			   @LoggedIn Optional<UserAccount> userAccount,
			   RedirectAttributes redirectAttributes) {
		
		if(userAccount.isEmpty()) {
			return "redirect:/login";
		}
		
		Map<Item.Domain,Cart> carts = cart.getDomainItems(domainChoosen);
		Map<Item.Domain,List<ValidationState>> validations = 
									orderManager.validateCarts(
											carts, form);
		
		if(!validations.isEmpty()) {
			//error handling
			//send errors to specific pay redirect
			return "redirect:/cart";
		}
		
		List<MampfOrder> orders = orderManager.createOrders(carts, form, userAccount.get());
		
		List<Item.Domain> domains = new ArrayList<>();
		for(Item.Domain domain : carts.keySet()) {
			domains.add(domain);
		}
		for(Item.Domain domain : domains) {
			cart.removeCart(domain);
		} 
		
		// success handling
		
		return "redirect:/userOrders";
	}

/* ORDERS */
	
	/**
	 * lists orders ever for adminuser
	 */
	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	String orders(Model model) {

		ArrayList<MampfOrder> orders = orderManager.findAll();
		model.addAttribute("orders", orders);
		return "orders";
	}
	/**
	 * lists orders of a user
	 */

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
		if(userAccount.isEmpty())return "redirect:/login";
		List<MampfOrder> orders = orderManager.findByUserAcc(userAccount.get());
		model.addAttribute("orders", orders);
		return "orders";
	}
	
}
