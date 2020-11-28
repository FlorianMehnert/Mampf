package mampf.employee;

import javax.validation.constraints.NotEmpty;

public class RegistrationForm {
	@NotEmpty(message = "Username should not be empty")//
	private final String username;

	@NotEmpty(message = "Username should not be empty")//
	private final String first_name;

	@NotEmpty(message = "Username should not be empty")//
	private final String last_name;

	@NotEmpty(message = "Password should not be empty")//
	private final String password;

	//todo is there an email for registration?
	//@NotEmpty(message = "Email should not be empty")
	//private final String email;

	//todo should the code be in the registration form or in mobile breakfast page?
	//private final String accessCode;

	//todo do i have to write the name of the company & anschrift?

	public RegistrationForm(String username, String first_name, String last_name, String password){
		this.username = username;
		this.first_name = first_name;
		this.last_name = last_name;
		this.password = password;

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

	public String getPassword(){
		return password;
	}

	//public String getEmail(){
	//	return email;
	//}
}
