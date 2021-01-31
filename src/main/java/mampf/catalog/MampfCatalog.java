package mampf.catalog;

import java.util.ArrayList;
import java.util.Optional;

import org.salespointframework.catalog.Catalog;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;

import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;

/**
 * Represents the Repository for saving all item related Data
 */
public interface MampfCatalog extends Catalog<Item> {
	Sort DEFAULT_SORT = Sort.by("productIdentifier").descending();

	/**
	 * Searches all items for a specific id served as String value
	 * @param id of the item
	 * @return Returns either an existing Item or null
	 */
	default Optional<Item> findById(String id){
		Streamable<Item> items = this.findAll();
		Item found = null;
		for(Item item : items) {
			if(item.getId().toString().equals(id)){
				found = item;
			}
		}
		return Optional.of(found);
	}

	/**
	 * Filters all Items by a given category
	 * @param cat Value of the category as category enum
	 * @param sort Sort object which is responsible for ording all items descending
	 * @return Returns all Items which have the given category
	 */
	ArrayList<Item> findByCategory(Category cat, Sort sort);
	default ArrayList<Item> findByCategory(Category cat){
		return findByCategory(cat, DEFAULT_SORT);
	}
	/**
	 * Filters all Items by a given domain
	 * @param domain Value of the domain as domain enum
	 * @param sort Sort object which is responsible for ording all items descending
	 * @return Returns all Items which have the given domain
	 */
	ArrayList<Item> findByDomain(Domain domain, Sort sort);
	default ArrayList<Item> findByDomain(Domain domain){
		return findByDomain(domain, DEFAULT_SORT);
	}
}
