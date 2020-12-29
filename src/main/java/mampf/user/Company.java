package mampf.user;

import net.bytebuddy.utility.RandomString;

import javax.persistence.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "COMPANIES")
public class Company {

	private String name;
	
	//TODO: from a formular instead of static
	//time between booked MB and first possible MB Order(employees can now choose...)
	public static final Duration awaitBreakFastChoiceDuration = Duration.ofDays(2);
	
	@OneToMany
	private List<User> employees = new ArrayList<>();
	
	private LocalDate breakfastDate = null; //use of optional gives hibernate error...
	private String accessCode;

	private @Id @GeneratedValue long id;

	public Company(String name)	{
		this.name = name;
		this.accessCode = RandomString.make(6);
	}

	public Company() {
		this.accessCode = RandomString.make(6);
	}

	public void addEmployee(User employee) {
		employees.add(employee);
	}

	public void removeEmployee(User employee) {
		employees.remove(employee);
	}

	
	public boolean setbreakfastDate(LocalDate breakfastDate) {
		//the given time is already adjusted with awaitoffset
		//MB can only be booked once for each month
		assert breakfastDate != null;
		//TODO: set and reset as one operation
		if(!hasbreakfastDate() || LocalDate.now().getMonthValue()<breakfastDate.getMonthValue()) {	
			this.breakfastDate = breakfastDate;
			return true;
		}
		return false;
	}
	public boolean resetbreakfastDate() {
		//MB can only be reseted when breakfastDate exists and is over 
		if(hasbreakfastDate() && breakfastDate.isBefore(LocalDate.now())) {
			this.breakfastDate = null;
			return true;
		}
		return false;
	}
	public boolean hasbreakfastDate() {
		return breakfastDate != null;
	} 
	public LocalDate getBreakfastDate() {
		return breakfastDate;
	}
	public Optional<LocalDate> getBreakfastEndDate(){
		//MB will only be booked till the month is over
		if(hasbreakfastDate()) {
			return Optional.ofNullable(breakfastDate.withDayOfMonth(breakfastDate.lengthOfMonth()));
		}else {
			return Optional.ofNullable(null);
		}
	}
	public long getId() {
		return id;
	}

	public String getAccessCode() {
		return accessCode;
	}

	public List<User> getEmployees() {
		return employees;
	}

	public String getName() {
		return name;
	}
}
