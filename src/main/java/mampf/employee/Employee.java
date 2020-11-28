package mampf.employee;

import org.salespointframework.useraccount.UserAccount;

import javax.persistence.Id;

import java.util.ArrayList;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToOne;

@Entity
public class Employee {

	public static enum Role {
		COOK,
		SERVICE
  }
  private String name;
  private ArrayList<Date> booked;
  private Role role;

	private @Id @GeneratedValue long id;

	public Employee(String name, Role role){
    this.name = name;
    this.role = role;
    this.booked = new ArrayList<Date>();
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

  public ArrayList<Date> getBooked() {
    return this.booked;
  }

  public boolean setBooked(Date date) {
    try {
      this.booked.add(date);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

}
