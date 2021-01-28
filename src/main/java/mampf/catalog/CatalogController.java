package mampf.catalog;

import mampf.Util;
import mampf.catalog.Item.Domain;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.lib.Days;

import org.hibernate.exception.ConstraintViolationException;
import org.javamoney.moneta.Money;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

@Controller
public class CatalogController {

	// ! Quantity is not used so should be or deleted

	@Autowired
	private final MampfCatalog catalog;
	private final Inventory inventory;

	CatalogController(MampfCatalog catalog, Inventory inventory) {
		this.catalog = catalog;
		this.inventory = inventory;
	}

	// ? Probably not needed because user can choose domain from homepage or
	// navigation

	/**
	 * Returns a site from which the user can choose one the domains, currently
	 * (Eventcatering, Party-Service, Mobile-Breakfast and Rent-A-Cook)
	 *
	 * @param model
	 * @return html-template
	 */
	@GetMapping("/catalog")
	public String itemsCatalog(Model model) {

		ArrayList<Map<String, String>> domains = new ArrayList<>();

		for (Domain domain : Domain.values()) {
			Map<String, String> domainObj = new HashMap<>();
			domainObj.put("title", Util.renderDomainName(domain.toString()));
			domainObj.put("href", domain.toString().toLowerCase());
			domains.add(domainObj);
		}

		model.addAttribute("domains", domains);
		model.addAttribute("headline", "Mampf - Catalog");

		return "catalogIndex";
	}

	// @GetMapping("/catalog/edit")
	// public String catalogEdit(Model model) {
	// 	model.addAttribute("items", this.catalog.findAll());
	// 	return "catalog_itemList";
	// }

	@GetMapping("/catalog/create")
	public String catalogCreate(Model model) {
		model.addAttribute("domains", Item.Domain.values());
		model.addAttribute("categories", Item.Category.values());
		return "catalog_createItem";
	}

	@PostMapping("/catalog/create")
	public String catalogCreateItemPost(@Valid @ModelAttribute("form") CatalogItemForm form) {
		Item newItem = new Item(form.getName(), Money.of(new BigDecimal(form.getPrice().replace(",", ".")), "EUR"),
				Item.Domain.valueOf(form.getDomain().toUpperCase()), Item.Category.valueOf(form.getCategory().toUpperCase()),
				form.getDescription());
		this.catalog.save(newItem);
		this.inventory.save(new UniqueMampfItem(newItem, Quantity.of(1)));
		return "redirect:/inventory/name";
	}

	@PostMapping("/catalog/edit/{itemId}")
	public String catalogEditItemPost(@PathVariable String itemId, @Valid @ModelAttribute("form") CatalogItemForm form) {
		Optional<Item> item = this.catalog.findById(itemId);
		if (item.isEmpty()) {
			return "catalog_editItem";
		}
		Item realItem = item.get();
		Optional<UniqueMampfItem> inventoryItem = this.inventory.findByProduct(item.get());
		if (inventoryItem.isEmpty()) {
			return "catalog_editItem";
		}
		BigDecimal originalAmount = inventoryItem.get().getAmount();
		this.inventory.deleteById(inventoryItem.get().getId());
		try {
			this.catalog.delete(realItem);
		} catch (ConstraintViolationException e) {
			System.out.println(e.getConstraintName());
		}
		Item newItem = new Item(form.getName(),
				Money.of(new BigDecimal(form.getPrice().replaceAll("[^\\d,.]", "").replace(",", ".")), "EUR"),
				Item.Domain.valueOf(form.getDomain().toUpperCase()), Item.Category.valueOf(form.getCategory().toUpperCase()),
				form.getDescription());
		this.catalog.save(newItem);
		this.inventory.save(new UniqueMampfItem(newItem, Quantity.of(originalAmount.longValue())));
		return "redirect:/inventory/name";
	}

