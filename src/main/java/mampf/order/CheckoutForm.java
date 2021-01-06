
package mampf.order;

import mampf.catalog.Item;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
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

	@NotEmpty()
	private Map<String, @Valid String> allEndTimes;

	private ArrayList<String> domainsForCheckout;

	private List<Item.Domain> domains;


	@NotEmpty()
	private final String payMethod;

	private String domainChoosen;

	private String generalError;

	public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m");
	
	public final List<String> domainsWithoutForm = List.of(Item.Domain.MOBILE_BREAKFAST.name());
	
	public CheckoutForm(Map<String, String> startDates, String payMethod, Map<String, String> startTimes, Map<String, String> endTimes, String generalError,ArrayList<String> domainsForCheckout) {
		this.allStartDates = startDates;
		this.allStartTimes = startTimes;
		this.allEndTimes = endTimes;
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

	public LocalDateTime getEndDateTime(Item.Domain domain) {
		if(allStartDates == null || !allStartDates.containsKey(domain.name())) {
			return null;
		}
		return LocalDate.parse(allStartDates.get(domain.name()), dateFormatter).atTime(LocalTime.parse(allEndTimes.get(domain.name()), timeFormatter));
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

	public String getEndTime(String domain) {
		if(allEndTimes == null || allEndTimes.get(domain) == null) {
			return LocalTime.now().plusHours(2).format(timeFormatter);
		}
		return allEndTimes.get(domain);
	}

	public int getDurationOfDomain(String domain) {
		LocalTime startTime = LocalTime.parse(getStartTime(domain), timeFormatter);;
		LocalTime endTime = LocalTime.parse(getEndTime(domain), timeFormatter);

		return endTime.minusHours(startTime.getHour()).getHour();
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

	public Map<String, String> getAllEndTimes() {
		if(allEndTimes == null) {
			allEndTimes = new HashMap<>();
		}
		return allEndTimes;
	}

	public String getDomainChoosen() {
		return domainChoosen;
	}

	public void setDomainChoosen(String domainChoosen) {
		this.domainChoosen = domainChoosen;
	}
}
