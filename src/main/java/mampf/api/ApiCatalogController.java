package mampf.api;

import java.util.Optional;

import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;

@RestController
class ApiCatalogController {

	// ! catalog isn't in use so should be used or deleted
	private MampfCatalog catalog;

	ApiCatalogController(MampfCatalog catalog) {
		this.catalog = catalog;
	}

	// Single item

	@GetMapping("/_api/catalog/item/{itemId}")
	Item one(@PathVariable ProductIdentifier itemId) {
		return catalog.findById(itemId).get();
  }
}