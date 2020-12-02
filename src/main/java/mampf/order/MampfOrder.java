package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.Item.Category;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.salespointframework.catalog.Product;
import org.salespointframework.core.AbstractEntity;
import org.salespointframework.order.Cart;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

@Entity
public class MampfOrder extends Order {
	// TODO:
	//addEmployee
	//findByCategory

	//private int personalNeeded = 0;
	
	//private boolean needsAllocation;

	@OneToOne(cascade = CascadeType.ALL)
	private MampfDate date;
	
	@ManyToMany(cascade = CascadeType.ALL)
	private List<Employee> employees;
	
	@SuppressWarnings("unused")
	private MampfOrder(){}
	public MampfOrder(UserAccount account, Cash cash, MampfDate date) {
		super(account, cash);
		this.date = date;
		employees = new ArrayList<>();
	}

	//public OrderLine addOrderLine(Item product, Quantity quantity) {

		
		//if (product.getCategory().equals(Item.Category.PERSONEL)) {
		//	needsAllocation = true;
		//}
	//	return super.addOrderLine(product, quantity);
	//}

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
		return paymentMethod.toString();
	}
	//public boolean isDone() {
	//	if(done) return true;
	//	else return false;
	//}
}
