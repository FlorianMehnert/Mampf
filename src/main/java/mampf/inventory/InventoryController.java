package mampf.inventory;

import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InventoryController {

	private final Inventory inventory;

	InventoryController(Inventory inventory) {
		this.inventory = inventory;
	}

	@PostMapping("/inventory/add")
	String add(@RequestParam("pid") Item item, @RequestParam("number") int number) {

		UniqueInventoryItem currentItem = inventory.findByProduct(item).get();
		inventory.delete(currentItem);
		currentItem.increaseQuantity(Quantity.of(number));
		inventory.save(currentItem);
		return "redirect:/inventory";
	}

	@GetMapping("/inventory")
	// TODO only BOSS
	public String inventory(Model model) {
		model.addAttribute("inventory", inventory.findAllAndSort());
		return "inventory";
	}


}
