package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;

import javax.persistence.Entity;

@Entity
public class Item extends Product {

	public String description;
	private Domain domain;
	private Category category;

	public enum Domain {
		EVENTCATERING,
		PARTYSERVICE,
		MOBILE_BREAKFAST,
		RENT_A_COOK
	}

	public enum Category {
		FOOD,
		DECORATION,
		EQUIPMENT,
		STAFF,
		BUFFET,
		DINNER_EVENT,
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

	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public String toString() {
		return "name: " + getName() + " price: " + getPrice() + " domain: " + getDomain() + " category: "
				+ getCategory();
	}
	
	public String getDescription() {
		return description;
	}
	
	public Domain getDomain() {
		return this.domain;
	}

	public Category getCategory() {
		return this.category;
	}

}
