package mampf.catalog;

import org.salespointframework.catalog.Catalog;
import org.springframework.data.domain.Sort;

import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;

public interface MampfCatalog extends Catalog<Item> {
	static final Sort DEFAULT_SORT = Sort.by("productIdentifier").descending();

	Iterable<Item> findByCategory(Category cat, Sort sort);
	default Iterable<Item> findByCategory(Category cat){
		return findByCategory(cat, DEFAULT_SORT);
	}
	Iterable<Item> findByDomain(Domain domain, Sort sort);
	default Iterable<Item> findByDomain(Domain domain){
		return findByDomain(domain, DEFAULT_SORT);
	}
}
