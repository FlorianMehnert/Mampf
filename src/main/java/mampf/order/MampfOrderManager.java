package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.catalog.Personal;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.user.User;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.InventoryItem;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderIdentifier;
import org.salespointframework.order.OrderLine;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
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

	public MampfOrder createOrder(Cart cart, DateFormular form, UserAccount userAccount) {
		//validates Cart, 
		// creates Date
		// creates Order
		// pays Order (order should be added)
		// (if possible) completes order
		//reduces high amount of iteration-steps
		
	//PREVALIDATE:
		if(cart == null || userAccount == null || cart == null)return null;
		
	//VALIDATE:
	// save free employees
		
		//init
		LocalDateTime startDate = form.getStartDate(),endDate = form.getEndDate();
				
		//get free employees by date:
		//MampfOrder bookedOrder;
		boolean isFree = true;
		List<Employee> freeCooks = new ArrayList<>(), freeServicePersonal = new ArrayList<>();
		//TODO: employeeManagement method?
		for(Employee employee: employeeManagement.findAll()) {
			isFree = true;
			for(MampfOrder bookedOrder : employee.getBooked())
				if(bookedOrder.getDate().hasTimeOverlap(startDate,endDate))
					{isFree = false; break;}
			
			if(isFree) {
				Employee.Role employeeRole = employee.getRole();
				if(employeeRole.equals(Employee.Role.COOK)) freeCooks.add(employee);
				if(employeeRole.equals(Employee.Role.SERVICE)) freeServicePersonal.add(employee);
			}
		}
		
		//check if valid cart:
		// - check stock (sufficient amount)
		// - check personal (needed amount)
		// fallthrough if valid
		
		//init
		Item item;
		Item.Category itemCategory;
		Quantity itemQuantity;
		//TODO: replace with foreach Type of Personal - list:
		int personalserviceNeeded = 0,cookNeeded = 0;
		
		for(CartItem cartItem: cart) {
			item = ((Item)cartItem.getProduct());
			itemQuantity = cartItem.getQuantity();
			itemCategory = item.getCategory();
			
			switch(itemCategory) {
			case PERSONEL:
				//TODO: check if a item of its type IS a Personal - item
				Personal.Type personalType = ((Personal)item).getType();
				
				if(personalType.equals(Personal.Type.COOK))
					cookNeeded+= itemQuantity.getAmount().intValue();
				if(personalType.equals(Personal.Type.SERVICE))
					personalserviceNeeded+= itemQuantity.getAmount().intValue();
				break;
			case DECORATION:case EQUIPMENT:
				//TODO: situation wie personal möglich
				Optional<UniqueInventoryItem> optionalItem = inventory.findByProduct(item);
				if(optionalItem.isPresent()) 
					if(!optionalItem.get().hasSufficientQuantity(cartItem.getQuantity())) return null;
				break;
			}
		}
		//validate personal:
		if(freeCooks.size() < cookNeeded ||
				freeServicePersonal.size() < personalserviceNeeded) return null;
		
		
	//CREATE:
		
		//valid stock and personal,
		MampfDate orderDate = new MampfDate(form.getStartDate(), form.getEndDate(), form.getAddress()); 
		MampfOrder order = new MampfOrder(userAccount, Cash.CASH, orderDate);
		orderDate.setOrder(order);
		cart.addItemsTo(order);
		
		orderManagement.save(order);
		if(!orderManagement.payOrder(order))return null; 
		
		//TODO: catch errors
		
		//reduce amount/ set (concrete) booked:
		for(CartItem cartItem: cart) {
			item = ((Item)cartItem.getProduct());
			itemQuantity = cartItem.getQuantity();
			
			switch (item.getCategory()) {
			case PERSONEL:
				//TODO: check if really is personal
				Personal personalItem = ((Personal)item);
				Personal.Type personalItemType = personalItem.getType();
				
				//TODO: find better solution:
				int personalAmount = itemQuantity.getAmount().intValue();
				if(personalItemType.equals(Personal.Type.COOK))
					//while(personalAmount < 0){
					for(int i = 0; i < personalAmount; i++) {
					Employee dat = freeCooks.remove(0); dat.setBooked(order); order.addEmployee(dat);/*personalAmount--;*/}
				if(personalItemType.equals(Personal.Type.SERVICE))
					//while(personalAmount < 0){
					for(int i = 0; i < personalAmount; i++) {	
					Employee dat = freeServicePersonal.remove(0); dat.setBooked(order); order.addEmployee(dat);/*personalAmount--;*/}
				
				break;

			case EQUIPMENT: case DECORATION:
				//TODO: find better solution:
				inventory.reduceAmount((item), itemQuantity);
				break;
			}
			
		}
		
	
	//EVERYTHING WORKED FINE:
		orderManagement.save(order);
		cart.clear();
		//orderManagement.completeOrder(order);
		
		return order;
		
	}
	
	
	//public boolean payOrder(MampfOrder order) {
		
	//}

	public void completeOrder(MampfOrder order) {
		orderManagement.completeOrder(order);
	}


	//public boolean validateCart(Cart cart, DateFormular form) {
		
		
	//}
	
	// public void addEmployee(MampfOrder order, Employee employee) {
	// 	order.addEmployee(employee);
	// 	// if (order.isDone())
	// 	// 	orderManagement.completeOrder(order);

	// }

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
