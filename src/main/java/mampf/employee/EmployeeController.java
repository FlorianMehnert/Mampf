package mampf.employee;

import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EmployeeController {

	private final EmployeeManagement employeeManagement;

	EmployeeController(EmployeeManagement employeeManagement){

		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.employeeManagement = employeeManagement;
	}

	@PostMapping("/register")
	String registerNew(@Valid RegistrationForm form, Errors error){

		if(error.hasErrors()){
			return "register";
		}

		employeeManagement.createEmployee(form);

		return "redirect:/";
	}

	@GetMapping("/register")
	String register(Model model, RegistrationForm form){
		model.addAttribute("form", form);
		return "register";
	}
}
