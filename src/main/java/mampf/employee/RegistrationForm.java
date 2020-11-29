package mampf.employee;

import javax.validation.constraints.NotEmpty;

public class RegistrationForm {
	@NotEmpty(message = "Username should not be empty")//
	private final String username;

	@NotEmpty(message = "First name should not be empty")//
	private final String first_name;

	@NotEmpty(message = "Last name should not be empty")//
	private final String last_name;

	@NotEmpty(message = "Role should not be empty")//
	private final String role;


	public RegistrationForm(String username, String first_name, String last_name, String role){
		this.username = username;
		this.first_name = first_name;
		this.last_name = last_name;
		this.role = role;

	}

	public String getUsername(){
		return username;
	}

	public String getFirst_name(){
		return  first_name;
	}

	public String getLast_name(){
		return last_name;
	}

	public String getRole(){
		return role;
	}

}
