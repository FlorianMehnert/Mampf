package mampf.catalog;

import java.util.ArrayList;
import java.util.Optional;

import org.salespointframework.catalog.Catalog;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;

import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;

public interface MampfCatalog extends Catalog<Item> {
	Sort DEFAULT_SORT = Sort.by("productIdentifier").descending();

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

	ArrayList<Item> findByCategory(Category cat, Sort sort);
	default ArrayList<Item> findByCategory(Category cat){
		return findByCategory(cat, DEFAULT_SORT);
	}
	ArrayList<Item> findByDomain(Domain domain, Sort sort);
	default ArrayList<Item> findByDomain(Domain domain){
		return findByDomain(domain, DEFAULT_SORT);
	}
}
