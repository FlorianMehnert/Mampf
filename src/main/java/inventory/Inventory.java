package inventory;

import catalog.Catalog;
import catalog.Item;
import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.InventoryItemIdentifier;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import java.util.List;
import java.util.Optional;


public class Inventory{

	private final UniqueInventory<UniqueInventoryItem> inventory;
	private final Catalog catalog;

	public Inventory(UniqueInventory<UniqueInventoryItem> inventory, Catalog catalog) {
		this.inventory = inventory;
		this.catalog = catalog;
	}

	public Item findItem(String name){
		for(Item item : listItems()){
			if(item.getName().equals(name)){
				return item;
			}
		}
		return null;
	}
	public void reduceAmount(Product product, Quantity amount){
		assert product.getId() != null;
		Optional<UniqueInventoryItem> theItem = this.inventory.findByProduct(product);

		//TODO: what if theItem is not available
		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(amount));
	}

	public List<Item> listItems(){
		return null;
	}
}
