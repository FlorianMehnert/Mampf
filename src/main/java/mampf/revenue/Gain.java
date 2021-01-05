package mampf.revenue;

import org.salespointframework.catalog.Product;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Gain {
	private Integer amount;
	private Product product;

	public Gain(Product product, Integer amount) {
		this.product = product;
		this.amount = amount;
	}

	public Product getProduct() {
		return product;
	}

	public Integer getAmount() {
		return amount;
	}
}
