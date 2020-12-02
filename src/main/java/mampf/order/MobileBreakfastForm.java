package mampf.order;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

public class MobileBreakfastForm {
	@NotEmpty(message = "id should not be empty") //
	private final String id;

	@NotEmpty(message = "beverage should not be empty") //
	private final String beverage;

	@NotEmpty(message = "food should not be empty") //
	private final String food;

	private final boolean monday;
	private final boolean tuesday;
	private final boolean wednesday;
	private final boolean thursday;
	private final boolean friday;

	private final String time;

	public MobileBreakfastForm(
		String id, 
		String beverage, 
		String food, 
		boolean monday, 
		boolean tuesday, 
		boolean wednesday,
		boolean thursday, 
		boolean friday,
		String time
		) {
		this.id = id;
		this.beverage = beverage;
		this.food = food;
		this.monday = monday;
		this.tuesday = tuesday;
		this.wednesday = wednesday;
		this.thursday = thursday;
		this.friday = friday;

		// hh:mm
		this.time = time;
	}

	public String getId(){
		return id;
	}
	public String getBeverage(){
		return beverage;
	}
	public String getFood(){
		return food;
	}
	public Map<String, Boolean> getDays(){
		Map<String, Boolean> days = new HashMap<>();
		days.put("monday", this.monday);
		days.put("tuesday", this.tuesday);
		days.put("wednesday", this.wednesday);
		days.put("thursday", this.thursday);
		days.put("friday", this.friday);
		return days;
	}
	public LocalTime getTime(){
		return LocalTime.parse(this.time);
	}

}