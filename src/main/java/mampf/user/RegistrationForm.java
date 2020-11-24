package mampf.user;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

class RegistrationForm {

	@NotEmpty(message = "{RegistrationForm.name.NotEmpty}") //
	private final String name;

	@NotEmpty(message = "{RegistrationForm.password.NotEmpty}") //
	@Size(min = 5, max = 20, message = "{RegistrationForm.password.minMessage}")
	private final String password;

	@NotEmpty(message = "{RegistrationForm.address.NotEmpty}") // s
	private final String address;

	private final String role;

	public RegistrationForm(String name, String password, String address, String role) {
		this.name = name;
		this.password = password;
		this.address = address;
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	public String getAddress() {
		return address;
	}

	public String getRole() {
		return role;
	}
}
