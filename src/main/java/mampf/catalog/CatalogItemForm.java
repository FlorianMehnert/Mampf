package mampf.catalog;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

class CatalogItemForm {

	@NotEmpty(message = "Itemname cannot be null") //
	private final String name;

	@NotEmpty(message = "Domain cannot be null") //
	private final String domain;

	@NotEmpty(message = "Catgory cannot be null") //
	private final String category;

	@NotEmpty(message = "Price cannot be null") //
	private final String price;

	private final String description;

	public CatalogItemForm(String name, String domain, String category, String price, String description) {
		this.name = name;
		this.domain = domain;
		this.category = category;
		this.price = price;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDomain(){
		return domain;
	}

	public String getCategory(){
		return category;
	}

	public String getPrice(){
		return price;
	}

	public String getDescription(){
		return description;
	}
}