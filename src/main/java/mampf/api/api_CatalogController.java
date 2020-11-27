package mampf.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;

@RestController
class api_CatalogController {

	private MampfCatalog catalog;

  api_CatalogController(MampfCatalog catalog) {
    this.catalog = catalog;
  }

  // Single item

  @GetMapping("/_api/catalog/item/{item}")
  Item one(@PathVariable Item item) {
		return item;
  }
}