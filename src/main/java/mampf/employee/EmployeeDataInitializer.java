package mampf.employee;

import java.util.List;

import org.salespointframework.core.DataInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Initializes default {@link Employee}s. The following are created:
 * <ul>
 *     <li>The cooks "Elisa Schloss", "Monica Geller", "Peter Parker", "Chandler Bing",
 *     "Phoebe Buffay" and "Heinz Solo".</li>
 *     <li>The service personnel "Anna Lopez", "Maria Leon", "Rachel Green", "Scott Lang",
 *     "Joey Tribbiani", "Ross Geller"</li>
 * </ul>
 */
@Component
public class EmployeeDataInitializer implements DataInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(EmployeeDataInitializer.class);

	private final EmployeeManagement employeeManagement;

	/**
	 * Creates a new {@link EmployeeDataInitializer} with the given {@link EmployeeManagement}
	 *
	 * @param employeeManagement must not be {@literal null}
	 */
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
				new RegistrationForm("fashion", "Rachel", "Green", serviceRole),
				new RegistrationForm("antman", "Scott", "Lang", serviceRole),
				new RegistrationForm("food", "Joey", "Tribbiani", serviceRole),
				new RegistrationForm("dinosaur", "Ross", "Geller", serviceRole),
				new RegistrationForm("mcdreamy", "Meredith", "Grey", cookRole),
				new RegistrationForm("chef", "Monica", "Geller", cookRole),
				new RegistrationForm("notspiderman", "Peter", "Parker", cookRole),
				new RegistrationForm("chickduck", "Chandler", "Bing", cookRole),
				new RegistrationForm("smellycat", "Phoebe", "Buffay", cookRole),
				new RegistrationForm("heinz55", "Heinz", "Solo", cookRole))
				.forEach(employeeManagement::createEmployee);

	}
}
