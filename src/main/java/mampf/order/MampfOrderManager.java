package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.OrderController.BreakfastMappedItems;

import mampf.user.User;
import org.salespointframework.order.OrderManagement;
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

import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.OrderLine;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {
	
	enum ValidationState{
		NO_PERSONAL,NO_STOCK,NO_ITEM
	}
	private final OrderManagement<MampfOrder> orderManagement;
	private final Inventory inventory;
	private final MampfCatalog catalog;
	private final EmployeeManagement employeeManagement;

	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, 
							 Inventory inventory,
							 EmployeeManagement employeeManagement,
							 MampfCatalog catalog) {
		this.orderManagement = orderManagement;
		this.inventory = inventory;
		this.employeeManagement = employeeManagement;
		this.catalog = catalog;
	}
	
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
	
	private boolean hasStaff(Collection<Cart> carts) {
		for(Cart cart: carts) {
			if(cart.get().
			   anyMatch(cartitem -> ((Item)cartitem.getProduct()).
					   				getCategory().
					   				equals(Item.Category.STAFF))) {
				return true;
			}
		}
		return false;
	}
	
	private Map<String, Integer> getPersonalAmount(CheckoutForm form){
		Map<String, Integer> personalLeftSize = new HashMap<>();
		for(Map.Entry<String, List<Employee>> entry : getPersonal(form).entrySet()) {
			personalLeftSize.put(entry.getKey(), entry.getValue().size());
		}
		return personalLeftSize;
	}
	
	private Map<String, List<Employee>> getPersonal(CheckoutForm form){
		Map<String, List<Employee>> personalLeft = new HashMap<>();
		for(Employee.Role role: Employee.Role.values()) {
			List<Employee> xcy = employeeManagement.
					 getFreeEmployees(
							 form.getStartDateTime(),
							 role);
			personalLeft.put(role.toString(), xcy);
		}
		return personalLeft;
	}
	
	private List<CartItem> createCheckItems(CartItem cartitem) {
		
		Item item = ((Item)cartitem.getProduct());
		List<CartItem> checkitems = new ArrayList<>();
		if(item.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)) {
			MobileBreakfastForm breakfastForm = ((BreakfastMappedItems)item).getForm();
			checkitems.add(new Cart().
						   addOrUpdateItem(breakfastForm.getBeverage(),
								    	   Quantity.of(1)));
			checkitems.add(new Cart().
						   addOrUpdateItem(breakfastForm.getDish(),
								   		   Quantity.of(1)));
		}else {
			checkitems.add(cartitem);
		}
		return checkitems;
	}
	
	private void checkForAmount(CartItem checkitem,
								Item catalogItem,
								Map<Item.Domain, List<ValidationState>> validations,
								Item.Domain domain) {
		
		Optional<UniqueMampfItem> inventoryItem =  inventory.findByProduct(catalogItem);
		if(inventoryItem.isPresent()) {
			Quantity inventoryItemQuantity = inventoryItem.get().getQuantity();
			
			//finite and not reducable amount:
			if(inventoryItemQuantity.isGreaterThan(Quantity.of(0))&&
					inventoryItemQuantity.isLessThan(checkitem.getQuantity())) {
				updateValidations(validations, domain, ValidationState.NO_STOCK);
			}
		}
	}
	
	private void setPersonalBooked(MampfOrder order,
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
	}
	
	private MampfOrder createOrderMB(CartItem cartitem,
									 CheckoutForm form,
									 UserAccount account) {
		//get mapper-item:
		BreakfastMappedItems item = (BreakfastMappedItems)cartitem.
														  getProduct();
		//add choice:
		Cart cart = new Cart();
		cart.addOrUpdateItem(item.getForm().getBeverage(),Quantity.of(1));
		cart.addOrUpdateItem(item.getForm().getDish(),Quantity.of(1));
		
		//create special date:
		// days and time
		MampfDate orderDate = createDateMB(item);
		MampfOrder order = new MampfOrder(account,
										  createPayMethod(form.getPayMethod(),
												  		  account),
										  Item.Domain.MOBILE_BREAKFAST,
										  orderDate);
		orderDate.setOrder(order);
		order.addChargeLine(item.getPrice(), "static prize for a breakfast");
		
		cart.addItemsTo(order);
		
		return order;

	}


	private void updateValidations(Map<Item.Domain, List<ValidationState>> validations,
								   Item.Domain domain, 
								   ValidationState state) {
		if(validations.containsKey(domain)) {
			validations.get(domain).add(state);
		}else {
			List<ValidationState> stateList = new ArrayList<>();
			stateList.add(state);
			validations.put(domain, stateList);
		}
	}
	
	public Map<Item.Domain, List<ValidationState>> validateCarts(Map<Item.Domain, Cart> carts,
														   CheckoutForm form){
		
		Map<Item.Domain, List<ValidationState>> validations = new EnumMap<>(Item.Domain.class);
		Map<String, Integer> personalLeft = null;
		if(hasStaff(carts.values())) {
			personalLeft = getPersonalAmount(form);
		}
		for(Map.Entry<Item.Domain, Cart> entry : carts.entrySet()) {
			Item.Domain domain = entry.getKey();
			Cart cart = entry.getValue();
			for(CartItem cartitem: cart) {
				for(CartItem checkitem: createCheckItems(cartitem)) {
					Optional<Item> catalogItem = catalog.findById(checkitem.getProduct().getId());
					
					if(!catalogItem.isPresent()) {
						updateValidations(validations, domain, ValidationState.NO_ITEM);
						continue;
					}
					
					checkForAmount(checkitem, catalogItem.get(),validations, domain);
					
					if(catalogItem.get().getCategory().equals(Item.Category.STAFF)) {
						String staffType = ((StaffItem)catalogItem.get()).getType().toString();
						
						Integer amountLeft = personalLeft.get(staffType);
						
						//reduce
						if(amountLeft > -1) {
							amountLeft -= checkitem.getQuantity().getAmount().intValue();
							personalLeft.put(staffType, amountLeft); 
						}
						//error
						if(amountLeft < 0){
							updateValidations(validations, domain, ValidationState.NO_PERSONAL);
						}
					}
					
				}
			}
		}
		
		return validations;
	}
	
	public List<MampfOrder> createOrders(Map<Item.Domain, Cart> carts,  
										 CheckoutForm form,
										 User user) {
		
		if(carts == null || user == null || form == null) {
			return new ArrayList<>(); //sonarcube
		}
		
		Map<String, List<Employee>> personalLeft = new HashMap<>(); //sonarcube
		if(hasStaff(carts.values())) {
			personalLeft = getPersonal(form);
		}
		
		
		List<MampfOrder> orders = new ArrayList<>();
		for(Map.Entry<Item.Domain, Cart> entry : carts.entrySet()) {
			
			Item.Domain domain = entry.getKey();
			Cart cart = entry.getValue();
			
			MampfOrder order;
			
			if(domain.equals(Item.Domain.MOBILE_BREAKFAST)) {
				order = createOrderMB(cart.iterator().next(), 
									  form,
									  user.getUserAccount());
				
			}else {
				//create usual order:
				// create date with date and address:
				MampfDate orderDate = new MampfDate(form.getStartDateTime(), user.getAddress());
				order = new MampfOrder(user.getUserAccount(),
									   createPayMethod(form.getPayMethod(),user.getUserAccount()),
									   domain,
									   orderDate);
				orderDate.setOrder(order);
				cart.addItemsTo(order);
			}
			
			setPersonalBooked(order, personalLeft);
			
			if(!orderManagement.payOrder(order))return orders; 
			
			orderManagement.completeOrder(order);
			
			orderManagement.save(order);
			orders.add(order);
		}
		
		return orders;
		
	}
	


	public MampfOrder findOrderById(UserAccount user){
		return this.orderManagement.findBy(user).get().collect(Collectors.toList()).get(0);
	}

	public OrderManagement<MampfOrder> getOrderManagement() {
		return orderManagement;
	}

	public List<MampfOrder> findAll() {
		Stream<MampfOrder> stream = orderManagement.findAll(Pageable.unpaged()).get();
		List<MampfOrder> list = stream.collect(Collectors.toList());
		return new ArrayList<>(list);
	}

	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account))
			res.add(order);
		return res;
	}

	

}
