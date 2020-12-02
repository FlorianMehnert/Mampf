package mampf.order;

import mampf.catalog.Item;
import mampf.catalog.Item.Category;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import org.salespointframework.catalog.Product;
import org.salespointframework.core.AbstractEntity;
import org.salespointframework.order.Cart;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

@Entity
public class MampfOrder extends Order {
	// TODO:
	// addEmployee
	// findByCategory

	// private int personalNeeded = 0;
	private boolean done;
	private boolean needsAllocation;
	private ArrayList<Employee> employees;

	@SuppressWarnings("unused")
	private MampfOrder(){}
	public MampfOrder(UserAccount account, Cash cash) {
		super(account, cash);
	}

	public OrderLine addOrderLine(Item product, Quantity quantity) {

		if (product.getCategory().equals(Item.Category.STAFF) && needsAllocation == false) {
			needsAllocation = true;
		}
		return super.addOrderLine(product, quantity);
	}

	// public addEmployee(Employee employee) {

	// }

	// public boolean getPersonalNeeded() {
	// 	return personalNeeded;
	// }

	public boolean isDone() {
		if(done) return true;
		else return false;
	}
}
