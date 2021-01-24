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
    
    public static final Duration awaitBreakFastChoiceDuration = Duration.ofDays(3);

    @OneToMany
    private List<User> employees = new ArrayList<>();

    private LocalDate breakfastDate = null; // use of optional gives hibernate error...
    private String accessCode;

    private @Id @GeneratedValue long id;

    public Company(String name, long bossId) {
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
    /**
     * books mobile breakfast.</br>
     * when booking mobile breakfast, this company will set a fixed {@link LocalDate} which is also called breakfastDate.</br>
     * employees can then order their mobile breakfast - choice till the breakfastDate is no longer available.</br>
     * booking mobile breakfast will fail, when there is already a available breakfastDate.
     * 
     * @return {@code true} if booking of mobile breakfast was successful
     */
    public boolean setBreakfastDate() {
        if (canBookNewBreakfast()) {
            this.breakfastDate = getNextBreakfastDate();
            return true;
        }
        return false;
    }

    /**
     * estimates if the boss of this company can book a new mobile breakfast.</br>
     * the boss can book mobile breakfast, when there is no current breakfast Date.
     * @return {@code true} if the boss could book mobile breakfast
     */
    public boolean canBookNewBreakfast() {

        return (breakfastDate == null) || 
                LocalDateTime.now().isAfter(LocalDateTime.of(breakfastDate,
                LocalTime.of(0, 0).minus(awaitBreakFastChoiceDuration)));

    }

    /**
     * estimates if a employee of this company could order mobile breakfast.</br>
     * a employee could book mb, when there is a current breakfast Date.
     * @return {@code true} if the employee could order mobile breakfast.
     */
    public boolean hasBreakfastDate() {
        return (breakfastDate != null) && 
                LocalDateTime.now().isBefore(LocalDateTime.of(breakfastDate,LocalTime.of(0,0)));
    }

    /**
     * calculates the next possible breakfast Date as {@link LocalDate} and returns it. </br>
     * <b>return time.now + awaitBreakFastChoiceDuration</b>
     * @return 
     */
    public LocalDate getNextBreakfastDate() {
        return LocalDateTime.now().plus(awaitBreakFastChoiceDuration).toLocalDate();
    }

    /**
     * estimates the end Date for the current booked mobile breakfast (breakfastDate).</br> 
     * returns {@code empty} {@link Optional} if no breakfastDate is currently available. </br>
     * or returns {@link Optional} of a {@link LocalDate} with the first day of the next month.
     * @return 
     */
    public Optional<LocalDate> getBreakfastEndDate() {
        // MB will only be booked till the month is over
        if (hasBreakfastDate()) {
            return Optional.of(breakfastDate.withDayOfMonth(breakfastDate.lengthOfMonth()).plusDays(1));
        }
        return Optional.empty();
    }

    /**
     * unit testing purpose
     */
    public void resetCompany() {
        breakfastDate = null;
    }

    public Optional<LocalDate> getBreakfastDate() {
        if (breakfastDate != null) {
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
