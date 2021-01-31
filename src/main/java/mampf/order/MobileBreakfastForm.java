package mampf.order;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;


import mampf.catalog.BreakfastItem;

public class MobileBreakfastForm {
	private final BreakfastItem beverage;

	private final BreakfastItem dish;

	private final boolean monday;
	private final boolean tuesday;
	private final boolean wednesday;
	private final boolean thursday;
	private final boolean friday;


	public MobileBreakfastForm(
			BreakfastItem beverage,
			BreakfastItem dish,
			String monday,
			String tuesday,
			String wednesday,
			String thursday,
			String friday
	) {
		this.beverage = beverage;
		this.dish = dish;
		this.monday = monday != null;
		this.tuesday = tuesday != null;
		this.wednesday = wednesday != null;
		this.thursday = thursday != null;
		this.friday = friday != null;

	}

	public BreakfastItem getBeverage() {
		return beverage;
	}

	public BreakfastItem getDish() {
		return dish;
	}

	public Map<String, Boolean> getDays() {
		Map<String, Boolean> days = new HashMap<>();
		days.put("monday", this.monday);
		days.put("tuesday", this.tuesday);
		days.put("wednesday", this.wednesday);
		days.put("thursday", this.thursday);
		days.put("friday", this.friday);
		return days;
	}
}