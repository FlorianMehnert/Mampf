package mampf.lib;

import java.time.DayOfWeek;
import java.util.Collection;

public enum Days {
	MONDAY("Montag", "Mo", "monday"),
	TUESDAY("Dienstag", "Di", "tuesday"),
	WEDNESDAY("Mittwoch", "Mi", "wednesday"),
	THURSDAY("Donnerstag", "Do", "thursday"),
	FRIDAY("Freitag", "Fr", "friday");

	private final String fullname;
	private final String abbriviation;
	private final String value;

	//maybe use a better solution to compare DayOfWeek with Days
	public static String getFullNames(Collection<DayOfWeek> days) {
		StringBuilder res = new StringBuilder();
		for (Days day : Days.values()) {
			for (DayOfWeek weekDay : days) {
				if (day.getValue().equalsIgnoreCase(weekDay.name())) {
					res.append(day.getFullName()).append(" ");
					break;
				}
			}
		}
		return res.toString();
	}

	Days(String fullname, String abbriviation, String value) {
		this.fullname = fullname;
		this.abbriviation = abbriviation;
		this.value = value;
	}

	public String getFullName() {
		return this.fullname;
	}

	public String getAbbriviation() {
		return this.abbriviation;
	}

	public String getValue() {
		return this.value;
	}
}
