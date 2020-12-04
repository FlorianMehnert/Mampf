package mampf.order;

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

@Entity
public class MampfOrder extends Order {
	// TODO: addEmployee, findByCategory

	//private int personalNeeded = 0;
	
	//private boolean needsAllocation;

	@OneToOne(cascade = CascadeType.ALL)
	private MampfDate date;
	
	@ManyToMany(cascade = CascadeType.ALL)
	private List<Employee> employees;
	
	@SuppressWarnings("unused")
	private MampfOrder(){}
	public MampfOrder(UserAccount account, PaymentMethod paymentMethod, MampfDate date) {
		super(account, paymentMethod);
		this.date = date;
		employees = new ArrayList<>();
	}


	public void addEmployee(Employee employee) {
		//TODO: nullcheck
		employees.add(employee);
	}

	// public boolean getPersonalNeeded() {
	// 	return personalNeeded;
	// }
	public MampfDate getDate() {return date;}
	public List<Employee> getEmployees(){return employees;}
	
	//visuell:

	public String toString() {
		return "Order: "+this.getDate().toString();
	}
	
	public String getPayMethod(){
		PaymentMethod paymentMethod = super.getPaymentMethod(); 
		String res = "no payment";
		if(paymentMethod instanceof Cheque) { 
			Cheque cheque = ((Cheque)paymentMethod);
			res="CHECK:"+cheque.getBankName()+","+cheque.getAccountName()+","+cheque.getAccountNumber()+","
					+cheque.getBankAddress()+","+cheque.getBankIdentificationNumber();}
		if(paymentMethod instanceof Cash) {
			res = "CASH:";
		}
		return res;
	}
	//public boolean isDone() {
	//	if(done) return true;
	//	else return false;
	//}
}
