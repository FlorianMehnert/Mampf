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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.LineItemFilter;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {
	
	//config class??
	@Bean
	LineItemFilter filter() {
	return item -> false;
	}
	
	enum ValidationState{
		NO_PERSONAL,NO_STOCK,NO_ITEM
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
	
	private Map<String, Integer> getPersonalAmount(LocalDateTime startDate, LocalDateTime endDate){
		Map<String, Integer> personalLeftSize = new HashMap<>();
		for(Map.Entry<String, List<Employee>> entry : getPersonal(startDate,endDate).entrySet()) {
			personalLeftSize.put(entry.getKey(), entry.getValue().size());
		}
		return personalLeftSize;
	}
	
	private Map<String, List<Employee>> getPersonal(LocalDateTime startDate, LocalDateTime endDate){
		Map<String, List<Employee>> personalLeft = new HashMap<>();
		for(Employee.Role role: Employee.Role.values()) {
			List<Employee> xcy = employeeManagement.
					 getFreeEmployees(startDate,endDate,role);
			personalLeft.put(role.toString(), xcy);
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
													 Map<String,Integer> personalLeft,
									  				 CartItem checkitem) {
		
		Optional<UniqueMampfItem> inventoryItem =
				inventorySnapshot.stream().filter(
						i->i.getProduct().getId().equals(checkitem.getProduct().getId())).
				findFirst();
		Item catalogItem = ((Item)checkitem.getProduct());
		
		if(inventoryItem.isPresent()) {
			
			//finite:
			if(!Util.infinity.contains(catalogItem.getCategory())) {
				
				if(catalogItem.getCategory().equals(Category.STAFF)) {
					//personal: there is one resource at all (therefore check and update personalLeft)
					
					String staffType = ((StaffItem)catalogItem).getType().toString();
					Integer amountLeft = personalLeft.get(staffType);
					
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
		}
		return Optional.empty();
	}
	
	private void setPersonalBooked(EventOrder order,
								   Map<String, List<Employee>> personalLeft) {
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
				StaffItem personalItem = ((StaffItem)item);
				String personalItemType = personalItem.getType().toString();
				
				List<Employee> freeStaff = personalLeft.get(personalItemType);
				
				int personalAmount = itemQuantity.getAmount().intValue();
				Employee employee;
				for(int i = 0; i < personalAmount; i++) {
					if(!freeStaff.isEmpty()){
						employee = freeStaff.remove(0);
						employee.setBooked(order);
						order.addEmployee(employee);
					}
				}
			}
		}
	}
	
	
	/*
	private MampfDate createDateMB(OrderController.BreakfastMappedItems item) {
		return new MampfDate(item.
							 getForm().
							 getDays().
				  			 keySet().
				  			 stream().
				  			 filter(weekday -> item.
				  					 		   getForm().
				  					 		   getDays().
				  					 		   get(weekday).
				  					 		   booleanValue()).
				  			 		toArray(String[]::new),
				  			 item.
				  			 getForm().
				  			 getTime());
	}*/
	
	private MBOrder createOrderMB(CartItem cartitem,
								  UserAccount account,
								  String address,
								  String payment) {
		//get mapper-item:
		BreakfastMappedItems bfItem = (BreakfastMappedItems)cartitem.
														  getProduct();
		//add choice:
		//Cart cart = new Cart();
		//cart.addOrUpdateItem(item.getBeverage(),Quantity.of(1));
		//cart.addOrUpdateItem(item.getDish(),Quantity.of(1));
		Cart cart = new Cart();
		for(CartItem checkItem:createCheckItems(cartitem)) {
			cart.addOrUpdateItem(checkItem.getProduct(), checkItem.getQuantity());
		}
		
		MBOrder order = new MBOrder(account,
									createPayMethod(payment,account),
									bfItem,address);
		
		order.addChargeLine(cartitem.getPrice(), "mobile breakfast total");
		
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
	
	//reason why not in userManagemenet: create MBI and validation both need function
	public boolean hasBookedMB(UserAccount userAccount) {

		Optional<User> user = userManagement.findUserByUserAccount(userAccount.getId()); 
		if(user.isEmpty()) {
			return false;
		}
		Optional<Company> company = userManagement.findCompany(user.get().getId());
		if(company.isEmpty()) {
			return false;
		}
		return company.get().hasbreakfastDate();

	}
	
	private LocalDateTime getDate(boolean needStartDate, Domain domain, Cart cart, CheckoutForm form) {
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
				date = form.getStartDateTime();
			}else {
				date = EventOrder.getEndDate(form.getStartDateTime());
			}
			
			
		}
		return date;
	}
	
	public Map<Item.Domain, List<String>> validateCarts(Map<Item.Domain, Cart> carts,
														Map<Item.Domain, CheckoutForm> forms){
		
		//each domain can have mutliple errormessages:
		Map<Item.Domain, List<String>> validations = new EnumMap<>(Item.Domain.class);
		
		
		for(Item.Domain domain : carts.keySet()) {
			Cart cart = carts.get(domain);
			CheckoutForm form = forms.get(domain);
			
			//get Dates:
			LocalDateTime startDate = getDate(true,domain,cart,form),
						  endDate = getDate(false,domain,cart,form);
			
			
			List<UniqueMampfItem> inventorySnapshot = getFreeItems(startDate, endDate);
			Map<String,Integer> personalLeft = new HashMap<>();
			if(hasStaff(cart)) {
				personalLeft = getPersonalAmount(startDate, endDate);
			}
			for(CartItem cartitem: cart) {
				//de-map mapper-cartitems:
				for(CartItem checkitem: createCheckItems(cartitem)) {
					Optional<Item> catalogItem = catalog.findById(checkitem.getProduct().getId());
					
					if(catalogItem.isEmpty()) {
						updateValidations(validations, domain, "could not find item:"+cartitem.getProductName());
						continue;
					}
					
					Optional<UniqueMampfItem> inventoryItem = checkForAmount(inventorySnapshot, personalLeft,checkitem);
					if(inventoryItem.isPresent()){
						String validationState = "No amount! "+
												 catalogItem.get().getCategory().name().toLowerCase()+
												 " Amount left:"+inventoryItem.get().getQuantity().getAmount();
						updateValidations(validations, domain, validationState);
					}	
				}
			}	
		}
		return validations;
	}
	
	//CONTINUE
	public List<MampfOrder> createOrders(Map<Item.Domain, Cart> carts,  
										 Map<Item.Domain, CheckoutForm> forms,
										 String payment,
										 User user) {
		
		List<MampfOrder> orders = new ArrayList<>();
		for(Item.Domain domain : carts.keySet()) {
			Cart cart = carts.get(domain);
			CheckoutForm form = forms.get(domain);
			
			//get Dates:
			LocalDateTime startDate = getDate(true,domain,cart,form),
						  endDate = getDate(false,domain,cart,form);
			
			
			Map<String,List<Employee>> personalLeft = new HashMap<>();
			if(hasStaff(cart)) {
				personalLeft = getPersonal(startDate, endDate);
			}
			
			MampfOrder order;
			if(domain.equals(Domain.MOBILE_BREAKFAST)) {
				order = createOrderMB(cart.iterator().next(), 
									  user.getUserAccount(),
									  form.getAdress(),
									  payment);
				
			}else {
				//create usual order:
				order = new EventOrder(user.getUserAccount(),
									   createPayMethod(payment,user.getUserAccount()),
									   domain,
									   startDate,form.getAdress());
				
				cart.addItemsTo(order);
				setPersonalBooked((EventOrder)order, personalLeft);
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


	public OrderManagement<MampfOrder> getOrderManagement() {
		return orderManagement;
	}

	public List<MampfOrder> findAll() {
		return orderManagement.findAll(Pageable.unpaged()).getContent();
	}

	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account)) {
			res.add(order);
		}
		return res;
	}

	//inventorysnapshot: (what is left)
	public List<UniqueMampfItem> getFreeItems(LocalDateTime fromDate, LocalDateTime toDate) {
		
		List<UniqueMampfItem> res = getBookedItems(fromDate, toDate);
		for(UniqueMampfItem bookedItem: res){
			Optional<UniqueMampfItem> inventoryItem = inventory.findByProductIdentifier(bookedItem.getItem().getId());
			if(inventoryItem.isEmpty())continue; 
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
			if(!Util.infinity.contains(inventoryItem.get().getCategory())){
				bookedItem.getQuantity().subtract(inventoryItem.get().getQuantity());
			}
		}
		return res;
	}
	
	//TODO: find smaller version of getBookedItems functions
	//get booked items of all orders
	public List<UniqueMampfItem> getBookedItems(LocalDateTime fromDate, LocalDateTime toDate){
		List<UniqueMampfItem> res = new ArrayList<>();
		for(Map.Entry<ProductIdentifier, Quantity> entry : getOrderItems(fromDate, toDate).entrySet()){
			Optional<Item> catalogItem = catalog.findById(entry.getKey());
			if(catalogItem.isEmpty())continue;
			res.add(new UniqueMampfItem(catalogItem.get(),entry.getValue()));
		}
		return res;
	}
	
	public List<UniqueMampfItem> getBookedItems(Map<ProductIdentifier, Quantity> itemMap){
		List<UniqueMampfItem> res = new ArrayList<>();
			
		itemMap.forEach((id,q)->{
			Optional<Item> catalogItem = catalog.findById(id);
			if(catalogItem.isPresent())
			res.add(new UniqueMampfItem(catalogItem.get(),q));
		});
		return res;
	}
	
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
