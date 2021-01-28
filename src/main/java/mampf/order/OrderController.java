package mampf.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;

import mampf.order.MampfCart.DomainCart;
import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;
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

import mampf.Util;
import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;


import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("mampfCart")
public class OrderController {

	private UserManagement userManagement;
	private final MampfOrderManager orderManager;
	public static final TemporalAmount delayForEarliestPossibleBookingDate = Duration.ofHours(5);
	public static final TemporalAmount durationForEarliestBookingDate = Duration.ofHours(1);
	public static final LocalTime timeForEarliestPossibleBookingDate = LocalTime.of(5, 0);

	/**
	 * represents a Mobile Breakfast cart item.</br>
	 * - contains the chosen {@link BreakfastItem} dish and beverage, and other information about the breakfast</br>
	 *
	 * @author Konstii
	 */
	public class BreakfastMappedItems extends Item {

		private final LocalTime breakfastTime;
		private List<DayOfWeek> weekDays = new ArrayList<>();
		private final String address;
		private final BreakfastItem beverage, dish;
		private final long amount;

		/**
		 * setup Mobile Breakfast item with:</br>
		 * <li> formated weekDays of {@link DayOfWeek} from form </li>
		 * <li> a unchangable calculated amount of breakfast Dates for the given start and end Date</li>
		 *
		 * @param startDate a {@link LocalDateTime}, a breakfast date of a {@link Company}
		 * @param endDate   a {@link LocalDateTime}, a breakfastEnd date of a {@link Company}
		 * @param address   a {@link String}, a adress of a {@link Company}
		 * @param form      contains information about the chosen mobile breakfast informations
		 */
		public BreakfastMappedItems(LocalDateTime startDate, LocalDateTime endDate,
									String address,
									MobileBreakfastForm form) {

			super("Mobile Breakfast für " + form.getBeverage().getName() + " und " + form.getDish().getName(),
					BreakfastItem.BREAKFAST_PRICE, Item.Domain.MOBILE_BREAKFAST, Item.Category.FOOD,
					"temp");

			List<String> days = form.getDays().keySet().stream().filter(
					k -> form.getDays().get(k)). // get all marked
					map(String::toUpperCase).collect(Collectors.toList());
			for (DayOfWeek weekDay : DayOfWeek.values()) {
				if (days.contains(weekDay.name())) {
					weekDays.add(weekDay);
				}
			}

			breakfastTime = form.getTime();
			setDescription("vom " + startDate.toLocalDate() + " bis " + endDate.toLocalDate());
			beverage = form.getBeverage();
			dish = form.getDish();
			this.address = address;
			amount = MBOrder.getAmount(startDate, endDate, startDate, endDate, weekDays, breakfastTime);


		}

		public LocalTime getBreakfastTime() {
			return breakfastTime;
		}

		public BreakfastItem getDish() {
			return dish;
		}

		public String getAddress() {
			return address;
		}

		public BreakfastItem getBeverage() {
			return beverage;
		}

		public List<DayOfWeek> getWeekDays() {
			return weekDays;
		}

		public long getAmount() {
			return amount;
		}
	}

	public OrderController(MampfOrderManager orderManager, UserManagement userManagement) {
		this.orderManager = orderManager;
		this.userManagement = userManagement;
	}

	/* CART */

	@ModelAttribute("mampfCart")
	MampfCart initializeCart() {

		return new MampfCart();
	}

	/**
	 * adds item with {@link Quantity} of number to mampfCart
	 *
	 * @param item      the item which will be added to the {@link MampfCart}
	 * @param number    the amount which will be added to the {@link MampfCart}
	 * @param mampfCart
	 * @return /catalog
	 */
	@PostMapping("/cart")
	public String addItem(@RequestParam("pid") Item item, @RequestParam("number") int number,
						  @ModelAttribute("mampfCart") MampfCart mampfCart) {
		if (number > 0) {
			mampfCart.addToCart(item, Quantity.of(number));
		}
		return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
	}

