package mampf.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.Iterator;
import java.util.List;

@Controller
class UserController {

	private final UserManagement userManagement;

	UserController(UserManagement userManagement) {

		Assert.notNull(userManagement, "UserManagement must not be null!");

		this.userManagement = userManagement;
	}

	@PostMapping("/register")
	String registerNew(@Valid @ModelAttribute("form") RegistrationForm form, Errors result){
		if (result.hasErrors()) {
			return "register";
		}
		// (｡◕‿◕｡)
		// Falls alles in Ordnung ist legen wir einen Customer an
		userManagement.createUser(form);
		return "redirect:/";
	}

	@GetMapping("/register")
	String register(Model model, RegistrationForm form) {
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

		return "users";
	}

	@GetMapping("/userDetails/")
	@PreAuthorize("isAuthenticated()")
	public String userDetails(Model model, Authentication authentication)
	{
		if(userManagement.findUserByUsername(authentication.getName()).isPresent()) {
			model.addAttribute("user", userManagement.findUserByUsername(authentication.getName()).get());
			return "userDetails";
		}
		return "redirect:/";
	}

	@GetMapping("/userDetailsAsBoss/{userId}")
	@PreAuthorize("hasRole('BOSS')")
	public String userDetailsAsBoss(@PathVariable long userId, Model model)
	{
		if(userManagement.findUserById(userId).isEmpty()) {
			return "users";
		}
		User user = userManagement.findUserById(userId).get();
		model.addAttribute("user", user);
		return "userDetails";
	}

	@GetMapping("/deleteUser/{userId}")
	@PreAuthorize("hasRole('BOSS')")
	public String denyAuthentication(@PathVariable long userId, Model model)
	{
		userManagement.denyAuthenticationById(userId);
		return "redirect:/users";
	}


}