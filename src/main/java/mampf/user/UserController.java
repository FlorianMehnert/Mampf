package mampf.user;

import com.mysema.commons.lang.Pair;
import org.salespointframework.useraccount.Password;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Optional;

@Controller
public class UserController {

	private final UserManagement userManagement;

	UserController(UserManagement userManagement) {

		Assert.notNull(userManagement, "UserManagement must not be null!");

		this.userManagement = userManagement;
	}

	/**
	 * Take the input of an filled and validated {@link RegistrationForm} and do some additional validation logic.
	 * Reshow the registration form, if the validation fails.
	 * Else create a new user.
	 *
	 * @param form   The registration form to handle the input from
	 * @param result The Errors to bind them to the form.
	 * @return Return the register view again or redirect to the start page.
	 */
	@PostMapping("/register")
	public String registerNew(@Valid @ModelAttribute("form") RegistrationForm form, Errors result) {
		for (User user : userManagement.findAll()) {
			if ((form.getUsername().equals(user.getUserAccount().getUsername()))) {
				result.rejectValue("username", "RegistrationForm.username.exists",
						"This Username is already taken!");
			}
			if ((form.getEmail().equals(user.getUserAccount().getEmail()))) {
				result.rejectValue("email", "RegistrationForm.email.exists",
						"This E-Mail is already taken!");
			}
		}
		if(form.getRole().equals("EMPLOYEE")
				&& (form.getAccessCode().length() != 6 || userManagement.findCompany(form.getAccessCode()).isEmpty())
		) {
			result.rejectValue("accessCode", "RegistrationForm.accessCode.wrong","You've entered an incorrect access code!");
		}
		if (form.getRole().equals("COMPANY") && form.getCompanyName().length() == 0) {
			result.rejectValue("companyName", "RegistrationForm.companyName.NotEmpty",
					"The company name can not be empty!");
		}
		if (form.getRole().equals("INDIVIDUAL") &&
				(form.getCompanyName().length() > 0 || form.getAccessCode().length() > 0)) {
			result.reject("wrongInput",
					"Bad inputs were used!");
		}
		if (result.hasErrors()) {
			return "register";
		}
		// (｡◕‿◕｡)
		// Falls alles in Ordnung ist legen wir einen Customer an
		userManagement.createUser(form);
		return "redirect:/";
	}

	@GetMapping("/register")
	public String register(Model model, RegistrationForm form) {
		model.addAttribute("form", form);
		return "register";
	}

	/**
	 * List all users (only available if authenticated as BOSS)
	 * Optional: filter the name, username and account for the given filter string.
	 *
	 * @param model Bind all data to the model
	 * @param filter Optional: filter to apply to all users
	 * @return view of all (filtered) users
	 */
	@GetMapping("/users")
	@PreAuthorize("hasRole('BOSS')")
	public String users(Model model, @RequestParam(required = false) String filter) {

		model.addAttribute("userList", userManagement.findAll());
		ArrayList<Pair<User, String>> list = new ArrayList<>();
		String filterString = "";
		if (filter != null) {
			filterString = filter.toLowerCase();
		}
		for (User user : userManagement.findAll()) {
			String role = User.roleTranslations.get(user.getUserAccount().getRoles().toList().get(0).toString());
			if (filterString.length() == 0 || userContainsFilterString(user, filter)) {
				Pair<User, String> map = new Pair<>(user, role);
				list.add(map);
			}
		}
		if (filter == null) {
			filter = "";
		}
		model.addAttribute("filter", filter);
		model.addAttribute("pairs", list);
		return "users";
	}

	/**
	 * Test the given user if he matches the filter
	 * @param user The user model to test for.
	 * @param filter The filter to apply to the user.
	 * @return boolean true if the user matches the filter
	 */
	private boolean userContainsFilterString(User user, String filter) {
		return user.getUserAccount().getUsername().toLowerCase().contains(filter)
				|| user.getUserAccount().getFirstname().toLowerCase().contains(filter)
				|| user.getUserAccount().getLastname().toLowerCase().contains(filter)
				|| user.getUserAccount().getEmail().toLowerCase().contains(filter);
	}

	@GetMapping("/userDetails/")
	@PreAuthorize("isAuthenticated()")
	public String userDetails(Model model, Authentication authentication, ChangePasswordForm form) {
		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isPresent()) {
			model.addAttribute("user", user.get());
			model.addAttribute("form", form);
			return "userDetails";
		}
		return "redirect:/";
	}

	/**
	 * If the new password is valid, change it and log the user out.
	 * @param model The model for the user details view.
	 * @param passwordForm The {@link ChangePasswordForm} to validate
	 * @param result Contains validation errors
	 * @param authentication get the current user
	 * @param httpServletRequest needed for logging the user out
	 * @return the userdetails or the log in form
	 * @throws ServletException
	 */
	@PostMapping("/change_password/")
	@PreAuthorize("isAuthenticated()")
	public String changePassword(Model model, @Valid @ModelAttribute("form") ChangePasswordForm passwordForm,
								 Errors result, Authentication authentication,
								 HttpServletRequest httpServletRequest) throws ServletException {
		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (!passwordForm.getPassword().equals(passwordForm.getConfirmPassword())) {
			result.rejectValue("password", "RegistrationForm.password.MotEqual",
					"Your Passwords don't match!");
		}
		if (result.hasErrors()) {
			model.addAttribute("user", user.get());
			return "userDetails";
		}
		if (user.isPresent()) {
			userManagement.changePassword(user.get(), Password.UnencryptedPassword.of(passwordForm.getPassword()));
			httpServletRequest.logout();
			return "/login";
		}
		return "redirect:/";
	}

	@GetMapping("/userDetailsAsBoss/{userId}")
	@PreAuthorize("hasRole('BOSS')")
	public String userDetailsAsBoss(@PathVariable long userId, Model model) {
		Optional<User> user = userManagement.findUserById(userId);
		if (user.isEmpty()) {
			return "users";
		}
		model.addAttribute("user", user.get());
		return "userDetails";
	}

	@PostMapping("/deleteUser")
	@PreAuthorize("hasRole('BOSS')")
	public String denyAuthentication(@RequestParam("userId") long userId) {
		userManagement.denyAuthenticationById(userId);
		return "redirect:/users";
	}


	@GetMapping("/bookBreakfast")
	@PreAuthorize("isAuthenticated()")
	public String bookBreakfast(Authentication authentication) {

		if (userManagement.bookMobileBreakfast(authentication.getName())) {
			return "redirect:/userDetails/";
		}
		return "redirect:/";
	}

}