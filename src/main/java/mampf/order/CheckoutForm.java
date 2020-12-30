
package mampf.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mampf.catalog.Item;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class CheckoutForm {

	@NotEmpty()
	private Map<String, @Valid String> allStartDates;

	@NotEmpty()
	private Map<String, @Valid String> allStartTimes;

	private ArrayList<String> domainsForCheckout;

	private List<Item.Domain> domains;


	@NotEmpty()
	private final String payMethod;

	private String domainChoosen;

	private String generalError;
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m");

	public CheckoutForm(Map<String, String> startDates, String payMethod, Map<String, String> startTimes, String generalError, ArrayList<String> domainsForCheckout) {
		this.allStartDates = startDates;
		this.allStartTimes = startTimes;
		this.payMethod = payMethod;
		this.generalError = generalError;
		this.domainsForCheckout = domainsForCheckout;
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

	public void setDomainChoosen(String domainChoosen) {
		this.domainChoosen = domainChoosen;
	}
}
