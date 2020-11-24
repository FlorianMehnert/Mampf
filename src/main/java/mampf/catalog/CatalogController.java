package mampf.catalog;

import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

	@GetMapping("/items")
	String itemsCatalog(Model model){
		return "items"; 
	}
	
}
