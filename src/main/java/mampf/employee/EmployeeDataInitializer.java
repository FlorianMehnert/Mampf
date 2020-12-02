package mampf.employee;

import java.util.List;

import org.salespointframework.core.DataInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
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


		List.of(new RegistrationForm("anna89", "Anna", "Lopez", "SERVICE"),
				new RegistrationForm("marivi38", "Maria", "Leon", "SERVICE"),
				new RegistrationForm("x", "Maria1", "Leon1", "SERVICE"),
				new RegistrationForm("y", "Maria2", "Leon2", "SERVICE"),
				new RegistrationForm("z", "Maria3", "Leon3", "SERVICE"),
				new RegistrationForm("123", "Maria4", "Leon4", "SERVICE"),
				new RegistrationForm("elisas", "Elisa1", "Schloss1", "COOK"),
				new RegistrationForm("a", "Elisa2", "Schloss2", "COOK"),
				new RegistrationForm("b", "Elisa3", "Schloss3", "COOK"),
				new RegistrationForm("c", "Elisa4", "Schloss5", "COOK"),
				new RegistrationForm("d", "Elisa6", "Schloss6", "COOK"),
				new RegistrationForm("heinz55", "Heinz", "Solo", "COOK")).forEach(employeeManagement::createEmployee);

	}
}
