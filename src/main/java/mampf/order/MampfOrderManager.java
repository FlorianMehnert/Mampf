package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.MampfCatalog;


import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {

	private final OrderManagement<MampfOrder> orderManagement;
	private final Inventory inventory;
	private final MampfCatalog catalog;
	private final EmployeeManagement employeeManagement;
	
	
	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, Inventory inventory, EmployeeManagement employeeManagement, MampfCatalog catalog) {
		this.orderManagement = orderManagement;  this.inventory = inventory; this.employeeManagement = employeeManagement; this.catalog = catalog;
	}

	public Order save(MampfOrder order) {
		return orderManagement.save(order);
	}
	
	private PaymentMethod createPayMethod(String payMethod,UserAccount userAccount) {
		PaymentMethod method = Cash.CASH; //bydefault
		//if(payMethod.equals("Cash")) method = Cash.CASH; 
		if(payMethod.equals("Check")) 
			{method = new Cheque(userAccount.getUsername(),
								 userAccount.getId().getIdentifier(),
								 "checknummer 1",
								 userAccount.getFirstname(),
								 LocalDateTime.now(),
								 "a bank","a banks address","a banks data");}
		return method;
	}
	
	private List<Employee> getfreeEmployee(LocalDateTime startDate, Employee.Role role) {
		//init
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
	
	private int getEmployeeAmount(Cart cart, /*Employee.Role*/ StaffItem.Type type) {
		
		int employeeAmount = 0;
		
		Item item;
		
		for(CartItem cartItem: cart) {
			item = ((Item)cartItem.getProduct());
			
			//handle item amount: check after complete order
			//handle personal: 
			if(item.getCategory().equals(Item.Category.STAFF)) 
				{StaffItem staffItem = (StaffItem)item;
				if(staffItem.getType().equals(type))
					employeeAmount+= cartItem.getQuantity().getAmount().intValue();}
			

			/*switch(itemCategory) {
			case STAFF:
				//TODO: check if a item of its type IS a Personal - item
				StaffItem.Type personalType = ((StaffItem)item).getType();
				
				if(personalType.equals(StaffItem.Type.COOK))
					cookNeeded+= itemQuantity.getAmount().intValue();
				if(personalType.equals(StaffItem.Type.SERVICE))
					personalserviceNeeded+= itemQuantity.getAmount().intValue();
				break;
			case DECORATION:case EQUIPMENT:
				//TODO: situation wie personal möglich
				Optional<UniqueInventoryItem> optionalItem = inventory.findByProduct(item);
				if(optionalItem.isPresent()) 
					if(!optionalItem.get().hasSufficientQuantity(cartItem.getQuantity())) return null;
				break;
			}*/
		}
		return employeeAmount;
		//TODO

	}
	
	private void setEmloyees(Cart cart, MampfOrder order, List<Employee> freeCooks, List<Employee> freeServicePersonal) {

		Item item;
		Quantity itemQuantity;
		
		for(CartItem cartItem: cart) {
			item = ((Item)cartItem.getProduct());
			itemQuantity = cartItem.getQuantity();
			
			if(item.getCategory().equals(Item.Category.STAFF))
				{StaffItem personalItem = ((StaffItem)item);
				StaffItem.Type personalItemType = personalItem.getType();
				
				//TODO: find better solution:
				int personalAmount = itemQuantity.getAmount().intValue();
				Employee employee;
				for(int i = 0; i < personalAmount; i++) 
					{employee = null;
					if(personalItemType.equals(StaffItem.Type.COOK))employee = freeCooks.remove(0);
					if(personalItemType.equals(StaffItem.Type.SERVICE))employee = freeServicePersonal.remove(0);
					if(employee != null)
						{employee.setBooked(order);order.addEmployee(employee);}}
				}
			
			/*switch (item.getCategory()) {
			case STAFF:
				//TODO: check if really is personal
				StaffItem personalItem = ((StaffItem)item);
				StaffItem.Type personalItemType = personalItem.getType();
				
				//TODO: find better solution:
				int personalAmount = itemQuantity.getAmount().intValue();
				if(personalItemType.equals(StaffItem.Type.COOK))
					//while(personalAmount < 0){
					for(int i = 0; i < personalAmount; i++) {
					Employee dat = freeCooks.remove(0); dat.setBooked(order); order.addEmployee(dat);}
				if(personalItemType.equals(StaffItem.Type.SERVICE))
					//while(personalAmount < 0){
					for(int i = 0; i < personalAmount; i++) {	
					Employee dat = freeServicePersonal.remove(0); dat.setBooked(order); order.addEmployee(dat);}
				
				break;

			case EQUIPMENT: case DECORATION:
				//TODO: find better solution:
				inventory.reduceAmount((item), itemQuantity);
				break;
			}*/
			
		}
	}
	
	private MampfOrder createOrderMB(Cart cart,
									 DateFormular form, 
									 UserAccount account) {
		//creates special order for mobile breakfast
		
		//get mapper-item:
		CartItem cartitem = cart.get().findFirst().get();
		OrderController.BreakfastMappedItems item = 
				((OrderController.BreakfastMappedItems)cartitem.getProduct());
		//add choice:
		//TODO: cannot be removed from inventory
		cart.addOrUpdateItem(item.getForm().getBeverage(),Quantity.of(1));
		cart.addOrUpdateItem(item.getForm().getDish(),Quantity.of(0));
		
		
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

	public MampfOrder createOrder(Cart cart, 
								  DateFormular form, 
								  UserAccount userAccount) {
		//TODO: Forms optional 
		//validates Cart, 
		// creates Date
		// creates Order
		// pays Order (order should be added)
		// (if possible) completes order
		//reduces high amount of iteration-steps
		
	
		if(cart == null || userAccount == null)return null;
	
		
		Item.Domain orderDomain = ((Item)cart.iterator().next().getProduct()).getDomain();
		
		MampfOrder order;
		List<Employee> freeCooks = null, freeServicePersonal = null;
		
		if(orderDomain.equals(Item.Domain.MOBILE_BREAKFAST)) {
			order = createOrderMB(cart, form, userAccount);
			if(order == null)return null;
			
		}else {
			//create usual order:
			// create date with date and address:
			MampfDate orderDate = new MampfDate(form.getStartDate(), form.getAddress()); 
			order = new MampfOrder(userAccount,
								   createPayMethod(form.getPayMethod(),userAccount), 
								   orderDomain,
								   orderDate);
			orderDate.setOrder(order);
			cart.addItemsTo(order);
			
			
			LocalDateTime startDate = form.getStartDate();
			freeCooks = getfreeEmployee(startDate, Employee.Role.COOK); 
			freeServicePersonal = getfreeEmployee(startDate, Employee.Role.SERVICE);
			
			//TODO: check if order is really not existing when returning 
			if(freeCooks.size() < getEmployeeAmount(cart, StaffItem.Type.COOK) ||
					freeServicePersonal.size() < getEmployeeAmount(cart, StaffItem.Type.SERVICE)) return null;
		}
		
	
		if(!orderManagement.payOrder(order))return order; 
		

		//will (indirectly) reduce item amount if possible
		if(!orderDomain.equals(Item.Domain.MOBILE_BREAKFAST))orderManagement.completeOrder(order);
		
		
		orderManagement.save(order);
		if(!order.isCompleted())return null;
		
	//ADD VALIDATED PERSONAL:
	// there should never be a error
		
		if(!orderDomain.equals(Item.Domain.MOBILE_BREAKFAST)) {
			setEmloyees(cart, order, freeCooks, freeServicePersonal);
		}
		return order;
		
	}
	
	
	//public boolean payOrder(MampfOrder order) {
		
	//}


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

		// // new
	// public List<MampfOrder> findNewest(UserAccount account) {
	// 	List<MampfOrder> res = new ArrayList<>();
	// 	for (MampfOrder order : orderManagement.findBy(account))
	// 		if (order.isOpen())
	// 			res.add(order);
	// 	return res;
	// }

	public List<MampfOrder> findByUserAcc(UserAccount account) {
		List<MampfOrder> res = new ArrayList<>();
		for (MampfOrder order : orderManagement.findBy(account))
			res.add(order);
		return res;
	}

	// public List<MampfOrder> findByEmployee(Employee employee) {
	// 	List<MampfOrder> res = new ArrayList<>();
	// 	for (MampfOrder order : orderManagement.findBy(OrderStatus.PAID))
	// 		if (!order.isDone())
	// 			res.add(order);
	// 	return res;
	// }
}
