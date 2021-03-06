package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfCart.DomainCart;
import mampf.order.OrderController.BreakfastMappedItems;
import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.LineItemFilter;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.OrderLine;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

/**
 * class to manage {@link MampfOrder}
 *
 */
@Component
public class MampfOrderManager {

	@Bean
	LineItemFilter filter() {
		return item -> false;
	}

	private final OrderManagement<MampfOrder> orderManagement;
	private final Inventory inventory;
	private final MampfCatalog catalog;
	private final EmployeeManagement employeeManagement;
	private final UserManagement userManagement;

	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, Inventory inventory,
							 EmployeeManagement employeeManagement, UserManagement userManagement, MampfCatalog catalog) {
		this.orderManagement = orderManagement;
		this.inventory = inventory;
		this.employeeManagement = employeeManagement;
		this.catalog = catalog;
		this.userManagement = userManagement;
	}

	/**
	 * returns if the user for the given {@link UserAccount} can book mobile breakfast:</br>
	 * <ul>
	 * <li> the user needs to be a employee</li>
	 * <li> the company of the employee should have a available/current breakfast Date</li>
	 * <li> the user should not already have booked mobile breakfast for the available breakfast Date</li>
	 * </ul>
	 *
	 * @param userAccount {@link UserAccount}
	 * @return {@code true} if the user is able to buy the ordered mobile breakfast, otherwise {@code false}
	 */
	public boolean hasBookedMB(UserAccount userAccount) {

		//the user is no valid user:
		Optional<User> user = userManagement.findUserByUserAccount(userAccount.getId());
		if (user.isEmpty()) {
			return false;
		}
		
		return hasBookedMBPart(user, userAccount);
	}

	/**
	 * part of {@link MampfOrderManager#hasBookedMB(UserAccount)}
	 * @param user {@link User}
	 * @param userAccount {@link UserAccount}
	 * @return {@code true} if the user is able to buy the ordered mobile breakfast, otherwise {@code false}
	 */
	
	private boolean hasBookedMBPart(Optional<User> user, UserAccount userAccount){
		//the user is no employee:
		Optional<Company> company = userManagement.findCompany(user.get().getId());
		if (company.isEmpty()) {
			return false;
		}
		Optional<User> boss = userManagement.findUserById(company.get().getBossId());
		//the company of the user has not booked mb:
		if (!company.get().hasBreakfastDate() || company.get().getBreakfastDate().isEmpty() || boss.isEmpty()) {
			return false;
		}
		//the user already has ordered their choice:
		return findByTimeSpan(Optional.of(userAccount), LocalDateTime.of(company.get().getBreakfastDate().get(),
				LocalTime.MIN), LocalDateTime.of(company.get().getBreakfastEndDate().get(), LocalTime.MIN)).stream().
				noneMatch(order -> order instanceof MBOrder && order.getAddress().equals(boss.get().getAddress()));
	}

	/**
	 * creates Validations for a {@link Map} of {@link Domain} and {@link DomainCart} for a user with
	 * {@link UserAccount}. </br>
	 * a {@link DomainCart} is valid when:</br>
	 * <ul>
	 * <li>the requested items/employees are available </li>
	 * <li>the cart has a valid start and end-date </li>
	 * </ul>
	 * when a {@link DomainCart} is invalid, the validations will be filled with the fitting {@link String} errormessage
	 *
	 * @param carts       the chosen {@link DomainCart} mapped to its {@link Domain}
	 * @param userAccount user which requests to buy the chosen carts
	 * @return validations a new instance of {@link Map} which maps the created error messages
	 * to the fitting {@link DomainCart}
	 */
	public Map<Item.Domain, List<String>> validateCarts(UserAccount userAccount, Map<Item.Domain, DomainCart> carts) {
		// each domain can have multiple error messages:
		Map<Item.Domain, List<String>> validations = new EnumMap<>(Item.Domain.class);

		// get base inventory of all finite items:
		List<UniqueMampfItem> baseInv = new ArrayList<>(inventory.findAll().
				filter(i -> !Inventory.infinity.contains(i.getCategory())).toList());
		// init base carts:
		List<DomainCart> baseCarts = new ArrayList<>();
		// init employees:
		Map<Employee.Role, Quantity> baseEmp = new EnumMap<>(Employee.Role.class);
		for (Employee.Role role : Employee.Role.values()) {
			baseEmp.put(role, employeeManagement.getEmployeeAmount(role));
		}
		// cart should be updated correctly
		// get available Items:
		carts.forEach((domain, cart) -> {
			LocalDateTime startDate = cart.getStartDate();
			LocalDateTime endDate = cart.getEndDate();
			if (startDate == null || endDate == null) {
				updateValidations(validations, domain, "fehlende zeitangaben");
				return;
			}
			if (domain.equals(Domain.MOBILE_BREAKFAST) && !hasBookedMB(userAccount)) {
				updateValidations(validations, domain, "nicht mehr aktuell, oder bereits bestellt");
				return;
			}
			iterateThroughCart(cart, validations, domain, getFreeItems(baseInv, baseCarts, baseEmp, startDate, endDate));
			baseCarts.add(cart);
		});
		return validations;
	}

	/**
	 * used to reduce the complexity of {@link MampfOrderManager#validateCarts(UserAccount, Map)}
	 * @param cart the function iterates through cart
	 * @param validations used in updateValidations
	 * @param domain {@link Domain}
	 * @param inventorySnapshot temporary List of {@link UniqueMampfItem}
	 */
	private void iterateThroughCart(Cart cart, Map<Domain, List<String>> validations,
									Domain domain, List<UniqueMampfItem> inventorySnapshot){
		for (CartItem cartitem : cart) {
			// de-map mapper-cart items:
			for (CartItem cartItem : createCheckItems(cartitem)) {
				Optional<Item> catalogItem = catalog.findById(cartItem.getProduct().getId());
				if (catalogItem.isEmpty()) {
					updateValidations(validations, domain, "Item nicht vorhanden:"
							+ cartitem.getProductName());
					continue;
				}
				Optional<UniqueMampfItem> inventoryItem = checkForAmount(inventorySnapshot, cartItem);
				if (inventoryItem.isPresent()) {
					String validationState = "Keine verf??gbare Auswahl von '" + catalogItem.get().getName() + "' : " +
							" verbleibende Anzahl: "
							+ inventoryItem.get().getQuantity().getAmount();
					updateValidations(validations, domain, validationState);
				}
			}
		}
	}

	/**
	 * creates {@link MampfOrder} for the given carts and saves them in the {@link OrderManagement}</br>
	 * - sets staff to the orders (personal)
	 *
	 * @param carts chosen {@link DomainCart} mapped to {@link Domain}
	 * @param form {@link CheckoutForm}
	 * @param user {@link User}
	 * @return a new instance of {@link List} of {@link MampfOrder}
	 */
	public List<MampfOrder> createOrders(Map<Item.Domain, DomainCart> carts, CheckoutForm form, User user) {

		List<MampfOrder> orders = new ArrayList<>();
		for (Map.Entry<Domain, DomainCart> entry : carts.entrySet()) {
			Domain domain = entry.getKey();
			DomainCart cart = entry.getValue();

			// get Dates:
			LocalDateTime startDate = cart.getStartDate();
			LocalDateTime endDate = cart.getEndDate();
			String address = form.getAddress(domain);
			if (address == null || address.isEmpty()) {
				address = user.getAddress();
			}

			MampfOrder order;
			if (domain.equals(Domain.MOBILE_BREAKFAST)) {
				order = createOrderMB(cart.iterator().next(), startDate, endDate, form, user.getUserAccount());

			} else {
				// create usual order:
				order = new EventOrder(user.getUserAccount(), createPayMethod(form.getPayMethod(), user
						.getUserAccount()), domain, startDate, endDate, address);

				cart.addItemsTo(order);
				if (hasStaff(cart)) {
					setPersonalBooked((EventOrder) order, getPersonal(startDate, endDate));
				}
			}

			if (!orderManagement.payOrder(order)) {
				return orders;
			}

			orderManagement.completeOrder(order);

			orderManagement.save(order);
			orders.add(order);
		}
		return orders;

	}

	/**
	 * creates and returns a "inventory snapshot" (a {@link List} of {@link UniqueMampfItem})
	 * for a given timespan fromDate - toDate, 'tracking-items' baseInv and baseCarts
	 * <p>
	 * calculates for every baseInv {@link UniqueMampfItem} item the lasting amount: </br>
	 * <b>return baseInv - bookedItems(timespan) - bookedItems(timespan, baseCarts)</b></br>
	 * <ul>
	 * <li>does not modify any given parameter</li>
	 * <li>there is no amount left when the {@link Quantity} of the {@link UniqueMampfItem} has a value of zero </li>
	 * <li>items of {@link Inventory} infinity will be ignored</li>
	 * </ul>
	 *
	 * @param baseInv   a {@link List} of {@link UniqueMampfItem}, the base Inventory
	 * @param baseCarts a {@link List} of {@link DomainCart}, a collection of {@link Item} and {@link Quantity}
	 *                  which will reduce the base Inventory
	 * @param fromDate  timespan start
	 * @param toDate    timespan end
	 * @return a new instance of {@link List} of {@link UniqueMampfItem}, amount left of baseInv items
	 */
	public List<UniqueMampfItem> getFreeItems(List<UniqueMampfItem> baseInv, List<DomainCart> baseCarts,
											  Map<Employee.Role, Quantity> baseEmp,
											  LocalDateTime fromDate, LocalDateTime toDate) {
		//INIT:
		List<UniqueMampfItem> res = new ArrayList<>();
		// a "copy" of baseInv
		// items which has to be returned needs to be the same structure like baseInv:
		// also, the quantity will be modified during next steps
		baseInv.forEach(i -> res.add(new UniqueMampfItem(i.getItem(), Quantity.of(i.getAmount().longValue()))));
		// a "copy" of baseEmp
		// issue: items("STAFF") are requests which are not unique and refer to a specific resource type (employee),
		//        not a inventory resource
		// idea: calculate over requests the lasting resource,
		//       but update requests(items) depending on lasting resource as inventory item
		Map<Employee.Role, Quantity> personalLeft = new EnumMap<>(Employee.Role.class);
		baseEmp.forEach((role, q) -> personalLeft.put(role, Quantity.of(q.getAmount().longValue())));
		/*-----------------------------*/
		//GET:
		List<UniqueMampfItem> bookedItems = getBookedItems(fromDate, toDate);
		List<UniqueMampfItem> cartItems = getBookedItemsFromCart(baseCarts, fromDate, toDate);
		/*----------------------------*/
		//CALCULATE:
		Optional<UniqueMampfItem> actionItem;
		List<UniqueMampfItem> actionItems;
		for (UniqueMampfItem resItem : res) {
			for (int n = 0; n < 2; n++) {
				if (n == 0) {
					actionItems = bookedItems;
				} else {
					actionItems = cartItems;
				}

				actionItem = Optional.empty();
				someLoopFunction(actionItems, resItem, actionItem, personalLeft);
			}
		}
		/*----------------------------*/
		//UPDATE STAFF:

		res.stream().filter(i -> i.getCategory().equals(Category.STAFF)).collect(Collectors.toList()).
				forEach(j -> j.setQuantity(personalLeft.get(((StaffItem) j.getProduct()).getType())));

		return res;
	}



	private void someLoopFunction(List<UniqueMampfItem> actionItems, UniqueMampfItem resItem,
								  Optional<UniqueMampfItem> actionItem, Map<Employee.Role, Quantity> personalLeft){
		for (UniqueMampfItem bI : actionItems) {
			if (bI.getProduct().equals(resItem.getProduct())) {
				actionItem = Optional.of(bI);
				break;
			}
		}

		if (actionItem.isEmpty()) {
			return;
		}
		// subtract:

		if (actionItem.get().getCategory().equals(Category.STAFF)) {
			Employee.Role role = ((StaffItem) actionItem.get().getProduct()).getType();
			personalLeft.put(role, reduceValidationQuantity(personalLeft.get(role),
					actionItem.get().getQuantity()));
		} else if (!Inventory.infinity.contains(resItem.getCategory())) {
			resItem.setQuantity(reduceValidationQuantity(resItem.getQuantity(),
					actionItem.get().getQuantity()));
		}
	}

	/**
	 * checks the baseCarts for time-overlapping carts and returns their items </br>
	 * - handling carts as if they were already orders
	 *
	 * @param baseCarts already validated {@link DomainCart}
	 * @param fromDate  timespan start
	 * @param toDate    timespan end
	 * @return a new instance of {@link List} of {@link UniqueMampfItem}
	 * every needed items with amount in the given timespan
	 */

	private List<UniqueMampfItem> getBookedItemsFromCart(List<DomainCart> baseCarts,
														 LocalDateTime fromDate,
														 LocalDateTime toDate) {
		// each cart has a time span
		// when time span colliding with given time span
		// add all concrete items (f.e. BreakfastMappedItem is no concrete item, but the content)
		List<UniqueMampfItem> cartItems = new ArrayList<>();
		for (DomainCart cart : baseCarts) {
			if (!MampfOrder.hasTimeOverlap(fromDate, toDate, cart.getStartDate(), cart.getEndDate())) {
				continue;
			}
			CartItem firstItem = cart.iterator().next(); //there are no empty carts
			if (((Item) firstItem.getProduct()).getDomain().equals(Domain.MOBILE_BREAKFAST)) {
				BreakfastMappedItems bfItem = (BreakfastMappedItems) firstItem.getProduct();
				Quantity mbQuantity = Quantity.of(MBOrder.getAmount(fromDate, toDate, cart.getStartDate(),
						cart.getEndDate(), bfItem.getWeekDays()));

				cartItems.add(new UniqueMampfItem(bfItem.getBeverage(), mbQuantity));
				cartItems.add(new UniqueMampfItem(bfItem.getDish(), mbQuantity));
			} else {
				cart.forEach(cartItem ->
						cartItems.add(new UniqueMampfItem((Item) cartItem.getProduct(), cartItem.getQuantity())));
			}

		}
		return cartItems;
	}

	/**
	 * subtracts sub from origin according to {@link Quantity} minima ({@code zero})
	 *
	 * @param origin {@link Quantity}
	 * @param sub {@link Quantity}
	 * @return origin - sub or {@link Quantity} of {@code zero}
	 */
	private Quantity reduceValidationQuantity(Quantity origin, Quantity sub) {
		if (origin.isEqualTo(Quantity.of(0))) {
			return origin;
		}
		if (sub.isGreaterThan(origin)) {
			return Quantity.of(0);
		}

		return origin.subtract(sub);
	}

	/**
	 * creates and returns a list of all ordered items as {@link UniqueMampfItem} for a given timespan
	 *
	 * @param fromDate timespan start
	 * @param toDate   timespan end
	 * @return a new instance of {@link List} of {@link UniqueMampfItem}
	 */
	public List<UniqueMampfItem> getBookedItems(LocalDateTime fromDate, LocalDateTime toDate) {
		return convertToInventoryItems(getOrderItems(fromDate, toDate));
	}

	/**
	 * converts {@link ProductIdentifier} and {@link Quantity} to a list of items
	 *
	 * @param itemMap {@link Map}
	 * @return a new instance of {@link List} of {@link UniqueMampfItem}
	 */
	public List<UniqueMampfItem> convertToInventoryItems(Map<ProductIdentifier, Quantity> itemMap) {
		List<UniqueMampfItem> res = new ArrayList<>();
		itemMap.forEach((id, q) -> {
			Optional<Item> catalogItem = catalog.findById(id);
			catalogItem.ifPresent(item -> res.add(new UniqueMampfItem(item, q)));
		});
		return res;
	}

	/**
	 * creates and returns a list of every {@link MampfOrder} of a {@link UserAccount}
	 *
	 * @param account {@link UserAccount}
	 * @return a new instance of {@link List} of {@link MampfOrder}
	 */
	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account)) {
			res.add(order);
		}
		return res;
	}

	/**
	 * deletes a given {@link MampfOrder} from the {@link org.salespointframework.order.OrderManagement}
	 * when order is a {@link EventOrder}, removes {@link Employee} assignments to the given order
	 *
	 * @param order {@link MampfOrder}
	 */
	public void deleteOrder(MampfOrder order) {
		for (MampfOrder order_ : findAll()) {
			if (order.equals(order_)) {
				if (order_ instanceof EventOrder) {
					order.getEmployees().forEach(e -> e.removeBookedOrder((EventOrder) order));
				}
				orderManagement.delete(order_);
				return;
			}

		}
	}

	public Optional<MampfOrder> findById(String orderId) {
		return orderManagement.findAll(Pageable.unpaged()).
				filter(order -> Objects.requireNonNull(order.getId()).getIdentifier().equals(
						orderId)).get().findFirst();
	}

	/**
	 * returns every order for a given userAccount and timespan</br>
	 * - if the userAccount is {@code empty} every order of every user will be chosen
	 *
	 * @param userAccount {@link UserAccount}
	 * @param fromDate    timespan start
	 * @param toDate      timespan end
	 * @return a {@link Streamable} of {@link MampfOrder}
	 */
	public Streamable<MampfOrder> findByTimeSpan(Optional<UserAccount> userAccount,
												 LocalDateTime fromDate, LocalDateTime toDate) {
		Streamable<MampfOrder> orders;
		if (userAccount.isEmpty()) {
			orders = orderManagement.findAll(Pageable.unpaged());
		} else {
			orders = orderManagement.findBy(userAccount.get());
		}
		return orders.filter(order -> order.hasTimeOverlap(fromDate, toDate));
	}

	public List<MampfOrder> findAll() {
		return orderManagement.findBy(OrderStatus.COMPLETED).toList();
	}

	/**
	 * creates, depending on payMethod the correct {@link PaymentMethod}:
	 * <ul>
	 * <li>"{@code Check}" will return a new {@link Cheque} with the Mampf-banking information</li>
	 * <li>otherwise, returns a new {@link Cash}</li>
	 * </ul>
	 *
	 * @param payMethod   a {@link CheckoutForm} attribute
	 * @param userAccount purchaser
	 * @return {@link PaymentMethod}
	 */
	private PaymentMethod createPayMethod(String payMethod, UserAccount userAccount) {
		PaymentMethod method = Cash.CASH;
		if (payMethod.equals("Check")) {
			method = new Cheque(userAccount.getUsername(), userAccount.getId().getIdentifier(), "checknummer 1",
					userAccount.getFirstname(), LocalDateTime.now(), "MampfBank", "Lindenallee 12", "1223423478");
		}
		return method;
	}

	/**
	 * returns if the cart has a {@link CartItem} which has a {@link Item} with category {@link Category#STAFF}
	 *
	 * @param cart {@link Cart}
	 * @return {@code true} if so, else {@code false}
	 */
	private boolean hasStaff(Cart cart) {
		return (cart.get().anyMatch(cartItem -> ((Item) cartItem.getProduct()).getCategory().equals(
				Item.Category.STAFF)));

	}

	/**
	 * returns every available {@link Employee} for a given timespan, sorted by its {@link Employee.Role}
	 *
	 * @param startDate timespan start
	 * @param endDate   timespan  end
	 * @return a new instance of {@link Map} which maps a {@link List} of {@link Employee} to its {@link Employee.Role}
	 */
	private Map<Employee.Role, List<Employee>> getPersonal(LocalDateTime startDate, LocalDateTime endDate) {
		Map<Employee.Role, List<Employee>> personalLeft = new EnumMap<>(Employee.Role.class);

		for (Employee.Role role : Employee.Role.values()) {
			List<Employee> xcy = employeeManagement.getFreeEmployees(startDate, endDate, role);
			personalLeft.put(role, xcy);

		}
		return personalLeft;
	}

	/**
	 * given a {@link CartItem}, creates a list of {@link CartItem} which have existing catalog-{@link Item}
	 * <li>when the cart item has a {@link BreakfastMappedItems} as product, the returned
	 * items will contain {@link mampf.catalog.BreakfastItem} as product
	 * ({@link BreakfastMappedItems} is not a catalog-{@link Item})</li>
	 *
	 * @param cartItem to check
	 * @return a new instance of {@link List} of {@link CartItem}
	 */
	private List<CartItem> createCheckItems(CartItem cartItem) {

		Item item = ((Item) cartItem.getProduct());
		List<CartItem> checkItems = new ArrayList<>();
		if (item.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)) {
			BreakfastMappedItems bfItem = ((BreakfastMappedItems) item);
			checkItems.add(new Cart().addOrUpdateItem(bfItem.getBeverage(), cartItem.getQuantity()));
			checkItems.add(new Cart().addOrUpdateItem(bfItem.getDish(), cartItem.getQuantity()));
		} else {
			checkItems.add(cartItem);
		}
		return checkItems;
	}

	/**
	 * checks if the given inventorySnapshot has enough amount left for the requested amount of the requested item
	 * <li>only checks amount if the requested amount is finite</li>
	 * <li>otherwise (the inventorySnapshot has no item of this type,
	 * or the amount of the requested amount is infinite) will accept the request</li>
	 *
	 * @param inventorySnapshot the calculated available items
	 * @param checkItem         requested item and amount
	 * @return accepts the amount by returning a {@code empty} {@link Optional}, otherwise returns a
	 * {@link Optional} of the inventorySnapshot {@link UniqueMampfItem}
	 */
	private Optional<UniqueMampfItem> checkForAmount(List<UniqueMampfItem> inventorySnapshot, CartItem checkItem) {
		Optional<UniqueMampfItem> inventoryItem = inventorySnapshot.stream().filter(i ->
				i.getProduct().equals(checkItem.getProduct())).findFirst();
		Item catalogItem = ((Item) checkItem.getProduct());


		if (inventoryItem.isPresent() &&
				(catalogItem.getCategory().equals(Category.STAFF)
						|| !Inventory.infinity.contains(catalogItem.getCategory())) &&
				(inventoryItem.get().getQuantity().isLessThan(checkItem.getQuantity()))) {
			return inventoryItem;
		}
		return Optional.empty();
	}

	/**
   * assigns {@link Employee} to order when order lines contain STAFF-categorized-items:</br>
   * adds order to booked orders of {@link Employee}.</br>
   * adds {@link Employee} to order employees.
   * @param order {@link EventOrder}
   * @param personalLeft {@link List}
   */
	private void setPersonalBooked(EventOrder order, Map<Employee.Role, List<Employee>> personalLeft) {
		Item item;
		Quantity itemQuantity;

		for (OrderLine orderline : order.getOrderLines()) {
			Optional<Item> itemOptional = catalog.findById(orderline.getProductIdentifier());
			if (itemOptional.isEmpty()) {
				continue;
			}
			item = itemOptional.get();
			itemQuantity = orderline.getQuantity();

			if (item.getCategory().equals(Item.Category.STAFF)) {
				List<Employee> freeStaff = personalLeft.get(((StaffItem) item).getType());
				setPersonalBookedPart(itemQuantity, freeStaff, order);

			}
		}
	}

	private void setPersonalBookedPart(Quantity itemQuantity, List<Employee> freeStaff,
									   EventOrder order){
		for (int i = 0; i < itemQuantity.getAmount().intValue(); i++) {
			if (!freeStaff.isEmpty()) {
				Employee employee = freeStaff.remove(0);
				employee.setBooked(order);
				order.addEmployee(employee);
			}
		}
	}

	/**
	 * creates a mobile breakfast order.</br>
	 * <ul>
	 * <li> adds chosen breakfast items as {@link OrderLine} to a {@link MBOrder}</li>
	 * <li> adds mobile breakfast total prize as {@link org.salespointframework.order.ChargeLine} to a {@link MBOrder}</li>
	 * </ul>
	 * @param bfCartItem a @{@link CartItem} which contains a {@link BreakfastMappedItems}
	 * @param startDate start date of mobile breakfast
	 * @param endDate end date of mobile breakfast
	 * @param form {@link CheckoutForm}
	 * @param account {@link UserAccount}
	 * @return {@link MBOrder}
	 */
	private MBOrder createOrderMB(CartItem bfCartItem, LocalDateTime startDate, LocalDateTime endDate,
								  CheckoutForm form, UserAccount account) {

		BreakfastMappedItems bfItem = (BreakfastMappedItems) bfCartItem.getProduct();
		Cart cart = new Cart();
		for (CartItem checkItem : createCheckItems(bfCartItem)) {
			cart.addOrUpdateItem(checkItem.getProduct(), checkItem.getQuantity());
		}

		MBOrder order = new MBOrder(account, createPayMethod(form.getPayMethod(), account), startDate, endDate, bfItem);

		order.addChargeLine(bfCartItem.getPrice(), "mobile breakfast total");

		cart.addItemsTo(order);
		return order;

	}
	private void updateValidations(Map<Item.Domain, List<String>> validations, Item.Domain domain, String state) {
		if (validations.containsKey(domain)) {
			validations.get(domain).add(state);
		} else {
			List<String> stateList = new ArrayList<>();
			stateList.add(state);
			validations.put(domain, stateList);
		}
	}

	/**
   * sums up every needed items of every (completed) {@link MampfOrder} for a given timespan 
   * @param fromDate timespan start
   * @param toDate timespan end
   * @return a new instance of {@link Map} which maps all needed items as {@link ProductIdentifier} and {@link Quantity}
   */
	private Map<ProductIdentifier, Quantity> getOrderItems(LocalDateTime fromDate, LocalDateTime toDate) {

		Map<ProductIdentifier, Quantity> res = new HashMap<>();
		orderManagement.findBy(OrderStatus.COMPLETED).forEach(order -> order.getItems(fromDate, toDate).
				// some typical map updating:
						forEach((id, q) -> {
					if (res.containsKey(id)) {
						res.get(id).add(q);
					} else {
						res.put(id, q);
					}
				}));
		return res;
	}

}
