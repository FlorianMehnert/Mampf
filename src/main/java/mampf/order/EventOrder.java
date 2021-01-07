package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.money.MonetaryAmount;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.quantity.Quantity;
import org.springframework.lang.NonNullApi;

@Entity
public class EventOrder extends MampfOrder {
	private LocalDateTime endDateTime;
	private static MampfCatalog catalog;


	@ManyToMany(cascade = CascadeType.MERGE)
	private List<Employee> employees = new ArrayList<>();

	@SuppressWarnings("unused")
	private EventOrder() {}
	public EventOrder(MampfCatalog catalog, UserAccount account,
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  LocalDateTime startDate,
					  LocalDateTime endDateTime,
					  String adress) {
		super(account, paymentMethod,domain,startDate,adress);
		this.endDateTime = endDateTime;
		EventOrder.catalog = catalog;
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
		return endDateTime;
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


	@Override
	public MonetaryAmount getTotal() {
		MonetaryAmount total = Money.of(0, "EUR");
		for(OrderLine orderLine: getOrderLines()) {
			if(catalog.findById(orderLine.getProductIdentifier()).isPresent()) {
				Item item = catalog.findById(orderLine.getProductIdentifier()).get();
				if(item.getCategory().equals(Item.Category.STAFF)) {
					total = total.add(item.getPrice().multiply(eventDuration()).multiply(orderLine.getQuantity().getAmount()));
				}else{
					total = total.add(item.getPrice()).multiply(orderLine.getQuantity().getAmount());
				}
			}
		}
		return total.add(getAllChargeLines().getTotal());
	}

	private int eventDuration() {
		return endDateTime.toLocalTime().minusHours(getStartDate().getHour()).getHour();
	}
	
	
	/*@Override
	public String toString() {
		
		
		
		
		return ""+super.toString()
		return "Order: " + this.getDomain().toString();
	}*/
	
}



