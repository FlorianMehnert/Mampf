package mampf.employee;

import org.salespointframework.useraccount.UserAccount;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToOne;

@Entity
public class Employee {

	private @Id @GeneratedValue long id;

	@OneToOne
	private UserAccount userAccount;

	public Employee(UserAccount userAccount){
		this.userAccount = userAccount;
	}

	public long getId(){
		return id;
	}

	public UserAccount getUserAccount(){
		return userAccount;
	}

}
