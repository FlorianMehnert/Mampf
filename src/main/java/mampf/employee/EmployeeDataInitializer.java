package mampf.employee;

import java.util.List;

import org.salespointframework.core.DataInitializer;
import org.salespointframework.useraccount.Password.UnencryptedPassword;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccountManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class EmployeeDataInitializer implements DataInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(EmployeeDataInitializer.class);

	private final UserAccountManagement userAccountManagement;
	private final EmployeeManagement employeeManagement;

	EmployeeDataInitializer(UserAccountManagement userAccountManagement, EmployeeManagement employeeManagement){

		Assert.notNull(userAccountManagement, "UserAccountManagement should not be null");
		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.userAccountManagement = userAccountManagement;
		this.employeeManagement = employeeManagement;
	}

	@Override
	public void initialize(){

		LOG.info("Creating default employee users");

		var password = "123";

		List.of(new RegistrationForm("sofia89", "Sofia", "Lopez", password),
				new RegistrationForm("marivi38", "Maria", "Leon", password),
				new RegistrationForm("peters", "Peter", "Schloss", password));

	}
}
