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

@MappedSuperclass
public abstract class MampfOrder extends Order implements Comparable<MampfOrder>{
	
	//basic components of a order:
	private Item.Domain domain;
	private LocalDateTime startDate,endDate;
	private String adress;
	
	@SuppressWarnings("unused")
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
	
	public static boolean hasTimeOverlap(LocalDateTime startDate, LocalDateTime endDate,
										 LocalDateTime orderStartDate, LocalDateTime orderEndDate) {
		return endDate.isAfter(orderStartDate)&& (orderEndDate.isAfter(startDate));
	}
	
	//needed items for time span
	abstract Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate);
	
	abstract String getDescription();
	
	public boolean hasTimeOverlap(LocalDateTime startDate, LocalDateTime endDate) {
		return hasTimeOverlap(startDate,endDate,getStartDate(),getEndDate());
	}
	
	/*@Override
	public boolean equals(MampfOrder d) {
		return d.getAdress().equals(adress) && 
			   d.getDomain().equals(domain) &&
			   d.getStartDate().equals(startDate); 
	}
	*/
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