	@GetMapping("/catalog/edit/{itemId}")
	public String catalogEditItem(@PathVariable String itemId, Model model) {
		Item item;
		try {
			item = this.catalog.findById(itemId).get();
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.toString());
			model.addAttribute("error", String.format("URI is invalid.%n\"%s\" is not a valid item.", itemId));
			return "404";
		}
		model.addAttribute("item", item);
		model.addAttribute("domains", Item.Domain.values());
		model.addAttribute("categories", Item.Category.values());
		return "catalog_editItem";
	}

	/**
	 * @param domain String which indicates which items from catalog have to be
	 *               filtered
	 * @param model  for serving data to the template
	 * @return a thymeleaf html-template
	 */
	@GetMapping("/catalog/{domain}")
	public String catalogDomain(@PathVariable String domain, Model model) {
		Domain catalogDomain;
		// As domain is given as string we try to parse its enum from Item.Domain
		try {
			catalogDomain = Util.parseDomainEnum(domain);
		} catch (IllegalArgumentException ex) {
			// If the given domain does not exist in the enumeration
			// we let the user know by redirecting to an error page
			System.out.println(ex.toString());
			model.addAttribute("error", String.format("URI is invalid.%n\"%s\" is not a valid domain.", domain));
			return "404";
		}

		if (catalogDomain.equals(Item.Domain.MOBILE_BREAKFAST)) {
			return "redirect:/mobile-breakfast";
		}

		// reorganizing the items of the chosen domain to show them each under its
		// category
		ArrayList<Item> filteredCatalog = catalog.findByDomain(catalogDomain);
		Iterator<Item> iterator = filteredCatalog.iterator();
		Map<String, ArrayList<Item>> categorizedItems = new HashMap<>();
		while (iterator.hasNext()) {
			Item currentItem = iterator.next();

			// checking if the item is available for purchase in other words if is in stock
			if (inventory.findByProduct(currentItem).isEmpty()) {
				continue;
			}

			String currentCategory = Util.renderDomainName(currentItem.getCategory().toString());
			if (categorizedItems.containsKey(currentCategory)) {
				categorizedItems.get(currentCategory).add(currentItem);
			} else {
				ArrayList<Item> newArrayList = new ArrayList<>();
				newArrayList.add(currentItem);
				categorizedItems.put(currentCategory, newArrayList);
			}
		}
		String domainNameLocale = Util.renderDomainName(domain);
		model.addAttribute("domainTitle", domainNameLocale);
		model.addAttribute("catalog", categorizedItems);

		return "catalog";
	}

	@GetMapping("/mobile-breakfast")
	public String mobileBreakfast(Model model) {
		Map<String, ArrayList<Item>> reorganizedItems = new HashMap<>();
		for (Item item : this.catalog.findByDomain(Domain.MOBILE_BREAKFAST)) {
			BreakfastItem currentItem = (BreakfastItem) item;
			String category = Util.renderDomainName(currentItem.getType().toString());
			if (reorganizedItems.containsKey(category)) {
				reorganizedItems.get(category).add(currentItem);
			} else {
				ArrayList<Item> itemList = new ArrayList<>();
				itemList.add(currentItem);
				reorganizedItems.put(category, itemList);
			}
		}
		model.addAttribute("categories", reorganizedItems);
		model.addAttribute("domainTitle", "Mobile Breakfast");
		model.addAttribute("days", Days.values());
		return "mobile-breakfast";
	}

	@GetMapping("/catalog/item/detail/{item}")
	public String detail(Model model, @PathVariable Item item) {
		assert item != null;
		model.addAttribute("title", item.getName());
		model.addAttribute("category", Util.renderDomainName(item.getCategory().toString()));
		model.addAttribute("domain", Util.renderDomainName(item.getDomain().toString()));
		model.addAttribute("item", item);
		model.addAttribute("quantity", this.inventory.findByProduct(item).get().getQuantity());
		return "detail.html";
	}

}
