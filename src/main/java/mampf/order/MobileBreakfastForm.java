package mampf.order;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;

public class MobileBreakfastForm {
	@NotEmpty(message = "beverage should not be empty") //
	private final String beverage;

	@NotEmpty(message = "dish should not be empty") //
	private final String dish;

	@Nullable
	private final boolean monday;
	@Nullable
	private final boolean tuesday;
	@Nullable
	private final boolean wednesday;
	@Nullable
	private final boolean thursday;
	@Nullable
	private final boolean friday;

	private final String time;

	public MobileBreakfastForm(
		String beverage, 
		String dish,
		String monday, 
		String tuesday, 
		String wednesday,
		String thursday, 
		String friday,
		String time
		) {
		this.beverage = beverage;
		this.dish = dish;
		if(monday != null)
			this.monday = true;
		else
			this.monday = false;
		if(tuesday != null)
			this.tuesday = true;
		else
			this.tuesday = false;
		if(wednesday != null)
			this.wednesday = true;
		else
			this.wednesday = false;
		if(thursday != null)
			this.thursday = true;
		else
			this.thursday = false;
		if(friday != null)
			this.friday = true;
		else
			this.friday = false;

		// hh:mm
		this.time = time;
	}
	public String getBeverage(){
		return beverage;
	}
	public String getDish(){
		return dish;
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