package mampf.employee;

import mampf.catalog.Item;
import mampf.order.EventOrder;
import mampf.order.OrderController;
import mampf.user.User;
import mampf.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.processing.Generated;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EmployeeTest {
	private Employee employee;
	private String name = "test";
	private List<EventOrder> booked = new ArrayList<>();
	private Employee.Role role = Employee.Role.COOK;

	@Autowired
	UserAccountManagement userAccountManagement;

	@BeforeEach
	public void setUp(){
		employee = new Employee(name, role);
	}

	@Test
	public void testGetter(){
		assertTrue(name.equals(employee.getName()), "test name");
		assertTrue((role.equals(employee.getRole())), "test role");
	}


	/*@Test
	public void testSetter(){
		String username = "test";
		Password.UnencryptedPassword password = Password.UnencryptedPassword.of("123");
		String address = "mi casa";
		String email = "anna.c@gmail.com";
		PaymentMethod paymentMethod = PaymentMethod("Cash");

		UserAccount userAccount = userAccountManagement.create(username, password, email, Role.of(UserRole.INDIVIDUAL.name()));
		LocalDateTime startDate = LocalDateTime.now().plus(OrderController.delayForEarliestPossibleBookingDate).plus(Duration.ofDays(1));
		LocalDateTime endDate = startDate.plusHours(2);

		EventOrder order = new EventOrder(userAccount, null, Item.Domain.EVENTCATERING, startDate, endDate, address);
		booked.add(order);

		assertTrue(booked.equals(employee.setBooked(order)), "test order");
	}*/

}
