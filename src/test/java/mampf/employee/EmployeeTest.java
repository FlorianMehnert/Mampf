package mampf.employee;

import mampf.order.EventOrder;
import mampf.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.salespointframework.useraccount.UserAccount;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class EmployeeTest {
	private Employee employee;
	private String name = "test";
	private List<EventOrder> booked = null;
	private Employee.Role role = Employee.Role.COOK;

	@BeforeEach
	public void setUp(){
		employee = new Employee(name, role);
	}

	@Test
	public void testGetter(){
		assertTrue(name.equals(employee.getName()), "test name");
		assertTrue((role.equals(employee.getRole())), "test role");
	}


}
