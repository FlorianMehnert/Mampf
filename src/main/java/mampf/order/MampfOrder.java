package mampf.order;

import mampf.catalog.Item;
import mampf.employee.Employee;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.salespointframework.order.Order;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.data.annotation.Transient;

@Entity
public class MampfOrder extends Order {
	// TODO: addEmployee, findByCategory

	//private int personalNeeded = 0;
	
	//private boolean needsAllocation;
	
	private Item.Domain domain;
	
	//@Transient
	//private MobileBreakfastForm mobileBreakfastForm;
	
	@OneToOne(cascade = CascadeType.ALL)
	private MampfDate date;
	
	@ManyToMany(cascade = CascadeType.ALL)
	private List<Employee> employees;
	
	@SuppressWarnings("unused")
	private MampfOrder(){}
	public MampfOrder(UserAccount account, 
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  MampfDate date ) 
	{
		//TODO: create with nullable date or mbform
		super(account, paymentMethod);
		this.date = date;
		employees = new ArrayList<>();
		//TODO: make sure: every mb-domain has a non null form
		this.domain = domain;
		/*this.mobileBreakfastForm = mobileBreakfastForm;*/
	}


	public void addEmployee(Employee employee) {
		//TODO: nullcheck
		employees.add(employee);
	}
	
	// public boolean getPersonalNeeded() {
	// 	return personalNeeded;
	// }
	public Item.Domain getDomain(){return domain;}
	public MampfDate getDate() {return date;}
	public List<Employee> getEmployees(){return employees;}
	//@Transient
	//public MobileBreakfastForm getMobileBreakfastForm() {return mobileBreakfastForm;}
	
	//visuell:

	public String toString() {
		return "Order: "+this.getDomain().toString();
	}
	
	public String getPayMethod(){
		PaymentMethod paymentMethod = super.getPaymentMethod(); 
		String res = "no payment";
		if(paymentMethod instanceof Cheque) { 
			Cheque cheque = ((Cheque)paymentMethod);

			res="CHECK: Nutzer:"+cheque.getAccountName()+
					", Überweisung an: "+cheque.getBankName()+
					","+cheque.getBankAddress()+
					","+cheque.getBankIdentificationNumber();}
		
		if(paymentMethod instanceof Cash) res = "BAR";

		return res;
	}
	//public boolean isDone() {
	//	if(done) return true;
	//	else return false;
	//}
}
