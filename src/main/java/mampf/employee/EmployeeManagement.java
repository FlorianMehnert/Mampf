package mampf.employee;

import org.salespointframework.useraccount.Password.UnencryptedPassword;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Transactional
public class EmployeeManagement {

	public static final Role EMPLOYEE_ROLE = Role.of("EMPLOYEE");
	private final EmployeeRepository employees;
	private final UserAccountManagement employeeAccounts;

	EmployeeManagement(EmployeeRepository employees, UserAccountManagement employeeAccounts){
		Assert.notNull(employees, "EmployeeRepository should not be null");
		Assert.notNull(employeeAccounts, "UserAccountManagement should not be null");

		this.employees = employees;
		this.employeeAccounts = employeeAccounts;
	}

	public Employee createEmployee(RegistrationForm form){
		Assert.notNull(form, "Registration form should not be null");

		var password = UnencryptedPassword.of(form.getPassword());
		var userAccount = employeeAccounts.create(form.getUsername(), password, EMPLOYEE_ROLE);
		userAccount.setFirstname(form.getFirst_name());
		userAccount.setLastname(form.getLast_name());

		return employees.save(new Employee(userAccount));
	}

	public Streamable<Employee> findAll() {
		return employees.findAll();
	}
}
