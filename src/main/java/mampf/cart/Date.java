package mampf.cart;

import javax.persistence.Entity;
import java.lang.Comparable;
import java.time.LocalDateTime;

@Entity
public class Date implements Comparable<Date>{

	private LocalDateTime startTime=null,endTime=null;
	private String address=null;
	private Domain domain=null;
	
	//use in global space:
	public Date(LocalDateTime startTime, LocalDateTime endTime, String address){
		//TODO: add assertcheck
		this.startTime = startTime; this.endTime = endTime; this.address = address; 
	}
	
	//cart use:
	public Date(Domain d) {
		//TODO: null check
		this.domain = domain;
	}
	
	public LocalDateTime getStartTime() {return startTime;}
	public LocalDateTime getEndTime() {return endTime;}
	public Domain getDomain() {return domain;}
	public String getAddress() {return address;}
	
	public void setDate(LocalDateTime startTime, LocalDateTime endTime, String address) {/*TODO nullcheck*/this.startTime = startTime; this.endTime = endTime; this.address = address; }
	public void setDomain(Domain domain) {/*TODO: nullcheck*/this.domain=domain;}
	
	public boolean hasDate() {return (startTime != null && endTime != null && address != null);}
	
	public boolean equals(Date d) {
		return d.getAddress().equals(address)&&(compareTo(d) == 0);
	}
	public int compareTo(Date d) {
		//sort with equal hours
		//TODO: equal hours
		//TODO: Domain should be compareable
		int c1 = domain.compareTo(d.getDomain()); 
		if(c1 == 0)  
			return d.getStartTime().getTime().compareTo(d.getStartTime().getTime());
		return c1; 
	}
	
}
