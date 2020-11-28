package mampf.order;


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
	
	
	public MampfOrder(UserAccount account, Cash cash) {
		super(account,cash);
	}
}
