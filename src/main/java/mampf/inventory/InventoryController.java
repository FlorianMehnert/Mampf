package mampf.inventory;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import mampf.catalog.Item;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	@PreAuthorize("hasRole('BOSS')")
	public String inventory(Model model) {
		ArrayList<Pair<UniqueInventoryItem, String>> names = new ArrayList<>();
		ArrayList<String> ames = new ArrayList<>();
		for(UniqueInventoryItem item:inventory.findAllAndSort()){
			String name = Util.renderDomainName(((Item) item.getProduct()).getCategory().toString());
			Pair<UniqueInventoryItem, String> map = new Pair<>(item, name);
			names.add(map);
		}
		model.addAttribute("names", names);
		return "inventory";
	}
}
