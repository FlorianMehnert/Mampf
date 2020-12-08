package mampf.user;

import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.Optional;

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
		var userAccount = userAccounts.create(form.getUsername(), password, form.getEmail(), Role.of(form.getRole()));
		userAccount.setFirstname(form.getFirstname());
		userAccount.setLastname(form.getLastname());

		User user = new User(userAccount, form.getAddress());

		if(form.getRole().equals(UserRole.COMPANY.name())) {
			Company company = new Company(form.getCompanyName());
			user.setCompany(company);
		}
		if(form.getRole().equals(UserRole.EMPLOYEE.name()) && findCompany(form.getAccessCode()).isPresent()) {
			Company company = findCompany(form.getAccessCode()).get();
			company.addEmployee(user);
		}

		return users.save(user);
	}

	/**
	 * Returns all {@link User}s currently available in the system.
	 *
	 * @return all {@link User} entities.
	 */
	public Streamable<User> findAll() {
		return users.findAll();
	}

	public Optional<Company> findCompany(String accessCode) {
		Iterator<User> userIterator = users.findAll().iterator();
		while (userIterator.hasNext()) {
			User user = userIterator.next();
			if(user.getCompany().isPresent() && user.getCompany().get().getAccessCode().equals(accessCode)) {
				return user.getCompany();
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns a {@link User} that matches the id
	 * @return User
	 */
	public Optional<User> findUserById(long userId) {
		User user = null;
		Iterator<User> userIterator = users.findAll().iterator();
		do{
			user = userIterator.next();
			if(user.getId() == userId) {
				return Optional.of(user);
			}
		}while (userIterator.hasNext());
		return Optional.empty();
	}

	/**
	 * Returns a {@link User} that matches the id
	 * @return User
	 */
	public Optional<User> findUserByUsername(String username) {
		User user = null;
		Iterator<User> userIterator = users.findAll().iterator();
		do{
			user = userIterator.next();
			if(user.getUserAccount().getUsername().equals(username)) {
				return Optional.of(user);
			}
		}while (userIterator.hasNext());
		return Optional.empty();
	}

	public void denyAuthenticationById(long userId) {
		Optional<User> optionalUser= this.findUserById(userId);
		if(optionalUser.isPresent()) {
			User user = optionalUser.get();
			if(!user.getUserAccount().hasRole(Role.of(UserRole.BOSS.name()))) {
				user.getUserAccount().setEnabled(false);
				users.save(user);
			}
		}
	}

}