	/**
	 * view items of mampfcart.</br>
	 * - resets mampfcart Dates.
	 *
	 * @param model
	 * @param mampfCart
	 * @return cart template
	 */
	@GetMapping("/cart")
	public String basket(Model model, @ModelAttribute("mampfCart") MampfCart mampfCart) {

		mampfCart.resetCartDate();
		Map<Item.Domain, DomainCart> domains = mampfCart.getStuff();
		model.addAttribute("domains", domains);
		model.addAttribute("total", mampfCart.getTotal(null));
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
	 * handles adding and removing the amount of a {@link CartItem}.</br>
	 * - when newAmount less than 1, the {@link CartItem} will be removed.
	 *
	 * @param cartItemId Id of the {@link CartItem}
	 * @param newAmount  new Amount of the {@link CartItem}
	 * @param mampfCart
	 * @return cart template redirect
	 */
	@PostMapping("cart/setNewAmount")
	public String addCartItem(@RequestParam String cartItemId, @RequestParam int newAmount,
							  @ModelAttribute("mampfCart") MampfCart mampfCart) {

		CartItem cartItem = mampfCart.getCartItem(cartItemId);
		if (cartItem != null) {
			mampfCart.updateCart(cartItem, newAmount);
		}
		return "redirect:/cart";
	}

	/**
	 * handles ordering mobile breakfast.</br>
	 * when ordering mobile breakfast, a new instance of {@link BreakfastMappedItems} can be added to the mampfCart.</br>
	 * {@link BreakfastMappedItems} contains (nearly)all informations about the ordered mobile breakfast,
	 * which is needed when creating the actual order from the cart items.</br>
	 * when trying to order a new mobile breakfast, there are many errors which can occur:
	 * <ul>
	 * <li>invalid form</li>
	 * <li>the {@link User} of the userAccount is no employee</li>
	 * <li>the {@link Company} of the employee has not booked Mobile Breakfast</li>
	 * <li>the {@link Company} of the employee has booked Mobile Breakfast,
	 * but the booking date is no longer available</li>
	 * <li>the {@link Company} of the employee has booked Mobile Breakfast,
	 * but the employee has already ordered a mobile breakfast</li>
	 * </ul>
	 *
	 * @param userAccount        of the {@link User} who wants to order mb
	 * @param form
	 * @param mampfCart
	 * @param redirectAttributes holds errormessage
	 * @return cart/mobile-breakfast template redirect
	 */
	@PostMapping("/cart/add/mobile-breakfast")
	public String orderMobileBreakfast(@LoggedIn Optional<UserAccount> userAccount, @Valid MobileBreakfastForm form,
									   @ModelAttribute("mampfCart") MampfCart mampfCart, RedirectAttributes redirectAttributes) {

		String redirect = "redirect:/mobile-breakfast";
		if (userAccount.isEmpty()) {
			return "redirect:/login";
		}
		// ERRORS:
		String error = "error", errVal = null;
		if (form.getBeverage() == null) {
			errVal = "Kein Getränk ausgewählt";
		}
		if (form.getDish() == null) {
			errVal = "Nichts zum Essen ausgewählt";
		}
		if (form.getDays().values().stream().allMatch(v -> !v.booleanValue())) {
			errVal = "Keinen Wochentag ausgewählt";
		}
		if (!orderManager.hasBookedMB(userAccount.get())) {
			errVal = "Für diesen Monat wurde kein Mobile Breakfast bestellt, oder es wurde bereits bestellt";
		}
		if (errVal != null) {
			redirectAttributes.addFlashAttribute(error, errVal);
			return redirect;
		}

		User user = userManagement.findUserByUserAccount(userAccount.get().getId()).get();
		Optional<Company> company = userManagement.findCompany(user.getId());
		LocalDateTime startDate = LocalDateTime.of(company.get().getBreakfastDate().get(), LocalTime.of(0, 0));
		LocalDateTime endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(), LocalTime.of(0, 0));

		BreakfastMappedItems mbItem = new BreakfastMappedItems(startDate, endDate,
				userManagement.findUserById(company.get().getBossId()).get().getAddress(),
				form);

		if (mbItem.getAmount() == 0) {
			redirectAttributes.addFlashAttribute(error, "Die Bestellung beinhaltet keine Produkte");
			return redirect;
		}

		mampfCart.addToCart(mbItem, Quantity.of(mbItem.getAmount()));
		mampfCart.updateMBCart(startDate, endDate);

		return "redirect:/cart";
	}

	/**
	 * view buying site.</br>
	 * <p>
	 * checks domain for possible {@link Item.Domain} matches. </br>
	 * If there was a match, return the buying site for the matching {@link Item.Domain}.
	 * Otherwise return the buying site for every {@link Item.Domain}.</br>
	 * - resets mampfCart dates.
	 *
	 * @param model
	 * @param domain    requested paying domain, can be a {@link Item.Domain} name
	 * @param form
	 * @param mampfCart
	 * @return cart template redirect or buy_cart template
	 */
	@GetMapping("/pay/{domain}")
	public String chooseToBuy(Model model, @PathVariable String domain, @ModelAttribute("form") CheckoutForm form,
							  @ModelAttribute("mampfCart") MampfCart mampfCart, Authentication authentication) {

		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isEmpty()) {
			return "redirect:/login";
		}

		if (mampfCart.isEmpty()) {
			return "redirect:/cart";
		}

