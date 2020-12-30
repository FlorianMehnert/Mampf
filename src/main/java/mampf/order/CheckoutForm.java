
package mampf.order;

import mampf.catalog.Item;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class CheckoutForm {

	@NotEmpty()
	private Map<String, @Valid String> allStartDates;

	@NotEmpty()
	private Map<String, @Valid String> allStartTimes;

	private Map<String, List<@Valid String>> allErrors;
	
	private List<Item.Domain> domains;


	@NotEmpty()
	private final String payMethod;

	private String domainChoosen;

	private String generalError;
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m");

	public CheckoutForm(Map<String, String> startDates, String payMethod, Map<String, String> startTimes, String generalError,Map<String,List<String>> allErrors) {
		this.allStartDates = startDates;
		this.allStartTimes = startTimes;
		this.allErrors = allErrors;
		this.payMethod = payMethod;
		this.generalError = generalError;
	}

	public LocalDateTime getStartDateTime(Item.Domain domain) {
		if(allStartDates == null || !allStartDates.containsKey(domain.name())) {
			return null;
		}
		return LocalDate.parse(allStartDates.get(domain.name()), dateFormatter).atTime(LocalTime.parse(allStartTimes.get(domain.name()), timeFormatter));
	}

	public String getPayMethod() {
		return payMethod;
	}
	public String getStartDate(String domain) {
		if(allStartDates == null || allStartDates.get(domain) == null) {
			return LocalDate.now().format(dateFormatter);
		}
		return allStartDates.get(domain);
	}

	public String getToday() {
		return LocalDate.now().format(dateFormatter);
	}

	public String getTimeNow() {
		return LocalTime.now().format(timeFormatter);
	}
	public String getStartTime(String domain) {
		return allStartTimes != null ? allStartTimes.get(domain) : LocalTime.now().format(timeFormatter);
	}

	public String getGeneralError() {
		return generalError;
	}

	public List<Item.Domain> getDomains() {
		domains = new ArrayList<>();
		for (Item.Domain domain: Item.Domain.values()){
			if(allStartDates.containsKey(domain.name())) {
				domains.add(domain);
			}
		}
		return domains;
	}
	
	public Map<String,List<String>> getAllErrors() {
		if(allStartDates == null) {
			allErrors = new HashMap<>();
		}
		return allErrors;
	}

	public Map<String, String> getAllStartDates() {
		if(allStartDates == null) {
			allStartDates = new HashMap<>();
		}
		return allStartDates;
	}

	public Map<String, String> getAllStartTimes() {
		if(allStartTimes == null) {
			allStartTimes = new HashMap<>();
		}
		return allStartTimes;
	}

	public void setAllStartDates(Map<String, String> dates) {
		allStartDates = dates;
	}

	public String getDomainChoosen() {
		return domainChoosen;
	}
	
	public void setAllErrors(Map<String,List<String>> allErrors) {
		this.allErrors = allErrors;
	}
	
	public void setDomainChoosen(String domainChoosen) {
		this.domainChoosen = domainChoosen;
	}
}
