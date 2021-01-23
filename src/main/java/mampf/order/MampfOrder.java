package mampf.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.MappedSuperclass;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.quantity.Quantity;

import mampf.Util;
import mampf.catalog.Item;
import mampf.employee.Employee;
/**
 * base class for orders </br>
 * is a {@link Order}
 * 
 * @author Konstii
 */
@MappedSuperclass
public abstract class MampfOrder extends Order implements Comparable<MampfOrder>{
	
	//basic components of a order:
	private Item.Domain domain;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private String adress;
	public MampfOrder() {}
	public MampfOrder(UserAccount account, 
					  PaymentMethod paymentMethod,
					  Item.Domain domain, 
					  LocalDateTime startDate, 
					  LocalDateTime endDate, 
					  String adress) {
		super(account, paymentMethod);
		this.domain = domain;
		this.startDate = startDate;
		this.endDate = endDate;
		this.adress = adress;
	}
	
	public static final Comparator<MampfOrder> comparatorSortByCreation = (o1,o2)->o2.getDateCreated().compareTo(o1.getDateCreated());
	/**
	 * returns if the given timespans overlap:
	 * startDate > otherStartDate,...
	 * 
	 * @param startDate timespan1 start
	 * @param endDate timespan1 end
	 * @param orderStartDate timespan2 start
	 * @param orderEndDate timespan2 end
	 * @return
	 */
	public static boolean hasTimeOverlap(LocalDateTime startDate, LocalDateTime endDate,
										 LocalDateTime orderStartDate, LocalDateTime orderEndDate) {
		return endDate.isAfter(orderStartDate)&& (orderEndDate.isAfter(startDate));
	}
	
	/**
	 * returns actually needed and existing catalog-items of a order for a given timespan.</br>
	 * <li>can return a {@code empty} {@link Map} if the given timespan does not overlap with the timespan of this order.</li>
	 * <li>can return every {@link OrderLine} of this order if the given timespan totally overlaps with the timespan of this order.</li>
	 * @param fromDate timespan start
	 * @param toDate timespan end
	 * @return a new instance of {@link Map} of {@link ProductIdentifier} and {@link Quantity}
	 */
	abstract Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate);
	
	abstract String getDescription();
	
	public boolean hasTimeOverlap(LocalDateTime startDate, LocalDateTime endDate) {
		return hasTimeOverlap(startDate,endDate,getStartDate(),getEndDate());
	}
	
	public LocalDateTime getStartDate(){
		return startDate;
	}
	public LocalDateTime getEndDate() {
		return endDate;
	}
	public String getAdress() {
		return adress;
	}
	public Item.Domain getDomain(){
		return domain;
	}
	
	public List<Employee> getEmployees(){
		return new ArrayList<>();
	}
	
	/**
	 * creates a {@link Map} with added data about the orders {@link PaymentMethod}
	 * @return a new instance of {@link Map} of {@link String} and {@link String}
	 */
	public Map<String,String> getPayMethod() {
		PaymentMethod paymentMethod = getPaymentMethod();
		Map<String, String> allData = new HashMap<>();
		allData.put("Zahlende*r", getUserAccount().getUsername());
		if (paymentMethod instanceof Cheque) {
			Cheque cheque = (Cheque) paymentMethod;
			allData.put("Zahlungsempfänger",cheque.getBankName());
			allData.put("Anschrift",cheque.getBankAddress());
			allData.put("IBAN",cheque.getBankIdentificationNumber());
    }
		if (paymentMethod instanceof Cash) {
		  allData.put("Bezahlung","vor Ort");
		}
		return allData;
	}
	
	@Override 
	public String toString() {
		return "Bestellung: " + Util.renderDomainName(this.getDomain().name());
	}
	@Override
	public int compareTo(MampfOrder order) {
		assert order != null;
		return startDate.compareTo(order.getStartDate());
	}
}
