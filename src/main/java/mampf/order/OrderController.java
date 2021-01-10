package mampf.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.DayOfWeek;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

import javax.money.MonetaryAmount;
import javax.persistence.criteria.Order;
import javax.validation.Valid;

import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfCart.DomainCart;
import mampf.user.Company;
import mampf.revenue.Gain;
import mampf.revenue.Revenue;

import mampf.user.User;
import mampf.user.UserManagement;
import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.OrderIdentifier;

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

import org.springframework.web.bind.annotation.RequestParam;


@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("mampfCart")
public class OrderController {
	
	
	private UserManagement userManagement;
	private final MampfOrderManager orderManager;
	private final Inventory inventory;
	public final Revenue revenue;
	public static final TemporalAmount delayForEarliestPossibleBookingDate = Duration.ofHours(5);


	public class BreakfastMappedItems extends Item {
		
		private final LocalTime breakfastTime;
		private final List<DayOfWeek> weekDays;
		private final String adress;
		private final BreakfastItem beverage,dish;
		private final long amount;
		
		public BreakfastMappedItems(User user, LocalDateTime startDate, LocalDateTime endDate,
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
			setName(getName()+"\nFrom "+startDate.toLocalDate()+" to "+endDate.toLocalDate()+"\nEach: "+weekDays);
			Optional<User> boss = userManagement.findUserById(company.get().getBossId());
			if(boss.isPresent()) {
				adress=boss.get().getAddress();
			}else {
				adress="err";
			}
			
			//get items:
			beverage = form.getBeverage();
			dish = form.getDish();
			
			//set amount:
			List<UniqueMampfItem> totalItems = 
					orderManager.getBookedItems(
					MBOrder.getItems(startDate, endDate, 
									 startDate, endDate, 
									 weekDays.stream().collect(Collectors.toSet()), breakfastTime, 
									 List.of(beverage.getId(),
											 dish.getId())));
			if(totalItems.isEmpty()) {
				amount=0;
			}else { 
				amount = totalItems.get(0).getAmount().longValue();			
			}	
			
		}
		public LocalTime getBreakfastTime() {
			return breakfastTime;
		}
		public BreakfastItem getDish() {
			return dish;
		}
		public String getAdress() {
			return adress;
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

	public OrderController(MampfOrderManager orderManager, UserManagement userManagement, Inventory inventory, Revenue revenue) {
		this.orderManager = orderManager;
		this.userManagement = userManagement;
		this.inventory = inventory;
		this.revenue = revenue;
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
		
		//TODO: invalid amount check
		mampfCart.addToCart(item, Quantity.of(number));
		return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
	}

	/**
	 * view cart
	 */
	@GetMapping("/cart")
	public String basket(Model model,
						 @ModelAttribute("mampfCart") MampfCart mampfCart) {
		
		mampfCart.resetCartDate();
		Map<Item.Domain, DomainCart> domains = mampfCart.getStuff();
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
		
		String redirect = "redirect:/mobile-breakfast";
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		//ERRORS:
		if(form.getBeverage() == null || form.getDish() == null) {
			//TODO: MB error: choice invalid
			
			return redirect;
		}
		if(!orderManager.hasBookedMB(userAccount.get())) {
			//TODO: MB error: not possible (was not booked)
			
			return redirect;
		}
		//TODO: MB error: mb is already booked
		//TODO: MB error: outdated (duplicate code from BreakfastmappedItems constructor...)(check if time now is after choiceTimeEnd)
		
		
		User user = userManagement.findUserByUserAccount(userAccount.get().getId()).get();
		Optional<Company> company = userManagement.findCompany(user.getId());
		LocalDateTime startDate = LocalDateTime.of(company.get().getBreakfastDate().get(),LocalTime.of(0, 0));
		LocalDateTime endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(),LocalTime.of(0, 0));
		
		BreakfastMappedItems mbItem = new BreakfastMappedItems(user,startDate,endDate,form);
		 
		mampfCart.addToCart(mbItem,Quantity.of(mbItem.getAmount()));
		mampfCart.updateMBCart(startDate, endDate);
		
		return "redirect:/cart";
	}


	/**
	 * view buying site
	 */
	@GetMapping("/pay/{domain}")
	public String chooseToBuy(Model model,
							  @PathVariable String domain,
							  @ModelAttribute("form") CheckoutForm form,
							  @ModelAttribute("mampfCart") MampfCart mampfCart) {
		model.addAttribute("validations", new HashMap<String,List<String>>());
		return buyCart(domain, model, mampfCart, form);
	}

