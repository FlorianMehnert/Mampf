package mampf.employee;

import javax.validation.constraints.NotEmpty;

public class RegistrationForm {
	@NotEmpty(message = "Username should not be empty")//
	private final String username;

	@NotEmpty(message = "First name should not be empty")//
	private final String firstName;

	@NotEmpty(message = "Last name should not be empty")//
	private final String lastName;

	@NotEmpty(message = "Role should not be empty")//
	private final String role;


	public RegistrationForm(String username, String firstName, String lastName, String role){
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;

	}

	public String getUsername(){
		return username;
	}

	public String getFirstName(){
		return firstName;
	}

	public String getLastName(){
		return lastName;
	}

	public String getRole(){
		return role.toUpperCase();
	}

}
