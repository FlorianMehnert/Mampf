package mampf.user;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import mampf.catalog.Item;
import mampf.employee.Employee;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.useraccount.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.ArrayList;
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
	String registerNew(@Valid @ModelAttribute("form") RegistrationForm form, Errors result, RedirectAttributes redirAttrs) {
		boolean err = false;
		for (User user : userManagement.findAll()) {
			if ((form.getUsername().equals(user.getUserAccount().getUsername()))) {
				redirAttrs.addFlashAttribute("userAlreadyExists", "This Username is already taken!");
				err = true;
			}
			if((form.getEmail().equals(user.getUserAccount().getEmail()))){
				redirAttrs.addFlashAttribute("EMailAlreadyExists", "This E-Mail does exists already!");
				err = true;
			}
		}
		if (form.getRole().equals("INDIVIDUAL") && (form.getCompanyName().length() > 0 || form.getAccessCode().length() > 0)) {
			redirAttrs.addFlashAttribute("wrongInput", "Bad inputs were used!");
			err = true;
		}
		if(err){
			return "redirect:/register";
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
		if (userManagement.findUserByUsername(authentication.getName()).isPresent()) {
			model.addAttribute("user", userManagement.findUserByUsername(authentication.getName()).get());
			return "userDetails";
		}
		return "redirect:/";
	}

	@GetMapping("/userDetailsAsBoss/{userId}")
	@PreAuthorize("hasRole('BOSS')")
	public String userDetailsAsBoss(@PathVariable long userId, Model model) {
		if (userManagement.findUserById(userId).isEmpty()) {
			return "users";
		}
		User user = userManagement.findUserById(userId).get();
		model.addAttribute("user", user);
		return "userDetails";
	}

	@PostMapping("/deleteUser")
	@PreAuthorize("hasRole('BOSS')")
	public String denyAuthentication(@RequestParam("userId") long userId, Model model) {
		userManagement.denyAuthenticationById(userId);
		return "redirect:/users";
	}


}