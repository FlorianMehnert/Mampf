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
import java.util.List;

@Controller
public class InventoryController {

	private final Inventory inventory;

	InventoryController(Inventory inventory) {
		this.inventory = inventory;
	}

	@PostMapping("/inventory/add")
	public String add(@RequestParam("item") Item item,
					  @RequestParam("number") String number,
					  @RequestParam ("negate") String neg) {
		int convNumber;
		if(number.equals("")){
			return "redirect:/inventory/add";
		}else{
			convNumber = Integer.parseInt(number);
		}
		UniqueMampfItem currentItem = inventory.findByProduct(item).get();
		UniqueMampfItem newItem = new UniqueMampfItem(currentItem.getItem(), currentItem.getQuantity());
		if (!Util.infinity.contains(currentItem.getCategory())) {
			inventory.delete(currentItem);
			if(neg.equals("decr")){
				if(currentItem.getQuantity().isGreaterThanOrEqualTo(Quantity.of(convNumber))) {
					newItem.decreaseMampfQuantity(convNumber);
				}
			}else {
				if(!(currentItem.getQuantity().add(Quantity.of(convNumber)).isGreaterThan(Quantity.of(2147000000)))) {
					newItem.increaseMampfQuantity(convNumber);
				}
			}
			inventory.save(newItem);
		}
		return "redirect:/inventory/amount";
	}

	@GetMapping("inventory/filter")
	public String look(Model model, @RequestParam("word") String word){
		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>(); // <UniqueItem, Category as String>
		System.out.println(word);
		List<UniqueMampfItem> list = inventory.findAllAndFilter(word);
		System.out.println(list);
		for (UniqueMampfItem item : list) {
			System.out.println(item);
			String name = nullCategory(item);
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		System.out.println(names);
		model.addAttribute("names", names);
		return "inventory";
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
	 * @param item {@link UniqueMampfItem} whose Category gets converted to String
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
