package mampf.user;

import net.bytebuddy.utility.RandomString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COMPANIES")
public class Company {

	private String name;


	@OneToMany
	private List<User> employees = new ArrayList<>();
	private boolean bookedBreakfast;
	private String accessCode;

	private @Id @GeneratedValue long id;

	public Company(String name)
	{
		this.name = name;
		this.accessCode = RandomString.make(6);
	}

	public Company() {
		this.accessCode = RandomString.make(6);
	}

	public void addEmployee(User employee) {
		employees.add(employee);
	}

	public void removeEmployee(User employee) {
		employees.remove(employee);
	}


	public long getId() {
		return id;
	}

	public String getAccessCode() {
		return accessCode;
	}

	public List<User> getEmployees() {
		return employees;
	}

	public String getName() {
		return name;
	}
}
