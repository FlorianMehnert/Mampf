package mampf.employee;

import javax.validation.Valid;

import mampf.user.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmployeeController {

	private final EmployeeManagement employeeManagement;

	EmployeeController(EmployeeManagement employeeManagement){

		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.employeeManagement = employeeManagement;
	}

	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees/add")
	String createEmployee(Model model) {
		return "employee_add";
	}

	@PostMapping("/intern/employees/add")
	String registerNew(@Valid RegistrationForm form, Errors error, RedirectAttributes redirAttrs){

		boolean err = false;
		if(form.getRole() != "COOK" || form.getRole() != "SERVICE"){
			redirAttrs.addFlashAttribute("noType", "This type of employee doesn't exist");
			err = true;
		}
		if(err){
			return "redirect:/intern/employees/add";
		}
		if(error.hasErrors()){
			return "redirect:/intern/employees";
		}

		employeeManagement.createEmployee(form);

		return "redirect:/intern/employees";
	}

	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees")
	String register(Model model){
		model.addAttribute("employees", employeeManagement.findAll());
		return "employees";
	}
}
