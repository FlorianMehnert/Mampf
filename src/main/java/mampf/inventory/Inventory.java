package mampf.inventory;

import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface Inventory extends UniqueInventory<UniqueInventoryItem> {
	default Optional<UniqueInventoryItem> findByName(String name) {
		if(name != null){
			for (UniqueInventoryItem item : this.findAll()) {
				if (item.getProduct().getName().equals(name)) {
					return Optional.of(item);
				}
			}
		}
		return Optional.empty();
	}
	default Optional<UniqueInventoryItem> reduceAmount(Product product, Quantity amount) {
		Optional<UniqueInventoryItem> theItem = this.findByProduct(product);
		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(amount));
		return theItem;
	}

	default List<UniqueInventoryItem> findAllAndSort() {
		List<UniqueInventoryItem> list = this.findAll().toList();
		List<UniqueInventoryItem> sortableList = new ArrayList<>(list);
		sortableList.sort(new SortByName());
		return sortableList;
	}

	default List<UniqueInventoryItem> sortByCategory() {
		List<UniqueInventoryItem> list = this.findAll().toList();
		List<UniqueInventoryItem> sortableList = new ArrayList<>(list);
		sortableList.sort(new SortByName());
		return sortableList;
	}

	class SortByName implements Comparator<UniqueInventoryItem> {
		@Override
		public int compare(UniqueInventoryItem a, UniqueInventoryItem b) {
			int comp = a.getProduct().getName().compareTo(b.getProduct().getName());
			if(comp == 0){
				comp = a.getProduct().getPrice().compareTo(b.getProduct().getPrice());
			}
			return comp;
		}
	}
}
