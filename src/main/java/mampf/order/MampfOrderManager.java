package mampf.order;

import mampf.Util;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.LineItemFilter;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.OrderLine;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
	
	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, 
							 Inventory inventory,
							 EmployeeManagement employeeManagement,
							 UserManagement userManagement,
							 MampfCatalog catalog) {
		this.orderManagement = orderManagement;
		this.inventory = inventory;
		this.employeeManagement = employeeManagement;
		this.catalog = catalog;
		this.userManagement = userManagement;
	}
	
	/**
	 * Check if the user-(employee)s company has booked Mobile Breakfast
	 * returns false if not so
	 * @param userAccount
	 * @return
	 */
	public boolean hasBookedMB(UserAccount userAccount) {

		Optional<User> user = userManagement.findUserByUserAccount(userAccount.getId()); 
		if(user.isEmpty()) {
			return false;
		}
		Optional<Company> company = userManagement.findCompany(user.get().getId());
		if(company.isEmpty()) {
			return false;
		}
		return company.get().hasBreakfastDate();

	}
	/**
	 * Checks if requested items (cart items)/personal are available for the given time (form)
	 * checks if MB is in-time 
	 * returns list of validations (domainspec.), never null 
	 * @param carts
	 * @param form
	 * @return
	 */
	public Map<Item.Domain, List<String>> validateCarts(Map<Item.Domain, DomainCart> carts){
		//each domain can have mutliple errormessages:
		Map<Item.Domain, List<String>> validations = new EnumMap<>(Item.Domain.class);
		
		
		for(Map.Entry<Domain, DomainCart> entry : carts.entrySet()) {
			Domain domain = entry.getKey();
			DomainCart cart = entry.getValue();
			LocalDateTime startDate = cart.getStartDate(),endDate = cart.getEndDate();
			
			
			//check for MB 'you are too late'-error:
			if(domain.equals(Domain.MOBILE_BREAKFAST) && startDate.isBefore(LocalDateTime.now())) {
				updateValidations(validations, domain, "Zu späte MB Auswahl"); 
				continue;
			}
			
			//get Items:
			List<UniqueMampfItem> inventorySnapshot = getFreeItems(startDate, endDate);
			Map<Employee.Role,Integer> personalLeft = new HashMap<>();
			if(hasStaff(cart)) {
				personalLeft = getPersonalAmount(startDate, endDate);
			}
			
			for(CartItem cartitem: cart) {
				//de-map mapper-cartitems:
				for(CartItem checkitem: createCheckItems(cartitem)) {
					Optional<Item> catalogItem = catalog.findById(checkitem.getProduct().getId());
					
					if(catalogItem.isEmpty()) {
						updateValidations(validations, domain, "Item nicht vorhanden:"+cartitem.getProductName());
						continue;
					}
					
					Optional<UniqueMampfItem> inventoryItem = checkForAmount(inventorySnapshot, personalLeft,checkitem);
					if(inventoryItem.isPresent()){
						String validationState = "Keine verfügbare Auswahl "+
												 catalogItem.get().getCategory().name().toLowerCase()+": "+catalogItem.get().getName()+
												 " verbleibende Anzahl:"+inventoryItem.get().getQuantity().getAmount();
						updateValidations(validations, domain, validationState);
					}	
				}
			}	
		}
		return validations;
	}
	
	
	/**
	 * creates orders for the given items (cart items) and saves them in the SP orderManagement
	 * sets personal to the orders
	 * 
	 * @param carts
	 * @param form
	 * @param user
	 * @return
	 */
	public List<MampfOrder> createOrders(Map<Item.Domain, DomainCart> carts,  
										 CheckoutForm form,
										 User user) {
		
		List<MampfOrder> orders = new ArrayList<>();
		for(Map.Entry<Domain, DomainCart> entry : carts.entrySet()) {
			Domain domain = entry.getKey();
			DomainCart cart = entry.getValue();
			
			//get Dates:
			LocalDateTime startDate = cart.getStartDate(),endDate = cart.getEndDate();
			
			MampfOrder order;
			if(domain.equals(Domain.MOBILE_BREAKFAST)) {
				order = createOrderMB(cart.iterator().next(),startDate,endDate,form,user.getUserAccount());
				
			}else {
				//create usual order:
				order = new EventOrder(user.getUserAccount(),
						createPayMethod(form.getPayMethod(),user.getUserAccount()),
						domain,
						startDate,
						endDate,
						user.getAddress());
				
				cart.addItemsTo(order);
				if(hasStaff(cart)) {
					setPersonalBooked((EventOrder)order, getPersonal(startDate, endDate));
				}
			}
			
			
			//TODO: orderManagement.payOrder(order) how to manage errors??
			if(!orderManagement.payOrder(order)) {
				return orders;
			}
			
			orderManagement.completeOrder(order);
			
			orderManagement.save(order);
			orders.add(order);
		}
		
		return orders;
		
	}

	
	/**
	 * creates and returns a "inventory snapshot" (a list of UniqueMampfItems) which represents the inventory for a given time span 
	 * calculates the snapshot from the actual inventory
	 * checks every order for ordered items/amount
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public List<UniqueMampfItem> getFreeItems(LocalDateTime fromDate, LocalDateTime toDate) {
		
		List<UniqueMampfItem> res = new ArrayList<>(inventory.findAll().toList());
		for(UniqueMampfItem bookedItem: getBookedItems(fromDate, toDate)){
			int index = -1;
			UniqueMampfItem inventoryItem = null;
			for(int i=0; i < res.size(); i++) { 
				inventoryItem = res.get(i);
				
				if(inventoryItem.getProduct().equals(bookedItem.getProduct())) {
					index = i;
					break;
				}
			}
			if(index < 0) {
				continue;
			}
			
			//TODO: add reducable items instead of only just checking for infinity amount:			
			/* 
			 * -finite: 
			 *   per request(order) non restockable items (decoration)
			 * -infinite:
			 *   per request(order) restockable items (Food)
			 * maybe also:
			 * -reducable:
			 *   items which can be consumed (Food)
			 */
			//for finite items: set which quantity is left in stock:
			if(!Util.infinity.contains(inventoryItem.getCategory())){
				res.set(index, 
						new UniqueMampfItem((Item)inventoryItem.getProduct(), inventoryItem.getQuantity().subtract(bookedItem.getQuantity())));
			}
		}
		return res;
	}
	
	/**
	 * creates and returns a list of all ordered items for a time span
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	//TODO: find smaller version of getBookedItems functions
	public List<UniqueMampfItem> getBookedItems(LocalDateTime fromDate, LocalDateTime toDate){
		List<UniqueMampfItem> res = new ArrayList<>();
		for(Map.Entry<ProductIdentifier, Quantity> entry : getOrderItems(fromDate, toDate).entrySet()){
			Optional<Item> catalogItem = catalog.findById(entry.getKey());
			if(catalogItem.isEmpty())continue;
			res.add(new UniqueMampfItem(catalogItem.get(),entry.getValue()));
		}
		return res;
	}
	
	/**
	 * creates and returns a list of all ordered items for a time span
	 * @param itemMap
	 * @return
	 */
	public List<UniqueMampfItem> getBookedItems(Map<ProductIdentifier, Quantity> itemMap){
		List<UniqueMampfItem> res = new ArrayList<>();
			
		itemMap.forEach((id,q)->{
			Optional<Item> catalogItem = catalog.findById(id);
			if(catalogItem.isPresent())
			res.add(new UniqueMampfItem(catalogItem.get(),q));
		});
		return res;
	}
	
	/**
	 * creates and returns a list of every COMPLETED Order of a useraccount
	 * @param account
	 * @return
	 */
	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account)) {
			res.add(order);
		}
		return res;
	}
	/**
	 * deletes order
	 */
	public void deleteOrder(MampfOrder order) {
		for(MampfOrder order_: findAll()) {
			if(order.equals(order_)) {
				if(order_ instanceof EventOrder) {
					order.getEmployees().forEach(e->e.removeBookedOrder((EventOrder)order));
				}
				orderManagement.delete(order_);
				return;
			}

		}
	}
	public Optional<MampfOrder> findById(String orderId){
		return orderManagement.findAll(Pageable.unpaged()).filter(order->order.getId().getIdentifier().equals(orderId)).get().findFirst();
	}
	public List<MampfOrder> findAll() {
		return orderManagement.findBy(OrderStatus.COMPLETED).toList();
	}
	/**
	 * only unit testing purpose
	 * @return
	 */
	public OrderManagement<MampfOrder> getOrderManagement() {
		return orderManagement;
	}
	
	
	//TODO: createPayMethod needs to be updated
	private PaymentMethod createPayMethod(String payMethod,
										  UserAccount userAccount) {
		PaymentMethod method = Cash.CASH; 
		if(payMethod.equals("Check")) {
			method = new Cheque(userAccount.getUsername(),
								userAccount.getId().getIdentifier(),
								"checknummer 1",
								userAccount.getFirstname(),
								LocalDateTime.now(),
								"a bank","a banks address","a banks data");
		}
		return method;
	}
	
	private boolean hasStaff(Cart cart) {
		return(cart.get().anyMatch(cartitem -> 
				((Item)cartitem.getProduct()).getCategory().equals(Item.Category.STAFF)));
		
	}
	
	private Map<Employee.Role, Integer> getPersonalAmount(LocalDateTime startDate, LocalDateTime endDate){
		Map<Employee.Role, Integer> personalLeftSize = new HashMap<>();
		for(Map.Entry<Employee.Role, List<Employee>> entry : getPersonal(startDate,endDate).entrySet()) {
			personalLeftSize.put(entry.getKey(), entry.getValue().size());
		}
		return personalLeftSize;
	}
	
	private Map<Employee.Role, List<Employee>> getPersonal(LocalDateTime startDate, LocalDateTime endDate){
		Map<Employee.Role, List<Employee>> personalLeft = new HashMap<>();

		for(Employee.Role role: Employee.Role.values()) {
			List<Employee> xcy = employeeManagement.
					 getFreeEmployees(startDate,endDate,role);
			personalLeft.put(role, xcy);

		}
		return personalLeft;
	}
	
	private List<CartItem> createCheckItems(CartItem cartitem) {
		
		Item item = ((Item)cartitem.getProduct());
		List<CartItem> checkitems = new ArrayList<>();
		if(item.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)) {
			BreakfastMappedItems bfItem = ((BreakfastMappedItems)item);
			checkitems.add(new Cart().
						   addOrUpdateItem(bfItem.getBeverage(),
								    	   cartitem.getQuantity()));
			checkitems.add(new Cart().
						   addOrUpdateItem(bfItem.getDish(),
								   		   cartitem.getQuantity()));
		}else {
			checkitems.add(cartitem);
		}
		return checkitems;
	}
	
	//checkForAmount returns an empty Optional if the checked quantity is valid
	private Optional<UniqueMampfItem> checkForAmount(List<UniqueMampfItem> inventorySnapshot,
													 Map<Employee.Role,Integer> personalLeft,
									  				 CartItem checkitem) {
		
		Optional<UniqueMampfItem> inventoryItem =
				inventorySnapshot.stream().filter(
						i->i.getProduct().getId().equals(checkitem.getProduct().getId())).
				findFirst();
		Item catalogItem = ((Item)checkitem.getProduct());
		
		//finite (and existing)
		if(inventoryItem.isPresent() && !Util.infinity.contains(catalogItem.getCategory())) {
			
			if(catalogItem.getCategory().equals(Category.STAFF)) {
				//personal: there is one resource at all (therefore check and update personalLeft)
				
				Employee.Role staffType = ((StaffItem)catalogItem).getType();
				Integer amountLeft = personalLeft.get(staffType);
				//reduce till 0 if possible:
				if(checkitem.getQuantity().isGreaterThan(Quantity.of(amountLeft))) {
					return Optional.of(new UniqueMampfItem(catalogItem,Quantity.of(amountLeft)));
				}else {
					amountLeft -= checkitem.getQuantity().getAmount().intValue();
					personalLeft.put(staffType, amountLeft); 
				}
				
			}else { //sonarcube logic: 'else if {}' is allowed...
				if(inventoryItem.get().getQuantity().isLessThan(checkitem.getQuantity())) {
					return inventoryItem;
				}
			}
		}
		return Optional.empty();
	}
	
	private void setPersonalBooked(EventOrder order,
								   Map<Employee.Role, List<Employee>> personalLeft) {
		Item item;
		Quantity itemQuantity;
		
		for(OrderLine orderline: order.getOrderLines()) {
			Optional<Item> itemOptional = catalog.findById(orderline.getProductIdentifier());
			if(itemOptional.isEmpty()) {
				continue;
			}
			item = itemOptional.get();
			itemQuantity = orderline.getQuantity();
			
			if(item.getCategory().equals(Item.Category.STAFF)){
				List<Employee> freeStaff = personalLeft.get(((StaffItem)item).getType());
				Employee employee;
				for(int i = 0; i < itemQuantity.getAmount().intValue(); i++) {
					if(!freeStaff.isEmpty()){
						employee = freeStaff.remove(0);
						employee.setBooked(order);
						order.addEmployee(employee);
					}
				}
			}
		}
	}
	
	private MBOrder createOrderMB(CartItem bfCartItem, LocalDateTime startDate, LocalDateTime endDate, CheckoutForm form, UserAccount account) {
		
		BreakfastMappedItems bfItem = (BreakfastMappedItems)bfCartItem.getProduct();
		Cart cart = new Cart();
		for(CartItem checkItem:createCheckItems(bfCartItem)) {
			cart.addOrUpdateItem(checkItem.getProduct(), checkItem.getQuantity());
		}
		
		MBOrder order = new MBOrder(account,createPayMethod(form.getPayMethod(),account),startDate,endDate,bfItem);
		
		order.addChargeLine(bfCartItem.getPrice(), "mobile breakfast total");
		
		cart.addItemsTo(order);
		return order;
		
	} 
	
	private void updateValidations(Map<Item.Domain, List<String>> validations,
								   Item.Domain domain, 
								   String state) {
		if(validations.containsKey(domain)) {
			validations.get(domain).add(state);
		}else {
			List<String> stateList = new ArrayList<>();
			stateList.add(state);
			validations.put(domain, stateList);
		}
	}
	
	/*private LocalDateTime getDate(boolean needStartDate, Domain domain, DomainCart cart) {
		LocalDateTime date;
		if(domain.equals(Domain.MOBILE_BREAKFAST)) {
			//cart contains only one mapperItem: (otherwise the domain would not exist)
			CartItem bfCartItem = cart.iterator().next();
			BreakfastMappedItems bfItem = (BreakfastMappedItems)bfCartItem.getProduct();
			if(needStartDate) {
				date = bfItem.getStartDate();

			}else {
				date = bfItem.getEndDate();

			}
			
		}else {
			//form has the startdate and endDate (is static)
			if(needStartDate) {
				date = cart.getStartDate();
			}else {
				date = cart.getEndDate();
			}
		}

		return date;
	}*/
	
	//all ordered items for a time span
	private Map<ProductIdentifier, Quantity> getOrderItems(LocalDateTime fromDate, LocalDateTime toDate) {
		
		Map<ProductIdentifier, Quantity> res = new HashMap<>();
		orderManagement.findBy(OrderStatus.COMPLETED).forEach(
			order->	
			order.getItems(fromDate, toDate).
			//some typical map updating:
			forEach((id,q)->{if(res.containsKey(id)) res.get(id).add(q); else res.put(id, q);})
		);
		return res;
	}
	

}
