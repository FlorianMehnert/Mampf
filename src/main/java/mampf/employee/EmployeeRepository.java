package mampf.employee;

import java.util.Iterator;
import java.util.ArrayList;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;

import mampf.employee.Employee.Role;

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
