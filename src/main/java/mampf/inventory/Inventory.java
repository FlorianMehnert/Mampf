package mampf.inventory;

import mampf.Util;
import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.quantity.Quantity;

import java.util.*;

public interface Inventory extends UniqueInventory<UniqueMampfItem> {

	List<Item.Category> infinity = List.of(Item.Category.BUFFET, Item.Category.DECORATION,
			Item.Category.DINNER_EVENT, Item.Category.FOOD, Item.Category.SPECIAL_OFFERS);

	List<String> infinityStrings = List.of("unendlich", "infinity", "-1", "unbegrenzt", "unlimited");


	/**
	 * iterates through all Items in Inventory and returns matches by name
	 * @param name String which is the matching parameter
	 * @return UniqueMampfItem when there is a match or Optional.empty() for no match
	 */
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
	 *
	 * @param item   which product should be reduced
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
	 *
	 * @param item which item should be looked for
	 */
	default Optional<UniqueMampfItem> findByProduct(Item item) {
		return findByProductIdentifier(Objects.requireNonNull(item.getId()));
	}

	/**
	 * finds All {@link UniqueMampfItem}s in this Inventory and sorts them according type
	 *
	 * @param type what should be sorted for
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

	/**
	 *
	 * @param filter String for which should be filtered for
	 * @param type determines category in which the seach takes place
	 * @return List which is sorted with filter, type and partially
	 */
	default List<UniqueMampfItem> findAllAndFilter(String filter, String type) {
		List<UniqueMampfItem> list = this.findAll().toList();
		List<UniqueMampfItem> sortableList = new ArrayList<>(list);
		sortableList.sort(new SortByCategory());
		switch (type) {
			case "category":
					sortableList.removeIf(umi -> !(Util.renderDomainName(umi.getItem().getCategory().toString())).contains(filter));
				break;
			case "amount":
					try {
						sortableList.removeIf(umi -> {
							if ((!(Inventory.infinity.contains(umi.getCategory()))
									&& (Inventory.infinityStrings.contains(filter)))
									|| (!(Inventory.infinityStrings.contains(filter))
									&& umi.getQuantity().getAmount().intValue() != (Integer.parseInt(filter))))
								return true;
							if (!Inventory.infinityStrings.contains(filter)) {
								Integer.valueOf(filter);
							}
							return false;
						});
					} catch (NumberFormatException e) {
						return new ArrayList<>();
					}
				break;
			default: // case name
					sortableList.removeIf(umi -> !Util.renderDomainName(umi.getItem().getName()).contains(filter));
				break;
		}
		return sortableList;
	}
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

		boolean infinitA = Inventory.infinity.contains(a.getCategory());
		boolean infinitB = Inventory.infinity.contains(b.getCategory());

		if (infinitA && infinitB) {
			comp = alternative;
		} else if (infinitA) {
			comp = 1;
		} else if (infinitB) {
			comp = -1;
		} else if (comp == 0) {
			comp = a.getProduct().getName().compareTo(b.getProduct().getName());
		}
		return comp;
	}
}
