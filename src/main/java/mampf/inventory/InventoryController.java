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

@Controller
public class InventoryController {

	private final Inventory inventory;

	InventoryController(Inventory inventory) {
		this.inventory = inventory;
	}

	@PostMapping("/inventory/add")
	String add(@RequestParam("item") Item item, @RequestParam("number") int number) {
		if(inventory.findByProduct(item).isPresent()){
			UniqueInventoryItem currentItem = inventory.findByProduct(item).get();
			if(currentItem.getQuantity().equals(Quantity.of(-1))){
				return "redirect:/inventory";
			}else{
				inventory.delete(currentItem);
				currentItem.increaseQuantity(Quantity.of(number));
				inventory.save(currentItem);
				return "redirect:/inventory";
			}
		}else{
			//TODO bessere weiterleitung ausdenken
			return "404";
		}

	}

	private String nullCategory(UniqueInventoryItem item){
		String name = "";
		if(((Item) item.getProduct()).getCategory() != null){
			name = Util.renderDomainName(((Item) item.getProduct()).getCategory().toString());
		}
		return name;
	}

	@GetMapping("/inventory")
	@PreAuthorize("hasRole('BOSS')")
	public String inventory(Model model) {
		ArrayList<Pair<UniqueInventoryItem, String>> names = new ArrayList<>();
		for(UniqueInventoryItem item:inventory.findAllAndSort()){
			String name = nullCategory(item);
			Pair<UniqueInventoryItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		return "inventory";
	}
}
