package mampf.catalog;

import org.salespointframework.catalog.Catalog;
import org.springframework.data.domain.Sort;

import mampf.catalog.Item;

public interface MampfCatalog extends Catalog<Item> {
	static final Sort DEFAULT_SORT = Sort.by("productIdentifier").descending();

	
}
