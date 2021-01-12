package mampf.inventory;

import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
public class UniqueMampfItem extends UniqueInventoryItem {

	public UniqueMampfItem(Item item, Quantity quantity) {
		super(item, quantity);
	}

	public UniqueMampfItem() {
	}

	
	public Item getItem() {
		return (Item) getProduct();
	}
	
	public void increaseMampfQuantity(int increase) {
		if (increase > 0) {
			increaseQuantity(Quantity.of(increase));
		}
	}

	public void decreaseMampfQuantity(int decrease) {
		if(decrease > 0) {
			decreaseQuantity(Quantity.of(decrease));
		}
	}
	
	public BigDecimal getAmount() {
		return getQuantity().getAmount();
	}
	

	public mampf.catalog.Item.Domain getDomain() {
		return getItem().getDomain();
	}

	public mampf.catalog.Item.Category getCategory() {
		return getItem().getCategory();
	}
}
