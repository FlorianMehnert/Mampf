package mampf.order;

import mampf.catalog.Item;

import mampf.catalog.StaffItem;

import mampf.employee.Employee;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
/**
 * a {@link MampfOrder} class, which represents every order which can have:
 * assigned {@link Employee}
 * 
 *
 */
@Entity
public class EventOrder extends MampfOrder {

	
	public static final Predicate<Product> productHasPrizePerHour = p->p instanceof StaffItem;
	
	/**
	 * calculates the prize for the given timespan for a given prizePerHour. </br>
	 * the timespan (minute differenc) will always be rounded up to the next hour.
	 * @param fromDate timespan start
	 * @param toDate timespan end
	 * @param prizePerHour a {@link MonetaryAmount} for each hour
	 * @return the total prize, {@link Money} of {@literal zero} will be returned when the timespan is negativ.
	 */
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
	private List<String> productsWithPrizePerHour = new ArrayList<>();
	
	@SuppressWarnings("unused")
	private EventOrder() {}
	public EventOrder(UserAccount account,
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  LocalDateTime startDate,
					  LocalDateTime endDate,
					  String adress) {
		super(account, paymentMethod,domain,startDate,endDate,adress);

	}

	public int durationOfEvent() {
		return this.getEndDate().minusHours(this.getStartDate().toLocalTime().getHour()).getHour();
	}
	
	/**
	 * returns every {@link OrderLine} when the given timespan is overlapping with the timespan of this order.
	 */
	public Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate){
		
		Map<ProductIdentifier,Quantity> res = new HashMap<>();
		if(hasTimeOverlap(fromDate, toDate)) {
			for(OrderLine orderLine: getOrderLines()) {
				res.put(orderLine.getProductIdentifier(),orderLine.getQuantity());
			}
		}
		return res;
	}
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

	/**
	 * {@link OrderLine} items can have a prize per hour.</br>
	 * calculates and returns the sum of each total prize of each {@link OrderLine} for the timespan of this order.
	 */
	@Override 
	public MonetaryAmount getTotal() {
		Money total = Money.of(0, "EUR"); 
		for(OrderLine orderLine: getOrderLines()) {
			if(productsWithPrizePerHour.contains(orderLine.getProductIdentifier().getIdentifier())) {
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
	/**
	 * creates and returns every {@link OrderLine} of this order mapped to its total prize of this order.
	 * @return a new instance of {@link Map} of {@link OrderLine} and {@link MonetaryAmount}
	 */
	public Map<OrderLine, MonetaryAmount> getItems(){
		Map<OrderLine, MonetaryAmount> stuff = new HashMap<>();
		Iterator<OrderLine> it = getOrderLines().iterator();
		while(it.hasNext()) {
			OrderLine orderLine = it.next();
			Money price;
			if(productsWithPrizePerHour.contains(orderLine.getProductIdentifier().getIdentifier())) {
				price=(Money)EventOrder.calcPrizePerHour(getStartDate(), getEndDate(),orderLine.getPrice());
			}
			else{
				price=(Money)orderLine.getPrice();
			}
			stuff.put(orderLine,price);
		}	
		return stuff;
	}
	
	@Override
	public OrderLine addOrderLine(Product product, Quantity quantity) {
		if(productHasPrizePerHour.test(product) &&
		   !productsWithPrizePerHour.contains(product.getId().getIdentifier())) {    
		    productsWithPrizePerHour.add(product.getId().getIdentifier());
		}
		return super.addOrderLine(product, quantity);
	}
	
	@Override
	public void remove(OrderLine orderLine) {
		if(productsWithPrizePerHour.contains(orderLine.getProductIdentifier().getIdentifier())) {
			productsWithPrizePerHour.remove(orderLine.getProductIdentifier().getIdentifier());
		}
		super.remove(orderLine);
	}
	
}