		Item.Domain domainChoosen = null;
		String domainStr = Util.renderDomainName(domain);
		for (Item.Domain d : Item.Domain.values()) {
			if (d.name().equals(domain) || domainStr.equals(Util.renderDomainName(d.name()))) {
				domainChoosen = d;
				break;
			}
		}
		mampfCart.resetCartDate();
		model.addAttribute("validations", new HashMap<String, List<String>>());
		return buyCart(domainChoosen, model, mampfCart, form, user.get());
	}

	/**
	 * checkout request to reload the site with updated carts or buy carts.</br>
	 * there are two differnt use cases: </br>
	 * 1) the user can reload the site with updated cart prizes. </br>
	 * 2) the user can buy the carts:</br>
	 * <ol>
	 * <li>the form will be validated (there should be no empty field)</li>
	 * <li>the form's data will be validated</li>
	 * <li>the carts will be validated</li>
	 * <li> when there is any error message in the validations,
	 * the buying process will stop and the modified buying site with the errors will show up</li>
	 * <li> otherwise the orders will be created and the selected {@link DomainCart} will be deleted from mampfCart,
	 * and the user will be redirected to their orders</li>
	 * </ol>
	 *
	 * @param model
	 * @param reload         a {@link Optional} of {@link Boolean}, reloads the buying site if present
	 * @param form           a {@link CheckoutForm} which contains the chosen dates of the orders
	 * @param result         {@link Errors} for validating the form
	 * @param authentication a {@link Authentication} which represents a user
	 * @param mampfCart
	 * @return buy_cart template or userOrders template redirect
	 */
	@PostMapping("/checkout")
	public String buy(Model model, @RequestParam(name = "reload") Optional<Boolean> reload,
					  @Valid @ModelAttribute("form") CheckoutForm form, Errors result, Authentication authentication,
					  @ModelAttribute("mampfCart") MampfCart mampfCart) {

		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isEmpty()) {
			result.rejectValue("generalError", "CheckoutForm.generalError.NoLogin",
					"Bitte melden sie sich erneut an");
			return "redirect:/login";
		}
		if (form.hasValidData()) {
			mampfCart.updateCart(form);
		} else {
			result.rejectValue("generalError", "CheckoutForm.generalError.missingData",
					"die Felder müssen ausgefüllt sein");
		}

		Map<String, List<String>> validationsStr = new HashMap<>();

		if (reload.isPresent()) {
			model.addAttribute("validations", validationsStr);
			return buyCart(form.getDomainChosen(), model, mampfCart, form, user.get());
		}

		validateCheckoutForm(form, result);

		if (result.hasErrors()) {
			model.addAttribute("validations", validationsStr);
			return buyCart(form.getDomainChosen(), model, mampfCart, form, user.get());
		}

		Map<Item.Domain, DomainCart> carts = mampfCart.getDomainItems(form.getDomainChosen());
		Map<Item.Domain, List<String>> validations = orderManager.validateCarts(user.get().getUserAccount(), carts);

		if (!validations.isEmpty()) {
			result.rejectValue("generalError", "CheckoutForm.generalError.NoStuffLeft",
					"Items konnten nicht validiert werden");
			validations.forEach((domain, list) -> validationsStr.put(domain.name(), list));
		}

		if (result.hasErrors()) {
			model.addAttribute("validations", validationsStr);
			return buyCart(form.getDomainChosen(), model, mampfCart, form, user.get());
		}

		orderManager.createOrders(carts, form, user.get());

		List<Item.Domain> domainsToRemove = new ArrayList<>(carts.keySet());
		domainsToRemove.forEach(mampfCart::removeCart);

		return "redirect:/userOrders";
	}

	/**
	 * validates a valid form.</br>
	 * will create Errors by rejecting when there are problems using the given form dates for creating orders. </br>
	 * (when the endDate is before  the start Date/...)
	 *
	 * @param form
	 * @param result
	 */
	private void validateCheckoutForm(CheckoutForm form, Errors result) {
		if (result.hasErrors()) {
			return;
		}
		for (Item.Domain domain : form.getDomains()) {
			if (CheckoutForm.domainsWithoutForm.contains(domain.name())) {
				continue;
			}

			LocalDateTime startDate = form.getStartDateTime(domain);
			LocalDateTime endDate = form.getEndDateTime(domain);
			String errVar = "allStartDates[" + domain.name() + "]";
			String errDomain = "CheckoutForm.startDate";

			if (startDate.isBefore(LocalDateTime.now().plus(delayForEarliestPossibleBookingDate))) {
				result.rejectValue(errVar, errDomain + ".NotFuture",
						"Das Datum muss in der Zukunft liegen!");
			}
			if (startDate.toLocalTime().isBefore(timeForEarliestPossibleBookingDate)) {
				result.rejectValue(errVar, errDomain + ".TimeMin", "Zu früh!");
			}
			if (startDate.isAfter(endDate)) {
				result.rejectValue(errVar, errDomain + ".NegativeDate",
						"keine negativen Bestellungen erlaubt!");
			}
			if (Duration.between(startDate, endDate).compareTo((Duration) durationForEarliestBookingDate) < 0) {
				result.rejectValue(errVar, errDomain + ".DurationMin", "Zu kurz!");
			}

		}
	}

	/**
	 * views the buying site for a given domain.</br>
	 * sets the model and form for the template.
	 *
	 * @param domain    a {@link DomainCart}, can be {@code null}
	 * @param model
	 * @param mampfCart
	 * @param form
	 * @return buy_cart template
	 */
	private String buyCart(Item.Domain domain, Model model, MampfCart mampfCart, CheckoutForm form, User user) {
		form.setDomainChosen(domain);
		Map<Item.Domain, DomainCart> domains = mampfCart.getDomainItems(domain);
		model.addAttribute("canSubmit", domains.values().stream().allMatch(
				cart -> (cart.getStartDate() != null && cart.getEndDate() != null)));
		model.addAttribute("domains", domains);
		model.addAttribute("total", mampfCart.getTotal(domain));
		model.addAttribute("userAddress", user.getAddress());
		model.addAttribute("form", form);
		return "buy_cart";
	}

	/* ORDERS */


	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	public String orders(Model model) {
		model.addAttribute("stuff", getSortedOrders(Optional.empty(), Optional.empty()));
		return "orders";
	}

	/**
	 * shows details of an {@link MampfOrder}.
	 *
	 * @param orderId
	 * @param model
	 * @return ordersDetail template
	 */
	@GetMapping("/orders/detail/{orderId}")
	public String editOrder(@PathVariable String orderId, Model model) {

		Optional<MampfOrder> order = orderManager.findById(orderId);
		if (order.isEmpty()) {
			return "redirect:/login";
		}
		model.addAttribute("order", order.get());
		model.addAttribute("isMB", order.get() instanceof MBOrder);
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

	/**
	 * deletes a {@link MampfOrder}.</br>
	 * the boss only can delete orders.</br>
	 * redirects to the starting page if there was no {@link MampfOrder} found.
	 *
	 * @param orderId Id of an @
	 * @return orders template redirect or index template redirect
	 */
	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/orders/delete/{orderId}")
	public String deleteOrder(@PathVariable Optional<String> orderId) {
		String redirect = "redirect:/";
		if (orderId.isEmpty()) {
			return redirect;
		}
		Optional<MampfOrder> order = orderManager.findById(orderId.get());
		if (order.isEmpty()) {
			return redirect;
		}
		orderManager.deleteOrder(order.get());
		return "redirect:/orders";
	}

	/**
	 * sorts every/only user {@link MampfOrder} by given comp and saves them into a {@link Map}.</br>
	 * - orders will be categorized by a {@link java.time.LocalDate} which will be converted into a {@link String}. </br>
	 * <p>
	 * when the orders are sorted by their creation Date, there is another category beside {@link java.time.LocalDate}
	 * which is called "soeben erstellt".</br>
	 * "soeben erstellt" contains every {@link MampfOrder} which was created in the last hour.</br>
	 *
	 * @param comp        a {@link Optional} of {@link java.util.function.Predicate}, uses the natural ordering of
	 *                    {@link MampfOrder} when the {@link Optional} is {@code empty}
	 * @param userAccount
	 * @return a new instance of {@link Map} which contains sorted {@link MampfOrder} by {@link java.time.LocalDate}
	 */
	private Map<String, List<MampfOrder>> getSortedOrders(Optional<Comparator<MampfOrder>> comp,
														  Optional<UserAccount> userAccount) {

		Map<String, List<MampfOrder>> stuff = new LinkedHashMap<>();
		List<MampfOrder> orders;
		if (userAccount.isPresent()) {
			orders = orderManager.findByUserAcc(userAccount.get());
		} else {
			orders = orderManager.findAll();
		}
		if (comp.isPresent()) {
			orders.stream().sorted(comp.get());
		} else {
			orders.stream().sorted();
		}
		//inserted sorted keys:
		for (MampfOrder order : orders) {
			String insertTo;
			if (comp.isPresent()) {
				insertTo = order.getDateCreated().toLocalDate().toString();
				if (order.getDateCreated().isAfter(LocalDateTime.now().minus(Duration.ofHours(1)))) {
					insertTo = "soeben erstellt";
				}
			} else {
				insertTo = order.getStartDate().toLocalDate().toString();
			}

			if (stuff.containsKey(insertTo)) {
				stuff.get(insertTo).add(order);
			} else {
				List<MampfOrder> newList = new LinkedList<>();
				newList.add(order);
				stuff.put(insertTo, newList);
			}
		}
		return stuff;
	}
}
