
package mampf.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class CheckoutForm {

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "{CheckoutForm.startDate.NotNull}")
	private final LocalDate startDate;

	@DateTimeFormat(pattern = "H:m")
	@NotNull(message = "{CheckoutForm.startTime.NotNull}")
	private final LocalTime startTime;

	//@NotEmpty(message = "address empty") // s

	@NotEmpty()
	private final String payMethod;

	private String generalError;
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("H:m");

	public CheckoutForm(LocalDate startDate, String payMethod, LocalTime startTime, String generalError) {
		this.startDate = startDate;
		this.payMethod = payMethod;
		this.startTime = startTime;
		this.generalError = generalError;
	}

	public LocalDateTime getStartDateTime() {
		if(startTime == null || startDate == null) {
			return LocalDateTime.now();
		}
		return startDate.atTime(startTime);
	}
	public String getPayMethod() {
		return payMethod;
	}
	public String getStartDate() {
		return startDate != null ? startDate.format(formatter) : LocalDate.now().format(formatter);
	}
	public String getToday() {
		return LocalDate.now().format(formatter);
	}

	public String getStartTime() {
		return startTime != null ? startTime.format(formatterTime) : LocalTime.now().format(formatterTime);
	}

	public String getGeneralError() {
		return generalError;
	}
}
