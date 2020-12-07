package mampf.user;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;

@Controller
class UserController {

	private final UserManagement userManagement;

	UserController(UserManagement userManagement) {

		Assert.notNull(userManagement, "UserManagement must not be null!");

		this.userManagement = userManagement;
	}

	@PostMapping("/register")
	String registerNew(@Valid @ModelAttribute("form") RegistrationForm form, Errors result) {
		for (User user : userManagement.findAll()) {
			if ((form.getUsername().equals(user.getUserAccount().getUsername()))) {
				result.rejectValue("username", "RegistrationForm.username.exists", "This Username is already taken!");
			}
			if ((form.getEmail().equals(user.getUserAccount().getEmail()))) {
				result.rejectValue("email", "RegistrationForm.username.exists", "This E-Mail does exists already!");
			}
		}
		if(form.getRole().equals("EMPLOYEE") && form.getAccessCode().length() != 6 ) {
			result.rejectValue("accessCode", "RegistrationForm.accessCode.NotEmpty","You've entered an incorrect access code!");
		}
		if (form.getRole().equals("EMPLOYEE") && userManagement.findCompany(form.getAccessCode()).isEmpty()) {
			result.rejectValue("accessCode", "RegistrationForm.accessCode.notExists","You've entered a wrong access code!");
		}
		if (form.getRole().equals("COMPANY") && form.getCompanyName().length() == 0) {
			result.rejectValue("companyName", "RegistrationForm.companyName.NotEmpty","The company name can not be empty!");
		}
		if (form.getRole().equals("INDIVIDUAL") && (form.getCompanyName().length() > 0 || form.getAccessCode().length() > 0)) {
			result.reject("wrongInput", "Bad inputs were used!");
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

	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('BOSS')")
	public String users(Model model) {

		model.addAttribute("userList", userManagement.findAll());
		ArrayList<Pair<User, String>> list = new ArrayList<>();
		for (User user : userManagement.findAll()) {
			String role = Util.renderDomainName(user.getUserAccount().getRoles().toList().get(0).toString());
			Pair<User, String> map = new Pair<>(user, role);
			list.add(map);
		}
		System.out.println(list);
		model.addAttribute("pairs", list);
		return "users";
	}

	@GetMapping("/userDetails/")
	@PreAuthorize("isAuthenticated()")
	public String userDetails(Model model, Authentication authentication) {
		Optional<User> user = userManagement.findUserByUsername(authentication.getName());
		if (user.isPresent()) {
			model.addAttribute("user", user.get());
			return "userDetails";
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
	public String denyAuthentication(@RequestParam("userId") long userId, Model model) {
		userManagement.denyAuthenticationById(userId);
		return "redirect:/users";
	}


}