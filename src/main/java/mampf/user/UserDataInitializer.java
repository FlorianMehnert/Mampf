package mampf.user;

import org.salespointframework.core.DataInitializer;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccountManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Initializes default mampf.user accounts and customers. The following are created:
 * <ul>
 * <li>An admin mampf.user named "hansWurst".</li>
 * <li>The customers "hans", "dextermorgan", "earlhickey", "mclovinfogell" backed by mampf.user accounts with the same
 * name.</li>
 * </ul>
 *
 */
@Component
@Order(10)
public class UserDataInitializer implements DataInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(UserDataInitializer.class);

	private final UserAccountManagement userAccountManagement;
	private final UserManagement userManagement;

	/**
	 * Creates a new {@link UserDataInitializer} with the given {@link UserAccountManagement} and
	 * {@link UserRepository}.
	 *
	 * @param userAccountManagement must not be {@literal null}.
	 * @param userManagement must not be {@literal null}.
	 */
	UserDataInitializer(UserAccountManagement userAccountManagement, UserManagement userManagement) {

		Assert.notNull(userAccountManagement, "UserAccountManagement must not be null!");
		Assert.notNull(userManagement, "CustomerRepository must not be null!");

		this.userAccountManagement = userAccountManagement;
		this.userManagement = userManagement;
	}

	@Override
	public void initialize() {

		// Skip creation if database was already populated
		if (userAccountManagement.findByUsername("hansWurst").isPresent()) {
			return;
		}

		LOG.info("Creating default users and customers.");

		userAccountManagement.create("hansWurst", Password.UnencryptedPassword.of("123"), Role.of(UserRole.BOSS.name()));

		var password = "123";

		List.of(//
				new RegistrationForm("hans", password, "wurst", UserRole.INDIVIDUAL.name(), "", ""),
				new RegistrationForm("dextermorgan", password, "Miami-Dade County", UserRole.COMPANY.name(), "BroCompany", ""),
				new RegistrationForm("earlhickey", password, "Camden County - Motel", UserRole.INDIVIDUAL.name(), "", ""),
				new RegistrationForm("mclovinfogell", password, "Los Angeles", UserRole.INDIVIDUAL.name(), "", "")//
		).forEach(userManagement::createUser);
	}
}
