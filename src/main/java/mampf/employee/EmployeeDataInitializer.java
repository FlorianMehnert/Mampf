package mampf.employee;

import java.util.List;

import org.salespointframework.core.DataInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class EmployeeDataInitializer implements DataInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(EmployeeDataInitializer.class);


	private final EmployeeManagement employeeManagement;

	EmployeeDataInitializer(EmployeeManagement employeeManagement){

		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.employeeManagement = employeeManagement;
	}

	@Override
	public void initialize(){

		LOG.info("Creating default employee users");

		String serviceRole = "SERVICE";
		String cookRole = "COOK";
		List.of(new RegistrationForm("anna89", "Anna", "Lopez", serviceRole),
				new RegistrationForm("marivi38", "Maria", "Leon", serviceRole),
				new RegistrationForm("x", "Maria1", "Leon1", serviceRole),
				new RegistrationForm("y", "Maria2", "Leon2", serviceRole),
				new RegistrationForm("z", "Maria3", "Leon3", serviceRole),
				new RegistrationForm("123", "Maria4", "Leon4", serviceRole),
				new RegistrationForm("elisas", "Elisa1", "Schloss1", cookRole),
				new RegistrationForm("a", "Elisa2", "Schloss2", cookRole),
				new RegistrationForm("b", "Elisa3", "Schloss3", cookRole),
				new RegistrationForm("c", "Elisa4", "Schloss5", cookRole),
				new RegistrationForm("d", "Elisa6", "Schloss6", cookRole),
				new RegistrationForm("heinz55", "Heinz", "Solo", cookRole))
				.forEach(employeeManagement::createEmployee);

	}
}
