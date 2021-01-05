package mampf.order;

import mampf.catalog.Item;
import mampf.employee.Employee;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.quantity.Quantity;

@Entity
public class EventOrder extends MampfOrder {
	
	public static final Duration EVENTDURATION = Duration.ofHours(1);
	
	public static LocalDateTime getEndDate(LocalDateTime startDate) {
		return startDate.plus(EVENTDURATION);
	}
	
	@ManyToMany(cascade = CascadeType.MERGE)
	private List<Employee> employees = new ArrayList<>();

	@SuppressWarnings("unused")
	private EventOrder() {}
	public EventOrder(UserAccount account,
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  LocalDateTime startDate,
					  String adress) {
		super(account, paymentMethod,domain,startDate,adress);
	}
	
	//impl.:
	Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate){
		//if colliding return everything:
		Map<ProductIdentifier,Quantity> res = new HashMap<>();
		if(hasTimeOverlap(fromDate, toDate)) {
			for(OrderLine orderLine: getOrderLines()) {
				res.put(orderLine.getProductIdentifier(),orderLine.getQuantity());
			}
		}
		return res;
	}
	//impl.:
	public LocalDateTime getEndDate() {
		return getEndDate(getStartDate());
	}
	//impl.:
	public String getDescription() {
		return "Bestellung für ein Event";
	}
	
	
	public void addEmployee(Employee employee) {
		employees.add(employee);
	}

	
	@ManyToMany(cascade = CascadeType.MERGE)
	public List<Employee> getEmployees() {
		return employees;
	}
	

	
	
	/*@Override
	public String toString() {
		
		
		
		
		return ""+super.toString()
		return "Order: " + this.getDomain().toString();
	}*/
	
}



