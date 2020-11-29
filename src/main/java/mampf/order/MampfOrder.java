package mampf.order;

import mampf.employee;

import org.salespointframework.core.AbstractEntity;
import org.salespointframework.order.Cart;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;


public class MampfOrder extends Order{
	//TODO:
	// addEmployee
	// findByCategory
	
	private List<Employee> employees;
	private int personalNeeded = 0, cookNeeded = 0;
	
	public MampfOrder(UserAccount account, Cash cash) {
		super(account,cash);
		employees = new ArrayList<>();
	}
	
	public OrderLine addOrderLine(Product p, Quantity q) {
		
		if(p instanceof EmployeeItem) {
			EmployeeType type = p.getType();
			if(type.equals(EmployeeType.COOK)) cookNeeded+=q.getAmount().intValue();
			if(type.equals(EmployeeType.SERVICEPERSONAL)) personalNeeded+=q.getAmount().intValue();			
		}
		
		return super.addOrderLine(p,q);
	}
		
	
	public addEmployee(Employee employee) {
	//TODO: nullcheck
	//wird vom ordermanager aufgerufen
		EmployeeType type = employee.getType();
		if(type.equals(EmployeeType.COOK)) if(cookNeeded >0)cookNeeded--;else return; 
		if(type.equals(EmployeeType.SERVICEPERSONAL)) if(personalNeeded >0) personalNeeded--;else return;			
		
		employees.add(employee);		
	}
	
	public int getPersonalNeeded() {return personalNeeded;}
	public int getCookNeeded() {return cookNeeded;}
	public boolean isDone() {return (personalNeeded == 0 && cookNeeded == 0);}
}
