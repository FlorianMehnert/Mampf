package mampf.employee;

import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import mampf.order.MampfOrder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
		String name = form.getFirstName() + " " + form.getLastName();
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
		Optional<Employee> employee = employees.findById(id);
		return employee.map(value -> value.setBooked(order)).orElse(false);
	}

	public Streamable<Employee> findAll() {
		return employees.findAll();
	}

	public List<Employee> getFreeEmployees(LocalDateTime date, Employee.Role role){
		List<Employee> freeEmployees = new ArrayList<>();
		boolean isFree;
		for(Employee employee: employees.findByRole(role)) {
			//änderung: 
			// fall: es existieren keine bookedOrders -> "es wird nie freie employees geben"
			// fall: date überschneidet mehrere order -> nur einmal hinzufügen
			// -> durch isFree kann nur maximal einmal, falls möglich, der employee geaddet werden
			isFree = true;
			for (MampfOrder bookedOrder : employee.getBooked()){
				if(bookedOrder.getDate().hasTimeOverlap(date)){
					isFree = false;
					break;
				}
			}
			
			if(isFree) {
				freeEmployees.add(employee);
			}
		}
		return freeEmployees;
		
	}
		
}
