package mampf.employee;

import org.salespointframework.quantity.Quantity;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import mampf.order.EventOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Implementation of logic related to {@link Employee}s
 */
@Service
@Transactional
public class EmployeeManagement {

	private final EmployeeRepository employees;

	/**
	 * Creates a new {@link EmployeeManagement} with the given {@link EmployeeRepository}
	 *
	 * @param employees must not be {@literal null}
	 */
	EmployeeManagement(EmployeeRepository employees){
		Assert.notNull(employees, "EmployeeRepository should not be null");

		this.employees = employees;
	}

	/**
	 * Creates a new {@link Employee} using the information given in the {@link RegistrationForm}
	 *
	 * @param form must not be {@literal null}
	 * @return the new {@link Employee}
	 */
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

	/**
	 * Sets an {@link Employee} booked to an {@link EventOrder} that requires it
	 *
	 * @param id of the {@link Employee} who was selected for this {@link EventOrder}
	 * @param order already booked/payed by a {@link mampf.user.User}
	 * @return
	 */
	public boolean setEmployeeBooked(long id, EventOrder order){
		Optional<Employee> employee = employees.findById(id);
		return employee.map(value -> value.setBooked(order)).orElse(false);
	}

	/**
	 * Filters the {@link Employee}s by name
	 * @param filter name the boss is looking for
	 * @return the {@link Employee} with said name
	 */
	public ArrayList<Employee> filterByName(String filter) {
		return employees.filterByNameForSearching(filter);
	}

	/**
	 * Returns all {@link Employee}s currently available in the system
	 *
	 * @return all {@link Employee}s
	 */
	public Streamable<Employee> findAll() {
		return employees.findAll();
	}

	/**
	 * Gives the amount of {@link Employee}s depending on the role (cook or service)
	 *
	 * @param role type of {@link Employee}
	 * @return number of {@link Employee}s from one type of role
	 */
	public Quantity getEmployeeAmount(Employee.Role role) {
	  return Quantity.of(employees.findByRole(role).size());    
	}

	/**
	 * Gets the {@link Employee}s that are not yet booked
	 *
	 * @param fromDate start time of the {@link EventOrder}
	 * @param toDate end time of the {@link EventOrder}
	 * @param role type of {@link Employee}
	 * @return the list of {@link Employee}s that are not yet booked
	 */
	public List<Employee> getFreeEmployees(LocalDateTime fromDate, LocalDateTime toDate,Employee.Role role){
		List<Employee> freeEmployees = new ArrayList<>();
		boolean isFree;
		for(Employee employee: employees.findByRole(role)) {
			isFree = true;
			for (EventOrder bookedOrder : employee.getBooked()){
				if(bookedOrder.hasTimeOverlap(fromDate,toDate)){
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

	/**
	 * Edits the first and last name of an {@link Employee}
	 *
	 * @param employee which is already in the system
	 * @param form using information from the {@link RegistrationForm}
	 */
	public void editEmployee(Employee employee, RegistrationForm form){
		employee.setName(form.getFirstName() + " " + form.getLastName());
	}

	/**
	 * Searches an {@link Employee} by its id
	 *
	 * @param id the number assigned to this {@link Employee} by the system
	 * @return the id of the searched {@link Employee}
	 */
	public Employee searchById(long id){
		for(Employee currentEmployee : employees.findAll()){
			if(currentEmployee.getId() == id){
				return currentEmployee;
			}
		}
		return null;
	}

	/**
	 * Deletes an {@link Employee} when this is not yet booked to an {@link EventOrder}
	 *
	 * @param id of the {@link Employee} to be deleted
	 */
	public void deleteEmployee(long id){
		Employee employee = searchById(id);
		List<EventOrder> orders = employee.getBooked();
		for(EventOrder order : orders){
			if(setEmployeeBooked(id, order)) {
				return;
			}
		}
		employees.deleteById(id);
	}

	/**
	 * Filters the {@link Employee}s depending on their roles (cook or service)
	 *
	 * @param role the type of role the boss is looking for
	 * @return the {@link Employee}s with the same role
	 */
	public ArrayList<Employee> filterByRole(Employee.Role role) {
		return employees.findByRole(role);
	}
}
