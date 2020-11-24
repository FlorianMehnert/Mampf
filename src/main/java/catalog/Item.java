package catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;

public class Item extends Product{
	private Product product;
	private String description;
	private Domain domain;
	private Category category;


	private Item(String name, Money price, String description, Domain domain, Category category) {
		super(name, price);
		this.product.setName(name);
		this.product.setPrice(price);
		this.description = description;
		this.domain = domain;
		this.category = category;

	}

	public String getName(){
		return this.product.getName();
	}

	public ProductIdentifier getId() {
		return product.getId();
	}

	public String getDescription() {
		return description;
	}

	public Domain getDomain() {
		return domain;
	}

	public Category getCategory() {
		return category;
	}
}
