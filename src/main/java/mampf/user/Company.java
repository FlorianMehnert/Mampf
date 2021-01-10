package mampf.user;

import net.bytebuddy.utility.RandomString;

import javax.persistence.*;

import com.google.common.annotations.VisibleForTesting;

import mampf.order.EventOrder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "COMPANIES")
public class Company {

	private String name;
	private long bossId;
	//time between booked MB and first possible MB Order(employees can now choose...)
	public static final Duration awaitBreakFastChoiceDuration = Duration.ofDays(3);
	
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
		//MB can only be booked once for each month
		if(canBookNewBreakfast()) {	
			this.breakfastDate = getNextBreakfastDate();
			return true;
		}
		return false;
	}
	/**
	 * estimates if a new BreakfastDate is possible
	 * - if a boss-user can set a new date
	 * returns if bookable for the next month
	 * @return
	 */
	public boolean canBookNewBreakfast() {
		
		return 
		(breakfastDate == null) ||
		LocalDateTime.now().isAfter( 
				LocalDateTime.of(getBreakfastEndDate().get(),//is safely not null
						LocalTime.of(0, 0).minus(awaitBreakFastChoiceDuration)));
		
	}
	/**
	 * estimates if there is a current breakfastDate 
	 * @return
	 */
	public boolean hasBreakfastDate() {
		return breakfastDate != null;
	}
	/**
	 * calculates the next possible breakfastDate
	 * return value is: now + choiceDuration
	 * @return
	 */
	public LocalDate getNextBreakfastDate() {
		return LocalDateTime.now().plus(awaitBreakFastChoiceDuration).toLocalDate();
	}
	
	/**
	 * estimates the end Date for the current breakfastDate
	 * returns empty optional if no breakfastDate is currently available 
	 * returns optional of a localdate with the first day of the next month
	 * @return
	 */
	public Optional<LocalDate> getBreakfastEndDate(){
		//MB will only be booked till the month is over
		if(hasBreakfastDate()) {
			//always the first day of the next month
			return Optional.of(breakfastDate.withDayOfMonth(breakfastDate.lengthOfMonth()).plusDays(1));
		}
		return Optional.empty();
	}
	/**
	 * unit testing purpose 
	 */
	public void resetCompany(){
		breakfastDate = null;
	}
	public Optional<LocalDate> getBreakfastDate() {
		if(breakfastDate != null) {
			return Optional.of(breakfastDate);
		}
		return Optional.empty();
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
