
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

/**
 * Formular class to bind and store checkout fields when buying carts
 *
 */
public class CheckoutForm {

	private Map<String, @Valid String> allStartDates;

	private Map<String, @Valid String> allStartTimes;

	private Map<String, @Valid String> allEndTimes;

	private Map<String, @Valid String> allAddresses;


	@NotEmpty()
	private final String payMethod;

	private Item.Domain domainChoosen;

	private String generalError;

	public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m");
	
	public static final List<String> domainsWithoutForm = List.of(Item.Domain.MOBILE_BREAKFAST.name());
	
	public CheckoutForm(Map<String, String> startDates, String payMethod, Map<String, String> startTimes,
						Map<String, String> endTimes, Map<String, String> allAddresses,String generalError) {
		this.allStartDates = startDates;
		this.allStartTimes = startTimes;
		this.allEndTimes = endTimes;
		this.allAddresses = allAddresses;
		this.payMethod = payMethod;
		this.generalError = generalError;
	}
	
	/**
	 * checks if there are empty fields left, depending on the chosen {@link Item.Domain}
	 * @return {@code true} if valid
	 */
	public boolean hasValidData() {
	    
	    if(domainChoosen == null) {
	        for(Item.Domain domain: getDomains()) {
	            if(!validateDomain(domain))return false;
	        }
	    }else {
	        return validateDomain(domainChoosen);
	    }
	    return true;
	    
	}
	/**
	 * checks if the fields of the domain are filled
	 * <li> there are no fields for {@link Item.Domain#MOBILE_BREAKFAST} required</li>
	 * @param domain
	 * @return {@code true} if valid
	 */
	private boolean validateDomain(Item.Domain domain) {
	    if(domainsWithoutForm.contains(domain.name())) {
	        return true;
	    }
	    try {
          getStartDateTime(domain);
          getEndDateTime(domain);
      }catch (Exception e) {
          return false;
      }
	    
	    return true;
	}
	
	public LocalDateTime getStartDateTime(Item.Domain domain) {
		if(allStartDates == null || !allStartDates.containsKey(domain.name())) {
			return null;
		}
		return LocalDate.parse(allStartDates.get(domain.name()), dateFormatter).
				atTime(LocalTime.parse(allStartTimes.get(domain.name()), timeFormatter));
	}

	public LocalDateTime getEndDateTime(Item.Domain domain) {
		if(allStartDates == null || !allStartDates.containsKey(domain.name())) {
			return null;
		}
		return LocalDate.parse(allStartDates.get(domain.name()), dateFormatter).
				atTime(LocalTime.parse(allEndTimes.get(domain.name()), timeFormatter));
	}
	
	public String getAddress(Item.Domain domain) {
	    if(allAddresses == null || !allAddresses.containsKey(domain.name())) {
	      return null;
	    }
	    return allAddresses.get(domain.name());
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
		LocalTime startTime = LocalTime.parse(getStartTime(domain), timeFormatter);
		LocalTime endTime = LocalTime.parse(getEndTime(domain), timeFormatter);

		return endTime.minusHours(startTime.getHour()).getHour();
	}

	public String getGeneralError() {
		return generalError;
	}
	
	public List<Item.Domain> getDomains() {
		List<Item.Domain> domains = new ArrayList<>();
		if(allStartDates != null) {
		for (Item.Domain domain: Item.Domain.values()){
			if(allStartDates.containsKey(domain.name())) {
				domains.add(domain);
			}
		}}
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
	
	public Map<String, String> getAllAddresses() {
    if(allAddresses == null) {
      allAddresses = new HashMap<>();
    }
    return allAddresses;
  }
	
	public Item.Domain getDomainChoosen() {
		return domainChoosen;
	}

	public void setDomainChoosen(Item.Domain domainChoosen) {
		this.domainChoosen = domainChoosen;
	}
}
