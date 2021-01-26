package mampf.user;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

class ChangePasswordForm {

	@NotEmpty(message = "{RegistrationForm.password.NotEmpty}") //
	@Size(min = 5, message = "{RegistrationForm.password.minMessage}")
	private final String password;

	@NotEmpty(message = "{RegistrationForm.password.NotEmpty}") //
	@Size(min = 5, message = "{RegistrationForm.password.minMessage}")
	private final String confirmPassword;

	public ChangePasswordForm(String password, String confirmPassword) {
		this.password = password;
		this.confirmPassword = confirmPassword;

	}

	public String getPassword() {
		return password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}
}
