package mampf.inventory;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import mampf.catalog.Item;
import mampf.order.MampfOrderManager;

import org.salespointframework.quantity.Quantity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class InventoryController {

	private final Inventory inventory;
	private final MampfOrderManager mampfOrderManager;


	InventoryController(Inventory inventory, MampfOrderManager mampfOrderManager) {
		this.inventory = inventory;
		this.mampfOrderManager = mampfOrderManager;
	}


	/**
	 * changes the Quantity of already existing UniqueMampfItems in the inventory
	 *
	 * @param item               the items which amount should be altered
	 * @param number             always positive
	 * @param neg                used to determine between negative and positive inputs
	 * @param redirectAttributes used to store ErrorMessages
	 * @return inventory/amount
	 */
	@PostMapping("/inventory/add")
	@PreAuthorize("hasRole('BOSS')")
	public String add(@RequestParam("item") Item item,
					  @RequestParam("number") String number,
					  @RequestParam("negate") String neg,
					  RedirectAttributes redirectAttributes) {
		String error = "error";
		if (number.equals("")) {
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht leer sein!");
		} else if (Integer.parseInt(number) < 0) {
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht negativ sein!");
		} else if (number.length() > 4) {
			redirectAttributes.addFlashAttribute(error, "die Eingabe darf nicht gr????er als 9999 sein!");
		} else {
			infinity(number, item, neg);
			return "redirect:/inventory/amount";
		}
		return "redirect:/inventory";
	}

	/**
	 * part of add: Looks to see if the category of the item allows for an increase based on {@link Inventory#infinity}
	 *
	 * @param number amount of increase
	 * @param item item to be increased
	 * @param neg is the increase positive or ist it negative ({@code "decr"})
	 */

	private void infinity(String number, Item item, String neg) {

		// parsed number
		int convNumber = Integer.parseInt(number);

		// the given item in the inventory
		UniqueMampfItem currentItem = inventory.findByProduct(item).get();

		// the new item which will replace the current item
		UniqueMampfItem newItem = new UniqueMampfItem(currentItem.getItem(), currentItem.getQuantity());

		// checks weather the category of the current item allows it to get modified
		if (!Inventory.infinity.contains(currentItem.getCategory()) || currentItem.getCategory() != Item.Category.STAFF) {
			inventory.delete(currentItem);
			if (neg.equals("decr")) {
				if (currentItem.getQuantity().isGreaterThanOrEqualTo(Quantity.of(convNumber))
						&& checkOrderAmount(newItem.getAmount().longValue() - convNumber, item)) {
					newItem.decreaseMampfQuantity(convNumber);
				}
			} else {
				if (!(currentItem.getQuantity().add(Quantity.of(convNumber)).isGreaterThan(Quantity.of(2147000000)))) {
					newItem.increaseMampfQuantity(convNumber);
				}
			}
			inventory.save(newItem);
		}
	}

	/**
	 * ordered items should not be removable from inventory.</br>
	 * sums up amount of ordered items of item. </br>
	 * compares ordered amount with left amount of item
	 *
	 * @param leftAmount a number for the amount left =  inventoryAmount - subAmount
	 * @param item       a {@link Item} in the inventory
	 * @return {@code true} when the inventory item quantity can be reduced
	 */
	private boolean checkOrderAmount(long leftAmount, Item item) {
		LocalDateTime dateNow = LocalDateTime.now();
		long orderedAmount = 0;
		List<BigDecimal> stuff = new ArrayList<>();
		mampfOrderManager.findAll().stream().filter(order -> order.hasTimeOverlap(dateNow, dateNow)
				|| order.getStartDate().isAfter(dateNow)).forEach(
				order -> order.getOrderLines().filter(orderLine -> orderLine.refersTo(item)).forEach(
						orderLine -> stuff.add(orderLine.getQuantity().getAmount())));
		for (BigDecimal amount : stuff) {
			orderedAmount += amount.longValue();
		}
		return leftAmount >= orderedAmount;
	}

	/**
	 * used to filter for specific Items based on {@param type} and {@param word}
	 *
	 * @param model
	 * @param word  String which will be filtered for
	 * @param type  category of the inventory in which to filter
	 * @return /inventory with a filtered item list
	 */
	@GetMapping("inventory/filter")
	@PreAuthorize("hasRole('BOSS')")
	public String look(Model model, @RequestParam(required = false) String word, @RequestParam("type") String type) {
		if (word.equals("") || type.equals("")) {
			model.addAttribute("names", new ArrayList<>());
			model.addAttribute("filter", "");
			model.addAttribute("type", type);
			return "inventory";
		}

		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>(); // <UniqueItem, Category as String>
		List<UniqueMampfItem> list = inventory.findAllAndFilter(word, type);
		for (UniqueMampfItem item : list) {
			String name = Item.categoryTranslations.get(item.getCategory().toString());
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		model.addAttribute("filter", word);
		model.addAttribute("type", type);
		return "inventory";
	}

	/**
	 * reorders elements in inventory view
	 *
	 * @param model
	 * @param path  type of reordering
	 * @return /inventory
	 */
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
		model.addAttribute("type", "name");
		return "inventory";
	}

	/**
	 * used to render Domain names
	 *
	 * @param item {@link UniqueMampfItem} whose Category gets converted to String
	 */
	public String nullCategory(UniqueMampfItem item) {
		String name = "";
		if (((Item) item.getProduct()).getCategory() != null) {
			name = Util.renderDomainName(((Item) item.getProduct()).getCategory().toString());
		}
		return name;
	}

	/**
	 * defaulting for inventory
	 *
	 * @param model
	 * @return /inventory sorted by name
	 */
	@GetMapping("/inventory")
	@PreAuthorize("hasRole('BOSS')")
	public String inventory(Model model) {
		ArrayList<Pair<UniqueMampfItem, String>> names = new ArrayList<>();
		for (UniqueMampfItem item : inventory.findAllAndSort("")) {
			String name = Item.categoryTranslations.get(item.getCategory().toString());
			Pair<UniqueMampfItem, String> pair = new Pair<>(item, name);
			names.add(pair);
		}
		model.addAttribute("names", names);
		model.addAttribute("type", "name");
		return "inventory";
	}
}
