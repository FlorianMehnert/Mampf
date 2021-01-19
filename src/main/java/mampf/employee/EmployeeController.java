package mampf.employee;

import javax.validation.Valid;

import org.springframework.data.util.Streamable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmployeeController {

	private final EmployeeManagement employeeManagement;

	EmployeeController(EmployeeManagement employeeManagement){

		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.employeeManagement = employeeManagement;
	}

	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees/add")
	public String createEmployee(Model model, RegistrationForm form) {
		model.addAttribute("form", form);
		return "employee_add";
	}

	@PostMapping("/intern/employees/add")
	public String registerNew(@Valid @ModelAttribute("form")RegistrationForm form, Errors error){
		/*if(!form.getRole().equals("COOK") && !form.getRole().equals("SERVICE")){
			error.rejectValue("role", "This type of employee does not exist");
		}*/

		if(error.hasErrors()){
			return "employee_add";
		}

		employeeManagement.createEmployee(form);

		return "redirect:/intern/employees";
	}

	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees")
	public String register(Model model, @RequestParam(required = false) String filter){
		if(filter != null) {
			model.addAttribute("employees", employeeManagement.filterByName(filter));
			model.addAttribute("filter", filter);
		}else{
			model.addAttribute("employees", employeeManagement.findAll());
			model.addAttribute("filter", "");
		}
		return "employees";
	}
}
