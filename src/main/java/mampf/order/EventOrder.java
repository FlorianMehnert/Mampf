package mampf.order;

import mampf.catalog.Item;

import mampf.catalog.StaffItem;

import mampf.employee.Employee;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.money.MonetaryAmount;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.OrderLine;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.quantity.Quantity;
import org.springframework.lang.NonNullApi;

@Entity
public class EventOrder extends MampfOrder {

	
	public static final Predicate<Product> productHasPrizePerHour = p->p instanceof StaffItem;
	
	public static MonetaryAmount calcPrizePerHour(LocalDateTime fromDate,
												  LocalDateTime toDate, 
												  MonetaryAmount prizePerHour) {
		long diff = ChronoUnit.MINUTES.between(fromDate, toDate);
		if(diff < 1) {
			return Money.of(0, "EUR");
		} 
		return prizePerHour.abs().multiply(Math.ceil(((double)diff)/ChronoUnit.HOURS.getDuration().toMinutes()));		
	}



	@ManyToMany(cascade = CascadeType.MERGE)
	private List<Employee> employees = new ArrayList<>();
	
	@ElementCollection(fetch = FetchType.EAGER)
	private List<ProductIdentifier> productsWithPrizePerHour = new ArrayList<>();
	
	@SuppressWarnings("unused")
	private EventOrder() {}
	public EventOrder(MampfCatalog catalog, UserAccount account,
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  LocalDateTime startDate,
					  LocalDateTime endDate,
					  String adress) {
		super(account, paymentMethod,domain,startDate,endDate,adress);

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
	public String getDescription() {
		return "Bestellung für ein Event";
	}
	
	
	public void addEmployee(Employee employee) {
		employees.add(employee);
	}

	@Override
	@ManyToMany(cascade = CascadeType.MERGE)
	public List<Employee> getEmployees() {
		return employees;
	}

	/*@Override
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
	}*/

	@Override 
	public MonetaryAmount getTotal() {
		Money total = Money.of(0, "EUR"); 
		for(OrderLine orderLine: getOrderLines()) {
			if(productsWithPrizePerHour.contains(orderLine.getProductIdentifier())) {
				total=total.add(
				EventOrder.calcPrizePerHour(getStartDate(), getEndDate(),
				orderLine.getPrice()	
				)); 
			}
			else{
				total=total.add(orderLine.getPrice());
			}
		}	
		
		return total;
	}
	
	@Override
	public OrderLine addOrderLine(Product product, Quantity quantity) {
		if(productHasPrizePerHour.test(product)) {
			productsWithPrizePerHour.add(product.getId());
		}
		return super.addOrderLine(product, quantity);
	}
	
	@Override
	public void remove(OrderLine orderLine) {
		if(productsWithPrizePerHour.contains(orderLine.getProductIdentifier())) {
			productsWithPrizePerHour.remove(orderLine.getProductIdentifier());
		}
		super.remove(orderLine);
	}
	
	/*@Override
	public String toString() {
		
		
		
		
		return ""+super.toString()
		return "Order: " + this.getDomain().toString();
	}*/
	
}



