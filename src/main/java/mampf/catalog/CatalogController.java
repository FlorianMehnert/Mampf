package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.salespointframework.core.Currencies.*;

import mampf.catalog.Item;
import mampf.catalog.Item.Domain;

@Controller
public class CatalogController {

	private static final Quantity NONE = Quantity.of(0);

	private final MampfCatalog catalog;
	private final UniqueInventory<UniqueInventoryItem> inventory;
	private final BusinessTime businessTime;

	CatalogController(
		MampfCatalog catalog, 
		UniqueInventory<UniqueInventoryItem> inventory, 
		BusinessTime businessTime
	) {
		this.catalog = catalog;
		this.inventory = inventory;
		this.businessTime = businessTime;
	}

	@GetMapping("/catalog")
	String itemsCatalog(Model model){
		return "catalog"; 
	}

	@GetMapping("/catalog/add/random")
	String itemsCount(Model model) {

		if(catalog.count() == 0){
			for(int i = 0; i < 20; i++){
				catalog.save(
				new Item(
					"test"+ i,
					Money.of(i, EURO),
					Item.Domain.EVENTCATERING,
					Item.Category.EQUIPMENT,
					"Das ist nur ein Test"
				)
			);
			}
		}
		model.addAttribute("count", catalog.count());

		return "count";
	}

	@GetMapping("/catalog/{domain}")
	String catalogDomain(@PathVariable String domain, Model model) {

		Domain catalogDomain = Domain.valueOf(domain.toUpperCase());
		model.addAttribute("catalog", catalog.findByDomain(catalogDomain));

		return "catalog";
	}
	
}
