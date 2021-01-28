package mampf.employee;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import mampf.employee.Employee.Role;

@Controller
public class EmployeeController {

	private final EmployeeManagement employeeManagement;

	EmployeeController(EmployeeManagement employeeManagement){

		Assert.notNull(employeeManagement, "EmployeeManagement should not be null");

		this.employeeManagement = employeeManagement;
	}

	/**
	 * Shows the {@link RegistrationForm} to add a new {@link Employee}.
	 *
	 * @param model
	 * @param form information needed to create a new {@link Employee}
	 * @return /intern/employees/add where the boss can add a new {@link Employee}
	 */
	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees/add")
	public String createEmployee(Model model, RegistrationForm form) {
		model.addAttribute("form", form);
		return "employee_add";
	}

	/**
	 * To create and add a new {@link Employee}.
	 *
	 * @param form information needed to create the {@link Employee}
	 * @param error in case there are errors while filling the form
	 * @return /intern/employees if everything goes well
	 */
	@PostMapping("/intern/employees/add")
	public String registerNew(@Valid @ModelAttribute("form")RegistrationForm form, Errors error){

		if(error.hasErrors()){
			return "employee_add";
		}

		employeeManagement.createEmployee(form);

		return "redirect:/intern/employees";
	}

	/**
	 * Shows all the {@link Employee}s that are in the system and filters by name while
	 * searching for one.
	 *
	 * @param model
	 * @param filter the name which is searched for
	 * @return /intern/employees
	 */
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

	/**
	 * Shows the form to edit an {@link Employee}. One can only edit first and last name.
	 *
	 * @param model
	 * @param id from the {@link Employee} who one wants to edit
	 * @param form to write the new first and last name
	 * @return /intern/employees/edit/ + the id of the want to edit {@link Employee}
	 */
	@GetMapping("/intern/employees/edit/{id}")
	public String editEmployee(Model model, @PathVariable long id, RegistrationForm form){
		model.addAttribute("employee", employeeManagement.searchById(id));
		model.addAttribute("form", form);
		return "employee_edit";
	}

	/**
	 * To edit an {@link Employee}.
	 *
	 * @param id of the want to edit {@link Employee}
	 * @param form to change the first and last name
	 * @return /intern/employees when {@link Employee} is edited
	 */
	@PostMapping("/intern/employees/edit/{id}")
	public String edit(@PathVariable long id, @ModelAttribute("form") RegistrationForm form){
		Employee employee = employeeManagement.searchById(id);
		employeeManagement.editEmployee(employee, form);
		return "redirect:/intern/employees";
	}

	/**
	 * To delete an {@link Employee}.
	 *
	 * @param id of the to be deleted {@link Employee}
	 * @return /intern/employees without the deleted {@link Employee}
	 */
	@GetMapping("/intern/employees/deleteEmployee/{id}")
	public String deleteEmployee(@PathVariable long id){
		employeeManagement.deleteEmployee(id);
		return "redirect:/intern/employees";
	}

	/**
	 * To filter the {@link Employee}s by role (cook or service).
	 *
	 * @param model
	 * @param role which one wants to filter for
	 * @return /intern/employees/filter/ + the role one wants to filter
	 */
	@PreAuthorize("hasRole('BOSS')")
	@GetMapping("/intern/employees/filter/{role}")
	public String filter(Model model, @PathVariable("role") String role){
		Role role1 = Role.valueOf(role.toUpperCase());
		if(role1 != null){
			model.addAttribute("employees", employeeManagement.filterByRole(role1));
			model.addAttribute("role", role);
		}else{
			model.addAttribute("employees", employeeManagement.findAll());
		}
		return "employees";
	}
}
