package mampf.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.DayOfWeek;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;
import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;

import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.catalog.Item.Domain;
import mampf.order.MampfOrderManager.ValidationState;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("mampfCart")
public class OrderController {
	
	
	private UserManagement userManagement;
	private final MampfOrderManager orderManager;
	private final Inventory inventory;
	public static final TemporalAmount delayForEarliestPossibleBookingDate = Duration.ofHours(5);

	public class BreakfastMappedItems extends Item {
		
		private LocalTime breakfastTime;
		private LocalDateTime startDate,endDate; 
		private List<DayOfWeek> weekDays;
		private BreakfastItem beverage,dish;
		private long amount;
		
		public BreakfastMappedItems(User user,
									MobileBreakfastForm form) {
			
			super("Mobile Breakfast \nChoice: " +
	 			  form.getBeverage().getName() +", "+
	 			  form.getDish().getName(),
	 			  BreakfastItem.BREAKFAST_PRICE, 
	 			  Item.Domain.MOBILE_BREAKFAST, 
	 			  Item.Category.FOOD, 
	 			  "Employee Choice of beverage and dish");
			//get weekdays:
			//TODO: save risky .valueOf() function
			weekDays = form.getDays().keySet().stream().
					   filter(k->form.getDays().get(k).booleanValue()). //get all marked weekdays
					   map(k->DayOfWeek.valueOf(k.toUpperCase())). //convert string to weekday
					   collect(Collectors.toList()); 		
			
			//get breakfasttime:
			breakfastTime = form.getTime();
					
			//get start and end Dates:
			Optional<Company> company = userManagement.findCompany(user.getId());
			startDate = LocalDateTime.of(company.get().getBreakfastDate(),LocalTime.of(0, 0));
			endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(),LocalTime.of(0, 0));
			setName(getName()+"\nFrom "+startDate.toLocalDate()+" to "+endDate.toLocalDate()+"\nEach: "+weekDays);
			
			//get items:
			beverage = form.getBeverage();
			dish = form.getDish();
			
			//set amount:
			List<UniqueMampfItem> totalItems = 
					orderManager.getBookedItems(
					MBOrder.getItems(startDate, endDate, 
									 startDate, endDate, 
									 weekDays, breakfastTime, 
									 List.of(beverage.getId(),
											 dish.getId())));
			amount = totalItems.get(0).getAmount().longValue();			
		}
		public LocalDateTime getStartDate() {
			return startDate;
		}
		public LocalDateTime  getEndDate() {
			return endDate;
		}
		public LocalTime getBreakfastTime() {
			return breakfastTime;
		}
		public BreakfastItem getDish() {
			return dish;
		}
		public BreakfastItem getBeverage() {
			return beverage;
		}
		public List<DayOfWeek> getWeekDays(){
			return weekDays;
		}
		public long getAmount() {
			return amount;
		}
	}

	
	public OrderController(MampfOrderManager orderManager, UserManagement userManagement, Inventory inventory) {
		this.orderManager = orderManager;
		this.userManagement = userManagement;
		this.inventory = inventory;
	}

	/* CART */
	@ModelAttribute("mampfCart")
	MampfCart initializeCart() {
		return new MampfCart();
	}
	
	/**
	 * adds item to cart
	 */
	@PostMapping("/cart")
	public String addItem(@RequestParam("pid") Item item,
						  @RequestParam("number") int number,
						  @ModelAttribute("mampfCart") MampfCart mampfCart) {
		//UniqueMampfItem uniqueMampfItem = this.inventory.findByProduct(item).get();
		//if(uniqueMampfItem.getAmount().intValue() < number){
			// TODO: Maybe add Error-Message when amount is not available
		//	return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
		//}
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
		
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		//ERRORS:
		//TODO: MB error: choice invalid
		if(form.getBeverage() == null || form.getDish() == null) {
			return "redirect:/mobile-breakfast";
		}
		//TODO: MB error: not possible (was not booked)
		if(!orderManager.hasBookedMB(userAccount.get())) {
			
			return "redirect:/mobile-breakfast";
		}
		//TODO: MB error: outdated (check if time now is after choiceTimeEnd)
		BreakfastMappedItems mbItem = 
				new BreakfastMappedItems(
						userManagement.findUserByUserAccount(userAccount.get().getId()).get(),
						form);
		 
		mampfCart.addToCart(mbItem,Quantity.of(mbItem.getAmount()));

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
		form.setAdress("over the rainbow");
		buyCart(domain, model,mampfCart, form);
		return "buyCart";
	}

	/**
	 * buy cart(s)
	 */
	@PostMapping("/checkout")
	public String buy(Model model, @Valid @ModelAttribute("form") CheckoutForm form, Errors result,
					  Authentication authentication, @ModelAttribute("mampfCart") MampfCart mampfCart) {

		if(form.getStartDateTime().isBefore(LocalDateTime.now().plus(delayForEarliestPossibleBookingDate))) {
			result.rejectValue("startDate", "CheckoutForm.startDate.NotFuture", "Your date should be in the future!");
		}
		
		//for only one cart implementation works:
		

		Map<Item.Domain, Cart> carts = mampfCart.getDomainItems(form.getDomainChoosen());
		Map<Item.Domain, CheckoutForm> forms = new HashMap<>();
		forms.put(Item.Domain.valueOf(form.getDomainChoosen()), form);
		
		Map<Item.Domain, List<String>> validations = orderManager.validateCarts(carts, forms);
		
		//TODO: advanced error handling
		if(!validations.isEmpty()) {
		result.rejectValue("generalError",
					"CheckoutForm.generalError.NoStuffLeft",
					"There is no free stuff or personal for the selected time left!");
		
		}

		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isEmpty()) {
			result.rejectValue("generalError",
					"CheckoutForm.generalError.NoLogin",
					"There was an error during your login process");
		}


		if (result.hasErrors()) {
			//model.addAttribute("domains", carts);
			//model.addAttribute("form", form);
			//model.addAttribute("total", cart.getTotal(carts.values()));
			buyCart(form.getDomainChoosen(), model,mampfCart, form);
			return "buyCart";
		}

		orderManager.createOrders(carts, forms, form.getPayMethod(),user.get());

		List<Item.Domain> domains = new ArrayList<>();
		for (Item.Domain domain : carts.keySet()) {
			domains.add(domain);
		}
		for (Item.Domain domain : domains) {
			mampfCart.removeCart(domain);
		}
		//TODO: success handling (some fancy stuff)
			
		return "redirect:/userOrders";
	}
	
	private void buyCart(String domain, Model model, MampfCart mampfCart, CheckoutForm form) {
		Map<Domain, Cart> domains = mampfCart.getDomainItems(domain);
		model.addAttribute("domains", domains);
		form.setDomainChoosen(domain);	
		model.addAttribute("total", mampfCart.getTotal(domains.values()));
		model.addAttribute("form", form);
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
	public String editOrder(@PathVariable EventOrder order, Model model) {

		model.addAttribute("order", order);
		model.addAttribute("orderLines", order.getOrderLines());
		model.addAttribute("employees", order.getEmployees());

		return "ordersDetail";
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
