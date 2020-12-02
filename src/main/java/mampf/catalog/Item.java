package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import javax.persistence.Entity;

@Entity
public class Item extends Product {

	public String description;
	private Domain domain;
	private Category category;

	public static enum Domain {
		EVENTCATERING,
		PARTYSERVICE,
		MOBILE_BREAKFAST,
		RENT_A_COOK
	}

	public static enum Category {
		FOOD,
		DECORATION,
		EQUIPMENT,
		STAFF,
		BUFFET,
		DINNER_EVENT,
		NONE,
		SPECIAL_OFFERS
	}

	@SuppressWarnings({"unused", "deprecation"})
	public Item(){}

	public Item(
		String name,
		Money price,
		Domain domain,
		Category category,
		String description
		){
		super (name, price);

		this.domain = domain;
		this.category = category;
		this.description = description;
	}

	public Domain getDomain() {
		return this.domain;
	}

	public Category getCategory() {
		return this.category;
	}

}