	/**
	 * buy cart(s)
	 */
	@PostMapping("/checkout")
	public String buy(Model model, @RequestParam(name = "reload")Optional<Boolean> reload, @Valid @ModelAttribute("form") CheckoutForm form, Errors result,
					  Authentication authentication, @ModelAttribute("mampfCart") MampfCart mampfCart) {
		mampfCart.updateCart(form);
		if(reload.isPresent()) {
			model.addAttribute("validations", new HashMap<String,List<String>>());
			return buyCart(form.getDomainChoosen(), model, mampfCart, form);
		}

		for (Item.Domain domain: form.getDomains()){
			if(!CheckoutForm.domainsWithoutForm.contains(domain.name())) { 
				LocalDateTime startDate = form.getStartDateTime(domain);
				LocalDateTime endDate = form.getEndDateTime(domain);
				String errVar = "allStartDates["+domain.name()+"]";
				String errDomain = "CheckoutForm.startDate";
				
				if(startDate == null || endDate == null) {
					result.rejectValue(errVar, errDomain+".Invalid","Bitte Datum eingeben!");
					continue;
				}
				if(startDate.isBefore(LocalDateTime.now().plus(delayForEarliestPossibleBookingDate))) {

					result.rejectValue(errVar, errDomain+".NotFuture", "Das Datum muss in der Zukunft liegen!");
				}
				if(startDate.isAfter(endDate)) {
					result.rejectValue(errVar, errDomain+".idk", "keine negativen Bestellungen erlaubt!");
				}
			}
		}
		
		Map<Item.Domain, DomainCart> carts = mampfCart.getDomainItems(form.getDomainChoosen());
		Map<Item.Domain, List<String>> validations = new HashMap<>();
		if(!result.hasErrors()) {
			validations = orderManager.validateCarts(carts);
		}
		
		Map<String, List<String>> validationsStr = new HashMap<>();
		
		if(!validations.isEmpty()) {
			result.rejectValue("generalError",
					"CheckoutForm.generalError.NoStuffLeft",
					"There is no free stuff or personal for the selected time left!");
			//TODO: append errors to form instead of to model
			validations.forEach(
				(domain,list)->validationsStr.put(domain.name(),list)
			);
		}
		
		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isEmpty()) {
			result.rejectValue("generalError",
					"CheckoutForm.generalError.NoLogin",
					"There was an error during your login process");
		}


		if (result.hasErrors()) {
			model.addAttribute("validations",validationsStr);
			return buyCart(form.getDomainChoosen(), model,mampfCart, form);
		}

		orderManager.createOrders(carts, form,user.get());
		//TODO: revenue updaten
		//revenue.save(new Gain(form.getStartDateTime(), money));
		//Money money = mampfCart.getTotal(domains.values());
		
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
	
	private String buyCart(String domain, Model model, MampfCart mampfCart, CheckoutForm form) {
		Map<Domain, DomainCart> domains = mampfCart.getDomainItems(domain);
		form.setDomainChoosen(domain);
		model.addAttribute("domains", domains);
		model.addAttribute("total", mampfCart.getTotal(domains.values()));
		model.addAttribute("form", form);
		return "buy_cart";
	}

/* ORDERS */

	/**
	 * lists orders ever for adminuser
	 */
	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	public String orders(Model model) {

		model.addAttribute("stuff", getSortedOrders(Optional.empty(), Optional.empty()));
		return "orders";
	}

	
	@GetMapping("/orders/detail/{orderId}")
	public String editOrder(@PathVariable String orderId, Model model) {
		
		Optional<MampfOrder> order = orderManager.findById(orderId);
		if(order.isEmpty()) {
			return "redirect:/";
		}
		model.addAttribute("order", order.get());
		return "ordersDetail";
	}

	/**
	 * lists orders of a user
	 */
	@GetMapping("/userOrders")
	public String orderUser(Model model, @LoggedIn Optional<UserAccount> userAccount) {
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		
		model.addAttribute("stuff", getSortedOrders(Optional.of(MampfOrder.comparatorSortByCreation), userAccount));
		return "orders";
	}
	
	private Map<String,List<MampfOrder>> getSortedOrders(Optional<Comparator<MampfOrder>> comp, Optional<UserAccount> userAccount){
		
		Map<String,List<MampfOrder>> stuff = new LinkedHashMap<>();
		List<MampfOrder> orders = new ArrayList<>();
		if(userAccount.isPresent()) {
			orders = orderManager.findByUserAcc(userAccount.get());
		}else {
			orders = orderManager.findAll();
		}
		if(comp.isPresent()) {
			orders.stream().sorted(comp.get());
		}else {
			orders.stream().sorted();	
		}
		
		for(MampfOrder order:orders) {
			String insertTo;
			if(comp.isPresent()) {
				insertTo = order.getDateCreated().toLocalDate().toString();
				if(order.getDateCreated().isAfter(LocalDateTime.now().minus(Duration.ofHours(1)))) {
					insertTo = "soeben erstellt";
				}
			}else {
				insertTo = order.getStartDate().toLocalDate().toString();
			}
			
			if(stuff.containsKey(insertTo)) {
				stuff.get(insertTo).add(order);
			}else {
				List<MampfOrder> newList = new ArrayList<>();
				newList.add(order);
				stuff.put(insertTo,newList);
			}
		}
		
		if(comp.isPresent()) {
			stuff.forEach((k,list)->list.stream().sorted(comp.get()));
		}else {
			stuff.forEach((k,list)->list.stream().sorted());	
		}
		return stuff;
	}
}
