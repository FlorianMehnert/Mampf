package mampf.user;

import mampf.Util;
import mampf.catalog.Item;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;

import javax.persistence.*;
import java.util.Map;
import java.util.Optional;

@Entity
public class User {
	private @Id @GeneratedValue	long id;

	@OneToOne
	private UserAccount userAccount;

	@OneToOne(cascade = CascadeType.ALL)
	private Company company;

	private String address;

	public User(UserAccount userAccount, String address) {
		this.userAccount = userAccount;
		this.address = address;
	}

	public User() {	}

	static Map<String, String> roleTranslations = Map.of(
			UserRole.BOSS.toString(), "Mampf-Chef",
			UserRole.EMPLOYEE.toString(), "Mitarbeiter",
			UserRole.COMPANY.toString(), "Firma",
			UserRole.INDIVIDUAL.toString(), "Einzelperson");

	public long getId() {
		return id;
	}

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public Optional<Company> getCompany() {
		return Optional.ofNullable(company);
	}

	public void setCompany(Company company) {
		if(userAccount.hasRole(Role.of(UserRole.COMPANY.name()))) {
			this.company = company;
		}
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return userAccount.getUsername();
	}

}
