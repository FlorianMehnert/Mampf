package mampf.employee;

import mampf.order.MampfOrder;

import javax.persistence.Id;
import javax.persistence.ManyToMany;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

@Entity
public class Employee {

	public static enum Role {
		COOK,
		SERVICE
  }
  private String name;
  
  @ManyToMany
  private List<MampfOrder> booked;
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

  @ManyToMany
  public List<MampfOrder> getBooked() {
    return this.booked;
  }

  public boolean setBooked(MampfOrder order) {
    try {
      this.booked.add(order);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

}
