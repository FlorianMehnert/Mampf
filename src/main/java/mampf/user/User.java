package mampf.user;

import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class User {
	private @Id @GeneratedValue	long id;

	@OneToOne
	private UserAccount userAccount;

	public User(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	public User() {	}

	public long getId() {
		return id;
	}

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public static Role AccountRoleFromUserRole(UserRole role)
	{
		return Role.of(role.name());
	}
}
