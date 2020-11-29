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
				new RegistrationForm("elisas", "Elisa", "Schloss", "COOK"),
				new RegistrationForm("heinz55", "Heinz", "Solo", "COOK")).forEach(employeeManagement::createEmployee);

	}
}
