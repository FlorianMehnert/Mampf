package mampf.user;

import net.bytebuddy.utility.RandomString;

import javax.persistence.*;

import mampf.order.EventOrder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "COMPANIES")
public class Company {

	private String name;
	private long bossId;
	//TODO: from a formular instead of static
	//time between booked MB and first possible MB Order(employees can now choose...)
	public static final Duration awaitBreakFastChoiceDuration = Duration.ofDays(2);
	
	@OneToMany
	private List<User> employees = new ArrayList<>();
	
	private LocalDate breakfastDate = null; //use of optional gives hibernate error...
	private String accessCode;

	private @Id @GeneratedValue long id;

	public Company(String name,long bossId)	{
		this.name = name;
		this.bossId = bossId;
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

	
	public boolean setBreakfastDate() {
		LocalDate breakfastDate = LocalDateTime.now().plus
				(EventOrder.EVENTDURATION).toLocalDate();
		//MB can only be booked once for each month
		assert breakfastDate != null;
		//TODO: set and reset as one operation
		if(canBookNewBreakfast()) {	
			this.breakfastDate = breakfastDate;
			return true;
		}
		return false;
	}
	
	public boolean resetBreakfastDate() {
		//MB can only be reseted when breakfastDate exists and is over 
		if(hasBreakfastDate() && breakfastDate.isBefore(LocalDate.now())) {
			this.breakfastDate = null;
			return true;
		}
		return false;
	}
	public boolean canBookNewBreakfast() {
		return !hasBreakfastDate() || LocalDate.now().getMonthValue()<breakfastDate.getMonthValue();
	}
	public boolean hasBreakfastDate() {
		return breakfastDate != null;
	} 
	public Optional<LocalDate> getBreakfastDate() {
		if(hasBreakfastDate()) {
			return Optional.of(breakfastDate);
		}
		return Optional.empty();
	}
	public Optional<LocalDate> getBreakfastEndDate(){
		//MB will only be booked till the month is over
		if(hasBreakfastDate()) {
			//always the first day of the next month
			return Optional.ofNullable(breakfastDate.withDayOfMonth(breakfastDate.lengthOfMonth()).plusDays(1));
		}
		return Optional.ofNullable(null);
		
	}
	
	public void setBossId(long bossId) {
		this.bossId = bossId;
	}
	
	public long getId() {
		return id;
	}
	
	public long getBossId() {
		return bossId;
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
