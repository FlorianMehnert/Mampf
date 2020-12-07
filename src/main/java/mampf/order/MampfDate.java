package mampf.order;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

//import org.salespointframework.catalog.Product;

import java.lang.Comparable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
//import java.util.Objects;

import java.time.LocalDateTime;

@Entity
public class MampfDate implements Comparable<MampfDate> {

	private @Id @GeneratedValue long id;

	public static final Duration EVENTDURATION = Duration.ofHours(2);

	@OneToOne
	private MampfOrder order;
	private LocalDateTime startTime = null;
	private String address = null;

	private String[] days = null;
	private LocalTime time = null;

	@SuppressWarnings("unused")
	private MampfDate() {
	}

	// use as EVENT
	public MampfDate(LocalDateTime startTime, String address) {
		// TODO: add assertcheck
		this.startTime = startTime;
		this.address = address;
	}

	// use as MB
	public MampfDate(String[] days, LocalTime time) {
		// TODO: add assertcheck
		this.days = days;
		this.time = time;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public String getAddress() {
		return address;
	}

	public String[] getDays() {
		return days;
	}

	public LocalTime getTime() {
		return time;
	}

	public long getId() {
		return id;
	}

	public void setOrder(MampfOrder order) {
		this.order = order;
	}

	public boolean equals(MampfDate d) {
		// TODO nullchecking
		return startTime.equals(d.getStartTime()) && address.equals(d.getAddress());
	}

	public boolean hasTimeOverlap(LocalDateTime otherDate) {
		// checks with constant
		// TODO: nullcheck
		// time between events??
		// localdatetime should be immutable...
		return (otherDate.plusHours(MampfDate.EVENTDURATION.toHours())).isAfter(startTime)
				&& (startTime.plusHours(MampfDate.EVENTDURATION.toHours())).isAfter(otherDate);
	}

	public int compareTo(MampfDate d) {
		return startTime.compareTo(d.getStartTime());
	}

	public String toString() {
		String res = "";
		if (startTime != null)
			res += "EventStart: " + startTime.toString();
		if (days != null) {
			res += "Wochentage: ";
			for (String day : days) {
				res += day + " ";
			}
		}

		if (time != null)
			res += "Zeit: " + time.toString();
		if (address != null)
			res += "Anschrift: " + address.toString();

		return res;

	}

}
