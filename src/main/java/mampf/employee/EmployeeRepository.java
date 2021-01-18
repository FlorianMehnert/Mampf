package mampf.employee;

import mampf.employee.Employee.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.Stream;

interface EmployeeRepository extends CrudRepository<Employee, Long>{

	@Override //
	Streamable<Employee> findAll();

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
