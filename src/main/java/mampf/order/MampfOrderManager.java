package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.order.OrderController.BreakfastMappedItems;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;

import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {
	
	public static enum ValidationState{
		NO_PERSONAL,NO_STOCK,NO_ITEM
	};
	private final OrderManagement<MampfOrder> orderManagement;
	private final Inventory inventory;
	private final MampfCatalog catalog;
	private final EmployeeManagement employeeManagement;

	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, Inventory inventory,
							 EmployeeManagement employeeManagement, MampfCatalog catalog) {
		this.orderManagement = orderManagement;
		this.inventory = inventory;
		this.employeeManagement = employeeManagement;
		this.catalog = catalog;

	}
	
	private PaymentMethod createPayMethod(String payMethod,UserAccount userAccount) {
		PaymentMethod method = Cash.CASH; //bydefault
		//if(payMethod.equals("Cash")) method = Cash.CASH; 
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
	
	private List<Employee> getfreeEmployee(LocalDateTime startDate, Employee.Role role) {
	
		//get free employees by date:
		//MampfOrder bookedOrder;
		boolean isFree = true;
		List<Employee> freeEmployee = new ArrayList<>();
		//TODO: employeeManagement method?
		for(Employee employee: employeeManagement.findAll()) {
			isFree = true;
			for(MampfOrder bookedOrder : employee.getBooked())
				if(bookedOrder.getDate().hasTimeOverlap(startDate))
					{isFree = false; break;}
			
			if(isFree) 
				if(employee.getRole().equals(role)) freeEmployee.add(employee);
			
		}
		return freeEmployee;
	}
	
	private MampfOrder createOrderMB(Cart cart,
									 CheckoutForm form,
									 UserAccount account) {
		//creates special order for mobile breakfast
		
		//get mapper-item:
		CartItem cartitem = cart.get().findFirst().get();
		OrderController.BreakfastMappedItems item = 
				((OrderController.BreakfastMappedItems)cartitem.getProduct());
		//add choice:
		//TODO: cannot be removed from inventory
		cart.addOrUpdateItem(item.getForm().getBeverage(),Quantity.of(1));
		cart.addOrUpdateItem(item.getForm().getDish(),Quantity.of(1));
		
		
		//create special date:
		// days and time
		MampfDate orderDate = 
				new MampfDate(item.getForm().getDays().
							  keySet().stream().filter(weekday -> 
							  		item.getForm().getDays().get(weekday).booleanValue()).toArray(String[]::new),
							  item.getForm().getTime()); 
		
		MampfOrder order = new MampfOrder(account,
										  createPayMethod(form.getPayMethod(),
												  account),
										  Item.Domain.MOBILE_BREAKFAST,
										  orderDate);
		orderDate.setOrder(order);
		//add prize as chargeline
		order.addChargeLine(item.getPrice(), "static prize for a breakfast");
		//remove mapper-item:
		cart.removeItem(cartitem.getId());
		
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
		
		Map<Item.Domain, List<ValidationState>> validations = new HashMap<>();
		
		Map<String, Integer> personalLeft = null;
		
		for(Item.Domain domain: carts.keySet()) {
			for(CartItem cartitem: carts.get(domain)) {
				if(((Item)cartitem.getProduct()).getCategory().equals(Item.Category.STAFF)) {
					personalLeft = new HashMap<>();
					for(Employee.Role role: Employee.Role.values()) {
						personalLeft.put(role.toString(), 
										 Integer.valueOf(getfreeEmployee
												 (form.getStartDateTime(), role).
												 size()));
					}
					break;
				}
			}
		}
		
		
		for(Item.Domain domain: carts.keySet()) {
			Cart cart = carts.get(domain);
			
			for(CartItem cartitem: cart) {
				List<CartItem> checkitems = new ArrayList<>();
				Item item = (Item)cartitem.getProduct();
				
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
					
				for(CartItem checkitem: checkitems) {
					Optional<Item> catalogItem = catalog.findById(checkitem.getProduct().getId());
					
					if(catalogItem.isPresent()) {
						//are there other 'items' like personal??

						Optional<UniqueInventoryItem> inventoryItem =  
								inventory.findByProduct(catalogItem.get());
						if(inventoryItem.isPresent()) {
							Quantity inventoryItemQuantity = inventoryItem.get().getQuantity();
							//replace with function call?
							//finite and not reducable amount:
							if(inventoryItemQuantity.isGreaterThan(Quantity.of(0))&&
									inventoryItemQuantity.isLessThan(checkitem.getQuantity())) {
								updateValidations(validations, domain, ValidationState.NO_STOCK);
							}
						}
						
						if(catalogItem.get().getCategory().
								equals(Item.Category.STAFF)) {
							String staffType = ((StaffItem)item).getType().toString();
							
							Integer amountLeft = personalLeft.get(staffType);
							
							//reduce
							if(amountLeft > 0) {
								amountLeft -= checkitem.getQuantity().getAmount().intValue();
								personalLeft.put(staffType, amountLeft); //?
							}
							//error
							if(amountLeft < 0){
								updateValidations(validations, domain, ValidationState.NO_PERSONAL);
							}
						}
					}else {
						//item is not in the catalog
						updateValidations(validations, domain, ValidationState.NO_ITEM);
					}
				}
			}
		}
		
		return validations;
	}
	
	public List<MampfOrder> createOrders(Map<Item.Domain, Cart> carts,  
										 CheckoutForm form,
										 UserAccount userAccount) {
		
		if(carts == null || userAccount == null || form == null)return null;
		
		Map<String, List<Employee>> personalLeft = null;
		for(Item.Domain domain: carts.keySet()) {
			for(CartItem cartitem: carts.get(domain)) {
				if(((Item)cartitem.getProduct()).getCategory().equals(Item.Category.STAFF)) {
					personalLeft = new HashMap<>();
					for(Employee.Role role: Employee.Role.values()) {
						personalLeft.put(role.toString(), 
										 getfreeEmployee(form.getStartDateTime(), role));
					}
					break;
				}
			}
		}
		
		
		List<MampfOrder> orders = new ArrayList<>();
		for(Item.Domain domain: carts.keySet()) {
			
			Cart cart = carts.get(domain);
			
			MampfOrder order;
			
			if(domain.equals(Item.Domain.MOBILE_BREAKFAST)) {
				order = createOrderMB(cart, form, userAccount);
				
			}else {
				//create usual order:
				// create date with date and address:
				MampfDate orderDate = new MampfDate(form.getStartDateTime(), form.getAddress());
				order = new MampfOrder(userAccount,
									   createPayMethod(form.getPayMethod(),userAccount), 
									   domain,
									   orderDate);
				orderDate.setOrder(order);
				cart.addItemsTo(order);
			}
			
			//set personal as booked:
		
			Item item;
			Quantity itemQuantity;
			
			for(CartItem cartItem: cart) {
				item = ((Item)cartItem.getProduct());
				itemQuantity = cartItem.getQuantity();
				
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
		
			if(!orderManagement.payOrder(order))return null; 
			
	
			//will (indirectly) reduce item amount if possible
			//mb orders throws error
			if(!domain.equals(Item.Domain.MOBILE_BREAKFAST))orderManagement.completeOrder(order);
			
			orderManagement.save(order);
			if(!order.isCompleted())return null;
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

	public ArrayList<MampfOrder> findAll() {
		Stream<MampfOrder> stream = orderManagement.findAll(Pageable.unpaged()).get();
		List<MampfOrder> list = stream.collect(Collectors.toList());
		return new ArrayList<MampfOrder>(list);
	}

	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account))
			res.add(order);
		return res;
	}

	

}
