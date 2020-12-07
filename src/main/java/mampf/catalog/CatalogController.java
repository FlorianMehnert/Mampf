package mampf.catalog;

import mampf.Util;
import mampf.catalog.Item.Domain;
import mampf.inventory.Inventory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// import org.salespointframework.quantity.Quantity;

@Controller
public class CatalogController {

	//! Quantity is not used so should be or deleted

	private final MampfCatalog catalog;
	private final Inventory inventory;

	CatalogController(
		MampfCatalog catalog,
		Inventory inventory
	) {
		this.catalog = catalog;
		this.inventory = inventory;
	}

	//? Probably not needed because user can choose domain from homepage or navigation
	/**
	 * Returns a site from which the user can choose one the domains,
	 * currently (Eventcatering, Party-Service, Mobile-Breakfast and Rent-A-Cook)
	 * @param model
	 * @return html-template
	 */
	@GetMapping("/catalog")
	public String itemsCatalog(Model model){

		ArrayList<Map<String, String>> domains = new ArrayList<>();

		for(Domain domain : Domain.values()) {
			Map<String, String> domainObj = new HashMap<>();
			domainObj.put("title", Util.renderDomainName(domain.toString()));
			domainObj.put("href", domain.toString().toLowerCase());
			domains.add(domainObj);
		}

		model.addAttribute("domains", domains);
		model.addAttribute("headline", "Mampf - Catalog");

		return "catalog_index";
	}

	/**
	 *
	 * @param domain String which indicates which items from catalog have to be filtered
	 * @param model for serving data to the template
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

		if(catalogDomain.equals(Item.Domain.MOBILE_BREAKFAST)) {
			return "redirect:/mobile-breakfast";
		}

		// reorganizing the items of the chosen domain to show them each under its category
		Iterable<Item> filteredCatalog = catalog.findByDomain(catalogDomain);
		Iterator<Item> iterator = filteredCatalog.iterator();
		Map<String, ArrayList<Item>> categorizedItems = new HashMap<>();
		while(iterator.hasNext()){
			Item currentItem = iterator.next();

			// checking if the item is available for purchase in other words if is in stock
			if(inventory.findByProduct(currentItem).isEmpty()){
				continue;
			}

			// TODO: Add internationalization for the category in  the Util class
			String currentCategory = Util.renderDomainName(currentItem.getCategory().toString());
			if(categorizedItems.containsKey(currentCategory)){
				categorizedItems.get(currentCategory).add(currentItem);
			}else{
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
	public String mobileBreakfast(Model model){
		Map<String, ArrayList<Item>> reorganizedItems = new HashMap<>();
		Iterator<Item> breakFastItems = this.catalog.findByDomain(Item.Domain.MOBILE_BREAKFAST).iterator();
		while(breakFastItems.hasNext()){
			BreakfastItem currentItem = (BreakfastItem)breakFastItems.next();
			String category = Util.renderDomainName(currentItem.getType().toString());
			if(reorganizedItems.containsKey(category)){
				reorganizedItems.get(category).add(currentItem);
			}else{
				ArrayList<Item> itemList = new ArrayList<>();
				itemList.add(currentItem);
				reorganizedItems.put(category, itemList);
			}
		}
		model.addAttribute("categories", reorganizedItems);
		model.addAttribute("domainTitle", "Mobile Breakfast");
		return "mobile-breakfast.html";
	}

	@GetMapping("/catalog/item/detail/{item}")
	public String detail(Model model, @PathVariable Item item){
		assert item != null;
		model.addAttribute("title", item.getName());
		model.addAttribute("item", item);
		model.addAttribute("quantity", this.inventory.findByProduct(item).get().getQuantity());
		return "detail.html";
	}
	
}
