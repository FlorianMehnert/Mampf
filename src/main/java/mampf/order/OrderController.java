package mampf.order;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
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

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.order.MampfOrderManager.ValidationState;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("mampfCart")
public class OrderController {

	private MampfCart cart = new MampfCart();
	private final TemporalAmount delayForEarliestPossibleBookingDate;

	public class BreakfastMappedItems extends Item {
		private MobileBreakfastForm form;

		public BreakfastMappedItems(String name, Money price, String description, MobileBreakfastForm form) {
			super(name, price, Item.Domain.MOBILE_BREAKFAST, Item.Category.FOOD, description);
			this.form = form;
		}

		public MobileBreakfastForm getForm() {
			return form;
		}
	}

	private final MampfOrderManager orderManager;

	public OrderController(MampfOrderManager orderManager) {
		this.orderManager = orderManager;
		this.delayForEarliestPossibleBookingDate = Duration.ofHours(5);
	}

	/* CART */
	@ModelAttribute("mampfCart")
	MampfCart initializeCart() {return new MampfCart();}
	
	/**
	 * adds item to cart
	 */
	@PostMapping("/cart")
	public String addItem(@RequestParam("pid") Item item, 
						  @RequestParam("number") int number,
						  @ModelAttribute("mampfCart") MampfCart mampfCart) {
		mampfCart.addToCart(item, Quantity.of(number));
		return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
	}

	/**
	 * view cart
	 */
	@GetMapping("/cart")
	public String basket(Model model,
						 @ModelAttribute("mampfCart") MampfCart mampfCart) {
		
		Map<Item.Domain, Cart> domains = mampfCart.getStuff();
		model.addAttribute("domains", domains);
		model.addAttribute("total", mampfCart.getTotal(domains.values()));
		return "cart";
	}

	/**
	 * clears cart
	 */
	@PostMapping("cart/clear")
	public String clearCart(@ModelAttribute("mampfCart") MampfCart mampfCart) {
		mampfCart.clear();
		return "redirect:/cart";
	}

	/**
	 * handles adding and removing the amount of a cartitem
	 */
	@PostMapping("cart/setNewAmount")
	public String addCartItem(@RequestParam String cartitemId,
							  @RequestParam int newAmount,
							  @ModelAttribute("mampfCart") MampfCart mampfCart) {

		CartItem cartItem = mampfCart.getCartItem(cartitemId);
		if (cartItem != null) {
			mampfCart.updateCart(cartItem, newAmount);
		}
		return "redirect:/cart";
	}

	/**
	 * adds breakfast choice to cart as one cartitem
	 */
	@PostMapping("/cart/add/mobile-breakfast")
	public String orderMobileBreakfast(@LoggedIn Optional<UserAccount> userAccount,
									   @Valid MobileBreakfastForm form,
									   @ModelAttribute("mampfCart") MampfCart mampfCart) {
		if(form.getBeverage() == null || form.getDish() == null) {
			return "redirect:/mobile-breakfast";
		}
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		mampfCart.addToCart(
			 new BreakfastMappedItems("Mobile Breakfast Choice: " +
					 				  form.getBeverage().getName() +", "+
					 				  form.getDish().getName(),
					 				  BreakfastItem.BREAKFAST_PRICE,
					 				  "Employee Choice of beverage and dish",
					 				  form),
			 Quantity.of(1));

		return "redirect:/cart";
	}


	/**
	 * view buying site
	 */
	@GetMapping("/pay/{domain}")
	public String chooseToBuy(Model model,
							  @PathVariable String domain,
							  CheckoutForm form,
							  @ModelAttribute("mampfCart") MampfCart mampfCart) {

		Map<Item.Domain, Cart> domains = mampfCart.getDomainItems(domain);
		model.addAttribute("domains", domains);
		model.addAttribute("domainChoosen", domain);
		model.addAttribute("total", mampfCart.getTotal(domains.values()));
		model.addAttribute("form", form);
		return "buy_cart";
	}

	/**
	 * buy cart(s)
	 */
	@PostMapping("/checkout")
	//TODO: remove domainChoosen and use form instead
	public String buy(Model model, @RequestParam String domainChoosen, @Valid @ModelAttribute("form") CheckoutForm form, Errors result,
					  @LoggedIn UserAccount userAccount, @ModelAttribute("mampfCart") MampfCart mampfCart) {

		if(form.getStartDateTime().isBefore(LocalDateTime.now().plus(delayForEarliestPossibleBookingDate))) {
			result.rejectValue("startDate", "CheckoutForm.startDate.NotFuture", "Your date should be in the future!");
		}

		Map<Item.Domain, Cart> carts = mampfCart.getDomainItems(domainChoosen);
		Map<Item.Domain, List<ValidationState>> validations = orderManager.validateCarts(carts, form);

		if(!validations.isEmpty()) {
			result.rejectValue("generalError",  "CheckoutForm.generalError.NoStuffLeft","There is no free stuff or personal for the selected time left!");
		}

		if (result.hasErrors()) {
			model.addAttribute("domains", carts);
			model.addAttribute("domainChoosen", domainChoosen);
			model.addAttribute("total", cart.getTotal(carts.values()));
			return "buy_cart";
		}

		orderManager.createOrders(carts, form, userAccount);

		List<Item.Domain> domains = new ArrayList<>();
		for (Item.Domain domain : carts.keySet()) {
			domains.add(domain);
		}
		for (Item.Domain domain : domains) {
			mampfCart.removeCart(domain);
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
	public String orders(Model model) {

		List<MampfOrder> orders = orderManager.findAll();
		model.addAttribute("orders", orders);
		return "orders";
	}

	/**
	 * lists orders of a user
	 */

	@GetMapping("/orders/detail/{order}")
	public String editOrder(@PathVariable MampfOrder order, Model model) {

		model.addAttribute("order", order);
		model.addAttribute("orderLines", order.getOrderLines());
		model.addAttribute("employees", order.getEmployees());

		return "orders_detail";
	}

	@GetMapping("/userOrders")
	public String orderUser(Model model, @LoggedIn Optional<UserAccount> userAccount) {
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		List<MampfOrder> orders = orderManager.findByUserAcc(userAccount.get());
		model.addAttribute("orders", orders);
		return "orders";
	}

}
