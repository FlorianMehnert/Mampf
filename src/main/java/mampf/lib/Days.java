package mampf.lib;

public enum Days {
	MONDAY("Montag", "Mo", "monday"),
	TUESDAY("Dienstag", "Di", "tuesday"),
	WEDNESDAY("Mittwoch", "Mi", "wednesday"),
	THURSDAY("Donnerstag", "Do", "thursday"),
	FRIDAY("Freitag", "Fr", "friday");

	private final String fullname;
	private final String abbriviation;
	// TODO: value should be removed, once order logic is changed to accept integer instead of strings
	private final String value;

	private Days(String fullname, String abbriviation, String value){
		this.fullname = fullname;
		this.abbriviation = abbriviation;
		this.value = value;
	}

	public String getFullName(){
		return this.fullname;
	}

	public String getAbbriviation(){
		return this.abbriviation;
	}

	public String getValue(){
		return this.value;
	}
}
