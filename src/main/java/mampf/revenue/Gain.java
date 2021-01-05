package mampf.revenue;

import org.salespointframework.catalog.Product;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class Gain extends Product {
	private LocalDateTime time;

	public Gain(LocalDateTime time, MonetaryAmount price) {
		super("noName", price);
		this.time = time;
	}

	public Gain() {

	}

	public LocalDateTime getTime() {
		return time;
	}
}
