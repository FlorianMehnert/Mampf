package mampf.inventory;

import mampf.Util;
import mampf.catalog.Item;
import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.quantity.Quantity;

import java.util.*;

public interface Inventory extends UniqueInventory<UniqueMampfItem> {


	default Optional<UniqueMampfItem> findByName(String name) {
		if (name != null) {
			for (UniqueMampfItem item : this.findAll()) {
				if (item.getProduct().getName().equals(name)) {
					return Optional.of(item);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * reduces some {@link Item} by some Quantity
	 * @param item which product should be reduced
	 * @param amount by what amount should the product be reduced
	 */
	default Optional<UniqueMampfItem> reduceAmount(Item item, Quantity amount) {
		UniqueMampfItem theItem = this.findByProduct(item).get();
		Quantity quantity = theItem.getQuantity();
		this.delete(theItem);
		UniqueMampfItem newItem = new UniqueMampfItem(item, quantity.subtract(amount));
		this.save(newItem);
		return Optional.of(newItem);
	}

	/**
	 * finds some {@link UniqueMampfItem} with a given {@link Item}
	 * @param item which item should be looked for
	 */
	default Optional<UniqueMampfItem> findByProduct(Item item) {
		return findByProductIdentifier(Objects.requireNonNull(item.getId()));
	}

	/**
	 * finds All {@link UniqueMampfItem}s in this Inventory and sorts them according type
	 * @param  type  what should be sorted for
	 */
	default List<UniqueMampfItem> findAllAndSort(String type) {

		List<UniqueMampfItem> list = this.findAll().toList();
		List<UniqueMampfItem> sortableList = new ArrayList<>(list);
		switch (type) {
			case "category":
				sortableList.sort(new SortByCategory());
				break;
			case "amount":
				sortableList.sort(new SortByAmount());
				break;
			default:
				sortableList.sort(new SortByName());
		}

		return sortableList;
	}

	class SortByName implements Comparator<UniqueMampfItem> {

		/**
		 * Sorts By Name, does explicitly not sort for Quantity
		 */
		@Override
		public int compare(UniqueMampfItem a, UniqueMampfItem b) {
			int comp = Util.compareCategories(a.getProduct().getName(), b.getProduct().getName());
			if (comp == 0) {
				comp = a.getProduct().getPrice().compareTo(b.getProduct().getPrice());
			}
			return comp;
		}
	}

	class SortByCategory implements Comparator<UniqueMampfItem> {

		/**
		 * Sorts By Category, does explicitly not sort for Quantity
		 */
		@Override
		public int compare(UniqueMampfItem a, UniqueMampfItem b) {
			int comp = Util.compareCategories(a.getCategory(), b.getCategory());
			if (comp == 0) {
				comp = a.getProduct().getPrice().compareTo(b.getProduct().getPrice());
			}
			return comp;
		}
	}

	class SortByAmount implements Comparator<UniqueMampfItem> {
		/**
		 * Sorts By Amount, secondary sorts for Name
		 */
		@Override
		public int compare(UniqueMampfItem a, UniqueMampfItem b) {
			// compare Quantity
			int comp = a.getQuantity().getAmount().compareTo(b.getQuantity().getAmount());

			// compare Name
			int alternative = a.getProduct().getName().compareTo(b.getProduct().getName());

			Set<Item.Category> infinit = new HashSet<>();
			infinit.add(Item.Category.FOOD);
			infinit.add(Item.Category.STAFF);

			boolean infinitA = infinit.contains(a.getCategory());
			boolean infinitB = infinit.contains(b.getCategory());

			if (infinitA && infinitB) {
				return alternative;
			}else if(infinitA){
				return 1;
			}else if(infinitB){
				return -1;
			}else if(comp == 0){
				comp = a.getProduct().getName().compareTo(b.getProduct().getName());
			}
			return comp;
		}
	}
}
