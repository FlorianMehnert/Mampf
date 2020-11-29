package mampf.catalog;

import javax.persistence.Entity;

import org.javamoney.moneta.Money;

@Entity
public class Personal extends Item {
	public static enum Type {
		COOK, SERVICE
	}

	private Type type;

	@SuppressWarnings("unused")
	private Personal(){}

	public Personal(String name, Money price, Domain domain, Category category, String description, Type type) {
		super(name, price, domain, category, description);

		this.type = type;
	}

	public Type getType() {
		return type;
	}
}
