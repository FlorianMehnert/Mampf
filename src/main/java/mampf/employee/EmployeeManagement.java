package mampf.employee;



import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import mampf.order.MampfOrder;

@Service
@Transactional
public class EmployeeManagement {

	private final EmployeeRepository employees;

	EmployeeManagement(EmployeeRepository employees){
		Assert.notNull(employees, "EmployeeRepository should not be null");

		this.employees = employees;
	}

	public boolean createEmployee(RegistrationForm form){
		Assert.notNull(form, "Registration form should not be null");
		String name = form.getFirst_name() + " " + form.getLast_name();
		if(employees.findByName(name) != null){
			return false;
		}

		employees.save(
			new Employee(
				name,
				mampf.employee.Employee.Role.valueOf(form.getRole())
			)
		);
		return true;
	}

	public boolean setEmployeeBooked(long id, MampfOrder order){
		Employee employee = employees.findById(id).get();
		if(employee == null){
			return false;
		}
		employee.setBooked(order);
		return true;
	}

	public Streamable<Employee> findAll() {
		return employees.findAll();
	}
}
