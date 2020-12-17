package mampf.inventory;

import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.math.BigDecimal;

@Entity
public class UniqueMampfItem extends UniqueInventoryItem {
	@OneToOne
	private mampf.catalog.Item item;
	//private Quantity quantity;

	public UniqueMampfItem(Item item, Quantity quantity) {
		super(item, quantity);
		this.item = item;
		//this.quantity = quantity;
	}

	public UniqueMampfItem() {
	}

	public Item getItem() {
		return item;
	}

	/*@Override
	public Quantity getQuantity() {
		return quantity;
	}*/
	
	
	public void increaseMampfQuantity(int increase) {
		if(increase > 0) {
			increaseQuantity(Quantity.of(increase));
		}else {
			decreaseQuantity(Quantity.of(increase));
		}
		//this.quantity = Quantity.of((long) this.quantity.getAmount().intValue() + increase);
	}
	
	public BigDecimal getAmount() {
		return getQuantity().getAmount();
	}
	

	public mampf.catalog.Item.Domain getDomain() {
		return this.item.getDomain();
	}

	public mampf.catalog.Item.Category getCategory() {
		return this.item.getCategory();
	}
}
