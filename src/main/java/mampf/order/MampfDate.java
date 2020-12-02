package mampf.order;


//import mampf.catalog.Item;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
//import javax.persistence.Table;
import javax.persistence.OneToOne;

//import org.salespointframework.catalog.Product;

import java.lang.Comparable;
import java.time.LocalDateTime;
//import java.util.Objects;

@Entity
public class MampfDate implements Comparable<MampfDate>{
	
	private @Id @GeneratedValue long id;
	
	@OneToOne
	private MampfOrder order;
	private LocalDateTime startTime=null,endTime=null;
	private String address=null;
	

	@SuppressWarnings("unused")
	private MampfDate(){}
	public MampfDate(LocalDateTime startTime, LocalDateTime endTime, String address){
		//TODO: add assertcheck
		this.startTime = startTime; this.endTime = endTime; this.address = address; 
	}
	
	//cart use:
	//public Date(Item.Domain domain) {
	//	//TODO: null check
	//	this.domain = domain;
	//}
	
	public LocalDateTime getStartTime() {return startTime;}
	public LocalDateTime getEndTime() {return endTime;}
	//public Item.Domain getDomain() {return domain;}
	public String getAddress() {return address;}
	
	//public void setDate(/*TODO: nullcheck*/ LocalDateTime startTime, LocalDateTime endTime, String address) {/*TODO nullcheck*/this.startTime = startTime; this.endTime = endTime; this.address = address; }
	//public void setDomain(Item.Domain domain) {/*TODO: nullcheck*/this.domain=domain;}
	
	//public boolean hasNoDate() {return (startTime == null && endTime == null && address == null);}
	
	public void setOrder(MampfOrder order) {this.order = order;}
	public boolean equals(MampfDate d) {
		//TODO nullchecking
		return startTime.equals(d.getStartTime()) && endTime.equals(d.getEndTime()) && address.equals(d.getAddress());

	}
	public boolean hasTimeOverlap(LocalDateTime fromDate,LocalDateTime toDate) {
		//returns true if time overlapping
		//TODO: nullcheck
		//time between events??
		
		
		return toDate.isAfter(startTime) && endTime.isAfter(fromDate);
	}
	public int compareTo(MampfDate d) {
		
		return startTime.compareTo(d.getStartTime());	
	}
	
	public String toString() {
		return "start: "+startTime.toString()+", end: "+endTime.toString()+", address: "+address;
	}
	
	public long getId() {
		return id;
	}
}
