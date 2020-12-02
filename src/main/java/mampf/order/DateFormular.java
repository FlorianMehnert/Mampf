
package mampf.order;

import java.text.DateFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.NotEmpty;

import org.springframework.format.annotation.DateTimeFormat;

public class DateFormular {
	
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	//@NotEmpty(message = "startDate empty") //
	private LocalDateTime startDate; 
	
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	//@NotEmpty(message = "endDate empty") //
	private LocalDateTime endDate;
	
	//@NotEmpty(message = "address empty") // s
	private String address;

	private String payMethod;
	
	public DateFormular(LocalDateTime startDate, LocalDateTime endDate, String address,String payMethod) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.address = address;
		this.payMethod = payMethod;
	}

	public LocalDateTime getStartDate() {return startDate;}
	public LocalDateTime getEndDate() {return endDate;}
	public String getAddress() {return address;}
	public String getPayMethod() {return payMethod;}
	
	public boolean invalid() {
		return endDate.isBefore(startDate);
	}
	
	/*
	public void setStartDate(LocalDateTime startDate) {this.startDate = startDate;}
	public void setEndDate(LocalDateTime endDate) {this.endDate = endDate;}
	public void setAddress(String address) {this.address = address;}
	*/
}
