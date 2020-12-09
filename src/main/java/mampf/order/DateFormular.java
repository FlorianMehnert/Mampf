
package mampf.order;

import java.time.LocalDateTime;


import org.springframework.format.annotation.DateTimeFormat;

public class DateFormular {
	
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	//@NotEmpty(message = "startDate empty") //
	private LocalDateTime startDate; 
	
	//@NotEmpty(message = "address empty") // s
	private String address;

	private String payMethod;
	
	public DateFormular(LocalDateTime startDate,
						String address, 
						String payMethod) {
		this.startDate = startDate;
		this.address = address;
		this.payMethod = payMethod;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}
	public String getAddress() {
		return address;
	}
	public String getPayMethod() {
		return payMethod;
	}
	
	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
}
