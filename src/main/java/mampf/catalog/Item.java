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
		EVENTCATERING{
			@Override
			public String toString() {
				return "Eventcatering";
			}
		},
		PARTYSERVICE{
			@Override
			public String toString() {
				return "Partyservice";
			}
		},
		MOBILE_BREAKFAST{
			@Override
			public String toString() {
				return "Mobile Breakfast";
			}
		},
		RENT_A_COOK{
			@Override
			public String toString() {
				return "Rent a Cook";
			}
		}
	}

	public static enum Category {
		FOOD{
			@Override
			public String toString() {
				return "Food";
			}
		},
		DECORATION{
			@Override
			public String toString() {
				return "Decoration";
			}
		},
		EQUIPMENT{
			@Override
			public String toString() {
				return "Equipment";
			}
		},
		PERSONEL{
			@Override
			public String toString() {
				return "Personel";
			}
		},
		BUFFET{
			@Override
			public String toString() {
				return "Buffet";
			}
		},
		DINNER_EVENT{
			@Override
			public String toString() {
				return "Dinner Event";
			}
		},
		NONE{
			@Override
			public String toString() {
				return "";
			}
		},
		SPECIAL_OFFERS{
			@Override
			public String toString() {
				return "Special Offers";
			}
		}
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
