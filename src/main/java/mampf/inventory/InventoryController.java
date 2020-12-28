package mampf.inventory;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import mampf.catalog.Item;
import org.salespointframework.quantity.Quantity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	public String add(@RequestParam("item") Item item,
					  @RequestParam("number") int number) {
		UniqueMampfItem currentItem = inventory.findByProduct(item).get();
		UniqueMampfItem newItem = new UniqueMampfItem(currentItem.getItem(), currentItem.getQuantity());
		if (!Util.infinity.contains(currentItem.getCategory())) {
			inventory.delete(currentItem);
			newItem.increaseMampfQuantity(number);
			inventory.save(newItem);
		}
		return "redirect:/inventory";
	}

	@GetMapping("inventory/{path}")
	@PreAuthorize("hasRole('BOSS')")
	public String sortByPath(Model model, @PathVariable String path) {
		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>();
		for (UniqueMampfItem item : inventory.findAllAndSort(path)) {
			String name = nullCategory(item);
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		return "inventory";
	}

	/**
	 * used to render Domain names
	 * @param item {@link UniqueMampfItem} whose Categpry gets converted to String
	 */
	public String nullCategory(UniqueMampfItem item) {
		String name = "";
		if (((Item) item.getProduct()).getCategory() != null) {
			name = Util.renderDomainName(((Item) item.getProduct()).getCategory().toString());
		}
		return name;
	}

	@GetMapping("/inventory")
	@PreAuthorize("hasRole('BOSS')")
	public String inventory(Model model) {
		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>();
		for (UniqueMampfItem item : inventory.findAllAndSort("")) {
			String name = nullCategory(item);
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		return "inventory";
	}
}
