package mampf.inventory;

import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.math.BigDecimal;

@Entity
public class UniqueMampfItem extends UniqueInventoryItem {
	//@OneToOne
	//private mampf.catalog.Item item;

	public UniqueMampfItem(Item item, Quantity quantity) {
		super(item, quantity);
		//this.item = item;
	}

	public UniqueMampfItem() {
	}

	//public Item getItem() {
	//	return item;
	//}
	
	public Item getItem() {
		return (Item) getProduct();
	}
	
	public void increaseMampfQuantity(int increase) {
		if(increase > 0) {
			increaseQuantity(Quantity.of(increase));
		}else {
			decreaseQuantity(Quantity.of(increase));
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
