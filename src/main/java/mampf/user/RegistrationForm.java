package mampf.user;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

class RegistrationForm {

	@NotEmpty(message = "{RegistrationForm.username.NotEmpty}") //
	private final String username;

	@NotEmpty(message = "{RegistrationForm.firstname.NotEmpty}") //
	private final String firstname;

	@NotEmpty(message = "{RegistrationForm.lastname.NotEmpty}") //
	private final String lastname;

	@NotEmpty(message = "{RegistrationForm.password.NotEmpty}") //
	@Size(min = 5, max = 20, message = "{RegistrationForm.password.minMessage}")
	private final String password;

	@NotEmpty(message = "{RegistrationForm.address.NotEmpty}") //
	private final String address;

	@NotEmpty(message = "{RegistrationForm.email.NotEmpty}") // s
	private final String email;

	private final String role;

	private final String companyName;

	private final String accessCode;

	public RegistrationForm(String username, @NotEmpty String firstname, @NotEmpty String lastname, String password,
							String address, String email, String role, String companyName, String accessCode) {
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
		this.password = password;
		this.address = address;
		this.email = email;
		this.role = role;
		this.companyName = companyName;
		this.accessCode = accessCode;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getAccessCode() {
		return accessCode;
	}

	public String getAddress() {
		return address;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}
}
