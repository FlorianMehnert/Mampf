package mampf.user;

import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;

import javax.persistence.*;
import java.util.Optional;

@Entity
public class User {
	private @Id @GeneratedValue	long id;

	@OneToOne
	private UserAccount userAccount;

	//
	@OneToOne(cascade = CascadeType.ALL)
	private Company company;

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

	public Optional<Company> getCompany() {
		return Optional.ofNullable(company);
	}

	public void setCompany(Company company) {
//		if(userAccount.getRoles().toList().contains(UserRole.COMPANY.name())) {
			this.company = company;
//		}
	}
}
