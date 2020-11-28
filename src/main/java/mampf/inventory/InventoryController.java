package mampf.inventory;

import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InventoryController {

	private final UniqueInventory<UniqueInventoryItem> inventory;

	InventoryController(UniqueInventory<UniqueInventoryItem> inventory){
		this.inventory = inventory;
	}

	@GetMapping("/inventory")
	// TODO only BOSS
	public String inventory(Model model) {

		model.addAttribute("inventory", inventory.findAll());

		return "inventory";
	}
}
