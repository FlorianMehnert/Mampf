package mampf.employee;

import java.util.Iterator;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;

interface EmployeeRepository extends CrudRepository<Employee, Long>{

	@Override //
	Streamable<Employee> findAll();

	default Employee findByName(String name) {
		Streamable<Employee> employees = this.findAll();
		Iterator<Employee> iterator = employees.iterator();
		while (iterator.hasNext()) {
			Employee currentEmployee = iterator.next();
			if (currentEmployee.getName() == name) {
				return currentEmployee;
			}
		}
		return null;
	};
}
