package mampf.user;

import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Transactional
public class UserManagement {

	public static final Role CUSTOMER_ROLE = Role.of("CUSTOMER");

	private final UserRepository users;
	private final UserAccountManagement userAccounts;

	/**
	 * Creates a new {@link UserManagement} with the given {@link UserRepository} and
	 * {@link UserAccountManagement}.
	 *
	 * @param users must not be {@literal null}.
	 * @param userAccounts must not be {@literal null}.
	 */
	UserManagement(UserRepository users, @Qualifier("persistentUserAccountManagement") UserAccountManagement userAccounts) {

		Assert.notNull(users, "CustomerRepository must not be null!");
		Assert.notNull(userAccounts, "UserAccountManagement must not be null!");

		this.users = users;
		this.userAccounts = userAccounts;
	}

	/**
	 * Creates a new {@link User} using the information given in the {@link RegistrationForm}.
	 *
	 * @param form must not be {@literal null}.
	 * @return the new {@link User} instance.
	 */
	public User createUser(RegistrationForm form) {

		Assert.notNull(form, "Registration form must not be null!");
		Assert.isTrue(!form.getRole().equals(UserRole.BOSS.name()), "It's not allowed to set the boss here");

		var password = Password.UnencryptedPassword.of(form.getPassword());
		var userAccount = userAccounts.create(form.getName(), password, form.getName(), Role.of(form.getRole()));

		return users.save(new User(userAccount));
	}

	/**
	 * Returns all {@link User}s currently available in the system.
	 *
	 * @return all {@link User} entities.
	 */
	public Streamable<User> findAll() {
		return users.findAll();
	}
}
