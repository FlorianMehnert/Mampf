package mampf.cart;

import mampf.catalog.Item;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.salespointframework.catalog.Product;

import java.lang.Comparable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "DATES")
public class Date implements Comparable<Date>{
	
	private @Id @GeneratedValue long id;
	
	private LocalDateTime startTime=null,endTime=null;
	private String address=null;
	private Item.Domain domain=null;
	
	//use in global space:
	public Date(LocalDateTime startTime, LocalDateTime endTime, String address){
		//TODO: add assertcheck
		this.startTime = startTime; this.endTime = endTime; this.address = address; 
	}
	
	//cart use:
	public Date(Item.Domain domain) {
		//TODO: null check
		this.domain = domain;
	}
	
	public LocalDateTime getStartTime() {return startTime;}
	public LocalDateTime getEndTime() {return endTime;}
	public Item.Domain getDomain() {return domain;}
	public String getAddress() {return address;}
	
	public void setDate(/*TODO: nullcheck*/ LocalDateTime startTime, LocalDateTime endTime, String address) {/*TODO nullcheck*/this.startTime = startTime; this.endTime = endTime; this.address = address; }
	public void setDomain(Item.Domain domain) {/*TODO: nullcheck*/this.domain=domain;}
	
	public boolean hasNoDate() {
		return (startTime == null && endTime == null && address == null);}
	
	public boolean equals(Date d) {
		//TODO nullchecking
		boolean aEqual = false;
		if(d.getAddress() == null && address == null )aEqual = true;
		else if(address != null && d.getAddress() != null)if(address.equals(d.getAddress()))aEqual=true;
		
		boolean bEqual = false;
		if(d.getStartTime() == null && startTime == null )bEqual = true;
		else if(startTime != null && d.getStartTime() != null)if(startTime.equals(d.getStartTime()))bEqual=true;
		
		boolean cEqual = false;
		if(d.getEndTime() == null && endTime == null )cEqual = true;
		else if(endTime != null && d.getEndTime() != null)if(endTime.equals(d.getEndTime()))cEqual=true;
		
		boolean dEqual = false;
		if(d.getDomain() == null && domain == null )dEqual = true;
		else if(domain != null && d.getDomain() != null) if(domain.equals(d.getDomain()))dEqual=true;
		
		return (aEqual && bEqual && cEqual && dEqual);
		//if(d.getAddress() != null && address != null)
		//return d.getAddress().equals(address)&&(compareTo(d) == 0);
		//return false;
	}
	public int compareTo(Date d) {
		//sort with equal hours
		//TODO: equal hours
		//TODO: Domain should be compareable
		int c1 = -1;
		if(domain != null && d.getDomain() != null)
			c1 = domain.compareTo(d.getDomain()); 
		if(c1 == 0) {  
			if(d.getStartTime() != null && startTime != null)
			return startTime.compareTo(d.getStartTime());
			else if(d.getStartTime() == null && startTime != null)return -1;
			else if(d.getStartTime() != null && startTime == null)return 1;
		}
		return c1; 
	}
	
	public String toString() {
		String res = domain.name();
		if(startTime != null) res+= startTime.toString();
		return res;
	}

	/*@Override
    public int hashCode() {
		String s1 = "null";
		if(startTime != null)s1 = startTime.toString();
		String s2 = "null";
		if(endTime != null)s1 = endTime.toString();
		String s3 = "null";
		if(address != null)s3 = address;
		
        return Objects.hash(s1, s2, s3, domain);
    } */
	
	public long getId() {
		return id;
	}
}
