package mampf.inventory;

import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import java.util.Optional;

// public class Inventory{

// 	private final UniqueInventory<UniqueInventoryItem> inventory;

// 	public Inventory(UniqueInventory<UniqueInventoryItem> inventory) {
// 		this.inventory = inventory;
// 	}

// 	public Product findItem(String name){
// 		for(Product product : listItems()){
// 			if(product.getName().equals(name)){
// 				return product;
// 			}
// 		}
// 		return null;
// 	}

// 	public void reduceAmount(Product product, Quantity amount){
// 		assert product.getId() != null;
// 		Optional<UniqueInventoryItem> theItem = this.inventory.findByProduct(product);

// 		//TODO: what if theItem is not available
// 		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(amount));
// 	}

// 	public List<Product> listItems(){
// 		return null;
// 	}
// }

public interface Inventory extends UniqueInventory<UniqueInventoryItem> {
	default Optional<UniqueInventoryItem> findByName(String name){
		assert name != null;
		for (UniqueInventoryItem item: this.findAll()){
			if(item.getProduct().getName().equals(name)) return Optional.of(item);
		}
		return Optional.empty();
	}

	default void reduceAmount(Product product, Quantity amount) {
		assert product.getId() != null;
		Optional<UniqueInventoryItem> theItem = this.findByProduct(product);
		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(amount));
	}
}
