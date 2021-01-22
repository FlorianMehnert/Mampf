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
	private final UserRepository userRepository;

	/**
	 * Creates a new {@link UserDataInitializer} with the given {@link UserAccountManagement} and
	 * {@link UserRepository}.
	 *
	 * @param userAccountManagement must not be {@literal null}.
	 * @param userManagement must not be {@literal null}.
	 */
	UserDataInitializer(UserAccountManagement userAccountManagement, UserManagement userManagement,
						UserRepository userRepository) {

		Assert.notNull(userAccountManagement, "UserAccountManagement must not be null!");
		Assert.notNull(userManagement, "CustomerRepository must not be null!");

		this.userAccountManagement = userAccountManagement;
		this.userManagement = userManagement;
		this.userRepository = userRepository;
	}

	@Override
	public void initialize() {

		// Skip creation if database was already populated
		if (userAccountManagement.findByUsername("hansWurst").isPresent()) {
			return;
		}

		LOG.info("Creating default users and customers.");

		User admin = new User(userAccountManagement.create("hansWurst", Password.UnencryptedPassword.of("123"),
				"mampf@mampf.de",Role.of(UserRole.BOSS.name())), "Mampf - Firmenzentrale");
		admin.getUserAccount().setFirstname("Hanst");
		admin.getUserAccount().setLastname("Wurst");
		userRepository.save(admin);

		var password = "123";
		List.of(//
				new RegistrationForm("hans", "hans", "jürgen", password, "Burg Schreckenstein",
						"wurst@example.com", UserRole.INDIVIDUAL.name(), "", ""),
				new RegistrationForm("dextermorgan", "dexter", "morgan", password, "Burg Schreckenstein",
						"Miami-Dade-County@example.com", UserRole.COMPANY.name(), "BroCompany", ""),
				new RegistrationForm("earlhickey", "earl", "thickey", password, "Burg Schreckenstein",
						"CamdenCounty-Motel@example.com", UserRole.INDIVIDUAL.name(), "", ""),
				new RegistrationForm("mclovinfogell", "mc", "lovinfogell", password, "Burg Schreckenstein",
						"LosAngeles@example.com", UserRole.INDIVIDUAL.name(), "", "")//
		).forEach(userManagement::createUser);

		String accessCode = userManagement.findUserByUsername("dextermorgan").get().getCompany().get().getAccessCode();
		List.of(//
				new RegistrationForm("tripster", "trip", "ster", password, "Burg Schreckenstein",
						"taaaaaada@example.com", UserRole.EMPLOYEE.name(), "", accessCode),
				new RegistrationForm("booney", "booney", "-", password, "Burg Schreckenstein",
						"haha-es-geht-los@example.com", UserRole.EMPLOYEE.name(), "BroCompany", accessCode),
				new RegistrationForm("klabrovsky", "kla", "brovsky", password, "Burg Schreckenstein",
						"test01@example.com", UserRole.EMPLOYEE.name(), "", accessCode),
				new RegistrationForm("mcdonald", "donald", "mcdonald", password, "Burg Schreckenstein",
						"abcdefg@example.com", UserRole.EMPLOYEE.name(), "Beispielfirma", accessCode)//
		).forEach(userManagement::createUser);
	}
}
