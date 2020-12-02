package mampf.catalog;

import org.javamoney.moneta.Money;
// import org.salespointframework.quantity.Quantity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.salespointframework.core.Currencies.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mampf.Util;
import mampf.catalog.Item.Domain;
import mampf.inventory.Inventory;

@Controller
public class CatalogController {

	//! Quantity is not used so should be or deleted
	// private static final Quantity NONE = Quantity.of(0);

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
	String itemsCatalog(Model model){

		ArrayList<Map<String, String>> domains = new ArrayList<>();

		for(Domain domain : Domain.values()) {
			Map<String, String> domainObj = new HashMap<>();
			domainObj.put("title", Util.renderDomainName(domain.toString()));
			domainObj.put("href", domain.toString().toLowerCase());
			domains.add(domainObj);
		}

		model.addAttribute("domains", domains);

		return "catalog_index";
	}

	/**
	 *
	 * @param domain String which indicates which items from catalog have to be filtered
	 * @param model for serving data to the template
	 * @return a thymeleaf html-template
	 */
	@GetMapping("/catalog/{domain}")
	String catalogDomain(@PathVariable String domain, Model model) {
		Domain catalogDomain;
		// As domain is given as string we try to parse its enum from Item.Domain
		try {
			catalogDomain = Util.parseDomainEnum(domain);
		} catch (IllegalArgumentException ex) {
			// If the given domain does not exist in the enumeration we let the user know by redirecting to an error page
			System.out.println(ex.toString());
			model.addAttribute("error", String.format("URI is invalid.\n\"%s\" is not a valid domain.", domain));
			return "404";
		}

		// reorganizing the items of the chosen domain to show them each under its category
		Iterable<Item> filteredCatalog = catalog.findByDomain(catalogDomain);
		Iterator<Item> iterator = filteredCatalog.iterator();
		Map<String, ArrayList<Item>> categorizedItems = new HashMap<>();
		while(iterator.hasNext()){
			Item currentItem = iterator.next();

			// checking if the item is available for purchase in other words if is in stock
			if(!inventory.findByProduct(currentItem).isPresent()) continue;

			// TODO: Add internationalization for the category in  the Util class
			String currentCategory = Util.renderDomainName(currentItem.getCategory().toString());
			if(categorizedItems.containsKey(currentCategory)){
				categorizedItems.get(currentCategory).add(currentItem);
			}
			else {
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

	@GetMapping("/catalog/item/detail/{item}")
	public String detail(Model model, @PathVariable Item item){
		assert item != null;
		model.addAttribute("title", item.getName());
		model.addAttribute("item", item);
		return "detail.html";
	}

	// -----------------
	//* Development Paths
	// -----------------

	//! Todo: add task to delete when building
	// Used to add random entities to the catalog with different domains
	@GetMapping("/catalog/add/random/{amount}")
	String itemsCount(@PathVariable int amount) {
		if(catalog.count() == 0){
			for(int i = 0; i < amount; i++){
				catalog.save(
				new Item(
					"test"+ i,
					Money.of(i, EURO),
					Util.randomEnum(Item.Domain.class),
					Util.randomEnum(Item.Category.class),
					"Das ist nur ein Test"
				)
			);
			}
		}
		return "redirect:/catalog/count";
	}

	//! Todo: add task to delete when building
	// Used to get the current length of catalog items
	@GetMapping("/catalog/count")
	public ResponseEntity<String> catalogCount(){
		var httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
		String count = Long.toString(catalog.count());
    return new ResponseEntity<>(count, httpHeaders, HttpStatus.OK);
	}

	//! Todo: add task to delete when building
	// Used to reset the catalog and delete all items from it
	@GetMapping("/catalog/reset")
	public ResponseEntity<String> reset(){
		catalog.deleteAll();
		var httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
		return new ResponseEntity<>("Deleted all catalog items!", httpHeaders, HttpStatus.OK);
	}
	
}
