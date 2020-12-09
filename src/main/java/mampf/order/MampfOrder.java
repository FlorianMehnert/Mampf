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

@Entity
public class MampfOrder extends Order {
	private Item.Domain domain;

	@OneToOne(cascade = CascadeType.ALL)
	private MampfDate date;

	@ManyToMany(cascade = CascadeType.ALL)
	private List<Employee> employees;

	@SuppressWarnings("unused")
	private MampfOrder() {
	}

	public MampfOrder(UserAccount account,
					  PaymentMethod paymentMethod,
					  Item.Domain domain,
					  MampfDate date) {
		super(account, paymentMethod);
		this.date = date;
		employees = new ArrayList<>();
		this.domain = domain;
	}

	public void addEmployee(Employee employee) {
		employees.add(employee);
	}

	public Item.Domain getDomain() {
		return domain;
	}

	public MampfDate getDate() {
		return date;
	}

	public List<Employee> getEmployees() {
		return employees;
	}
	
	@Override
	public String toString() {
		return "Order: " + this.getDomain().toString();
	}

	public String getPayMethod() {
		PaymentMethod paymentMethod = super.getPaymentMethod();
		String res = "no payment";
		if (paymentMethod instanceof Cheque) {
			Cheque cheque = ((Cheque) paymentMethod);

			res = "CHECK: Nutzer:" + cheque.getAccountName() + ", Überweisung an: " + cheque.getBankName() + ","
					+ cheque.getBankAddress() + "," + cheque.getBankIdentificationNumber();
		}

		if (paymentMethod instanceof Cash)
			res = "BAR";

		return res;
	}
}
