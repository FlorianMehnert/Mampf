package mampf.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.persistence.MappedSuperclass;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.quantity.Quantity;

import mampf.catalog.Item;
import mampf.inventory.UniqueMampfItem;

@MappedSuperclass
public abstract class MampfOrder extends Order implements Comparable<MampfOrder>{
	
	//basic components of a order:
	private Item.Domain domain;
	private LocalDateTime startDate;
	private String adress;
	
	@SuppressWarnings("unused")
	public MampfOrder() {}
	public MampfOrder(UserAccount account, 
					  PaymentMethod paymentMethod,
					  Item.Domain domain, 
					  LocalDateTime startDate, 
					  String adress) {
		super(account, paymentMethod);
		this.domain = domain;
		this.startDate = startDate;
		this.adress = adress;
	}
	
	public static boolean hasTimeOverlap(LocalDateTime startDate, LocalDateTime endDate,
										 LocalDateTime orderStartDate, LocalDateTime orderEndDate) {
		return endDate.isAfter(orderStartDate)&& (orderEndDate.isAfter(startDate));
	}
	
	//needed items for time span
	abstract Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate);

	
	abstract LocalDateTime getEndDate();
	
	
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
	LocalDateTime getStartDate(){
		return startDate;
	}
	String getAdress() {
		return adress;
	}
	Item.Domain getDomain(){
		return domain;
	}
	
	
	public String getPayMethod() {
		PaymentMethod paymentMethod = getPaymentMethod();
		String res = "no payment";
		if (paymentMethod instanceof Cheque) {
			Cheque cheque = ((Cheque) paymentMethod);

			res = "CHECK: Nutzer:" + cheque.getAccountName() + ", Überweisung an: " + cheque.getBankName() + ","
					+ cheque.getBankAddress() + "," + cheque.getBankIdentificationNumber();
		}

		if (paymentMethod instanceof Cash) {
			res = "BAR";
		}

		return res;
	}
	
	@Override 
	public String toString() {
		return "Order: " + this.getDomain().toString();
	}
	@Override
	public int compareTo(MampfOrder order) {
		assert order != null;
		return order.getStartDate().compareTo(startDate);
	}
}
