package mampf.employee;

import org.salespointframework.useraccount.UserAccount;

import javax.persistence.Id;

import java.util.ArrayList;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

@Entity
public class Employee {

	public static enum Role {
		COOK,
		SERVICE
  }
  private String name;
  private ArrayList<LocalDateTime> booked;
  private Role role;

	private @Id @GeneratedValue long id;

	@SuppressWarnings("unused")
	private Employee(){};
	public Employee(String name, Role role){
    	this.name = name;
    	this.role = role;
    	this.booked = new ArrayList<LocalDateTime>();
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

  public ArrayList<LocalDateTime> getBooked() {
    return this.booked;
  }

  public boolean setBooked(LocalDateTime date) {
    try {
      this.booked.add(date);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

}
