package mampf.employee;

import mampf.order.EventOrder;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Entity
public class Employee {

	public enum Role {
		COOK,
		SERVICE
  	}

  	public static  final Map<String, String> roleTranslations = Map.of(
		  Role.COOK.toString(), "Koch",
		  Role.SERVICE.toString(), "Servicepersonal");

	private String name;

	//List where the booked Employees are assigned to an order
	@ManyToMany(mappedBy = "employees")
	private List<EventOrder> booked;
	private Role role;

	private @Id @GeneratedValue long id;

	@SuppressWarnings("unused")
	private Employee(){}

	public Employee(String name, Role role){
    	this.name = name;
    	this.role = role;
    	this.booked = new ArrayList<>();
	}

	public long getId(){
		return id;
  }
  
  	public String getName() {
    return this.name;
  }

  	public Role getRole() {
    return this.role;
  }

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany
  	public List<EventOrder> getBooked() {
    return this.booked;
  }

	/**
	 * Removes {@link EventOrder}s already fulfilled from the booked list of {@link Employee}s
	 * @param order which was already fulfilled
	 * @return
	 */
	public boolean removeBookedOrder(EventOrder order) {
		 if(!booked.contains(order)) {
			return false;
		 }
		 return booked.remove(order);
  	}

	/**
	 * Sets {@link Employee}s to the status booked when an order requires one
	 * @param order already booked/payed where an {@link Employee} is required
	 * @return true when the {@link Employee} is added to the booked list
	 */
	public boolean setBooked(EventOrder order) {
		try {
		  this.booked.add(order);
		} catch (Exception ex) {
		  ex.printStackTrace();
		  return false;
		}
		return true;
  	}

}
