package mampf.catalog;

import javax.persistence.Entity;

import org.javamoney.moneta.Money;

@Entity
public class BreakfastItem extends Item {
	public enum Type {
		DISH, BEVERAGE
	}

	public static final Money BREAKFAST_PRICE = Money.of(4.99, "EUR");

	private Type type;

	@SuppressWarnings("unused")
	private BreakfastItem() {
	}

	public BreakfastItem(
			String name,
			String description,
			Money price,
			Type type
	) {
		super(name, price, Item.Domain.MOBILE_BREAKFAST, Item.Category.FOOD, description);

		this.type = type;
	}

	public Type getType() {
		return type;
	}
}
