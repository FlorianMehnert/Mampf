
package mampf.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class CheckoutForm {

	@DateTimeFormat(pattern = "dd.MM.yyyy")
	@NotNull(message = "{CheckoutForm.startDate.NotNull}")
	private final LocalDate startDate;

	@DateTimeFormat(pattern = "H:m")
	@NotNull(message = "{CheckoutForm.startTime.NotNull}")
	private final LocalTime startTime;

	//@NotEmpty(message = "address empty") // s
	@NotEmpty()
	private final String address;

	@NotEmpty()
	private final String payMethod;

	public CheckoutForm(LocalDate startDate, String address, String payMethod, LocalTime startTime) {
		this.startDate = startDate;
		this.address = address;
		this.payMethod = payMethod;
		this.startTime = startTime;
	}

	public LocalDateTime getStartDateTime() {
		if(startTime == null || startDate == null) {
			return LocalDateTime.now();
		}
		return startDate.atTime(startTime);
	}
	public String getAddress() {
		return address;
	}
	public String getPayMethod() {
		return payMethod;
	}
	public String getStartDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		return startDate != null ? startDate.format(formatter) : LocalDate.now().format(formatter);
	}

	public String getStartTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:m");
		return startTime != null ? startTime.format(formatter) : LocalTime.now().format(formatter);
	}

}
