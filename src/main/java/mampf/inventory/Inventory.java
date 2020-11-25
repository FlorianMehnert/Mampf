package mampf.inventory;

import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import java.util.List;
import java.util.Optional;

public class Inventory{

	private final UniqueInventory<UniqueInventoryItem> inventory;

	public Inventory(UniqueInventory<UniqueInventoryItem> inventory) {
		this.inventory = inventory;
	}

	public Product findItem(String name){
		for(Product product : listItems()){
			if(product.getName().equals(name)){
				return product;
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

	public List<Product> listItems(){
		return null;
	}
}
