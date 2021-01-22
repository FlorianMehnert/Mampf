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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class InventoryController {

	private final Inventory inventory;

	InventoryController(Inventory inventory) {
		this.inventory = inventory;
	}


	@PostMapping("/inventory/add")
	@PreAuthorize("hasRole('BOSS')")
	public String add(@RequestParam("item") Item item,
					  @RequestParam("number") String number,
					  @RequestParam ("negate") String neg,
					  RedirectAttributes redirectAttributes){
		int convNumber;
		String error = "error";
		if(number.equals("")){
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht leer sein!");
			return "redirect:/inventory/add";
		} else if (Integer.parseInt(number) < 0){
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht negativ sein!");
			return "redirect:/inventory/add";
		} else if (number.length() > 4){
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht größer als 9999 sein!");
			return "redirect:/inventory/add";
		}
		else{
			convNumber = Integer.parseInt(number);
		}
		UniqueMampfItem currentItem = inventory.findByProduct(item).get();
		UniqueMampfItem newItem = new UniqueMampfItem(currentItem.getItem(), currentItem.getQuantity());
		if (!Inventory.infinity.contains(currentItem.getCategory()) || currentItem.getCategory() != Item.Category.STAFF) {
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
	@PreAuthorize("hasRole('BOSS')")
	public String look(Model model, @RequestParam(required = false) String word, @RequestParam("type") String type){
		if(word.equals("") || type.equals("")){
			model.addAttribute("names", new ArrayList<>());
			model.addAttribute("filter", "");
			model.addAttribute("type", type);
			return "inventory";
		}

		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>(); // <UniqueItem, Category as String>
		List<UniqueMampfItem> list = inventory.findAllAndFilter(word, type);
		for (UniqueMampfItem item : list) {
			String name = Item.categoryTranslations.get(item.getCategory().toString());
			System.out.println(name);
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		model.addAttribute("filter", word);
		model.addAttribute("type", type);
		return "inventory";
	}

	@GetMapping("inventory/{path}")
	@PreAuthorize("hasRole('BOSS')")
	public String sortByPath(Model model, @PathVariable String path) {
		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>();
		for (UniqueMampfItem item : inventory.findAllAndSort(path)) {
			String name = Item.categoryTranslations.get(item.getCategory().toString());
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
			String name = Item.categoryTranslations.get(item.getCategory().toString());
			System.out.println(name);
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		return "inventory";
	}
}
