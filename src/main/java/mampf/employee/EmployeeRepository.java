package mampf.employee;

import mampf.employee.Employee.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;
import java.util.ArrayList;

/**
 * A repository interface to manage the {@link Employee}s
 */
interface EmployeeRepository extends CrudRepository<Employee, Long>{

	/**
	 * Redeclared {@link CrudRepository#findAll()} to return a {@link Streamable}
	 */
	@Override //
	Streamable<Employee> findAll();

	/**
	 * Searches for the name of an {@link Employee}
	 *
	 * @param name of the {@link Employee} one would like to search for
	 * @return the {@link Employee} with said name
	 */
	default Employee findByName(String name) {
		Streamable<Employee> employees = this.findAll();
		for (Employee currentEmployee : employees) {
			if (currentEmployee.getName().equals(name)) {
				return currentEmployee;
			}
		}
		return null;
	}

	/**
	 * Filter the employees for substrings of the given filter
	 * The filter is case insensitive
	 * @param filter the filter to apply
	 * @return
	 */
	default ArrayList<Employee> filterByNameForSearching(String filter) {
		ArrayList<Employee> employees = new ArrayList<>();
		for (Employee currentEmployee : this.findAll()) {
			if (currentEmployee.getName().toLowerCase().contains(filter.toLowerCase())) {
				employees.add(currentEmployee);
			}
		}
		return employees;
	}

	/**
	 * Finds the {@link Employee}s by their role (cook or service)
	 *
	 * @param role which is looked for
	 * @return the list of the {@link Employee} of that role
	 */
	default ArrayList<Employee> findByRole(Role role){
		if(role == null){
			throw new IllegalArgumentException("role cannot be null");
		}
		ArrayList<Employee> employeesWithRole = new ArrayList<>();
		Streamable<Employee> employees = this.findAll();
		for (Employee currentEmployee : employees) {
			if (currentEmployee.getRole() == role) {
				employeesWithRole.add(currentEmployee);
			}
		}
		return employeesWithRole;
	}
}
