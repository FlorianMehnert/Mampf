package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
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
import mampf.catalog.Item;
import mampf.catalog.Item.Domain;

@Controller
public class CatalogController {

	private static final Quantity NONE = Quantity.of(0);

	private final MampfCatalog catalog;
	private final UniqueInventory<UniqueInventoryItem> inventory;
	private final BusinessTime businessTime;

	CatalogController(
			MampfCatalog catalog,
			UniqueInventory<UniqueInventoryItem> inventory,
			BusinessTime businessTime
	) {
		this.catalog = catalog;
		this.inventory = inventory;
		this.businessTime = businessTime;
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

		ArrayList<Map> domains = new ArrayList<>();

		for(Domain domain : Domain.values()) {
			Map<String, String> domainObj = new HashMap<>();
			domainObj.put("title", Util.renderDomainName(domain.toString()));
			domainObj.put("href", domain.toString().toLowerCase());
			domains.add(domainObj);
		}

		model.addAttribute("domains", domains);

		return "catalog_index";
	}

	// TODO: check if domain exists and return 404-Page if not eventually
	/**
	 *
	 * @param domain String which indicates which items from catalog have to be filtered
	 * @param model for serving data to the template
	 * @return a thymeleaf html-template
	 */
	@GetMapping("/catalog/{domain}")
	String catalogDomain(@PathVariable String domain, Model model) {
		Domain catalogDomain;
		try {
			catalogDomain = Util.parseDomainEnum(domain);
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.toString());
			model.addAttribute("error", String.format("URI is invalid.\n\"%s\" is not a valid domain.", domain));
			return "404";
		}
		Iterable<Item> filteredCatalog = catalog.findByDomain(catalogDomain);
		Iterator<Item> iterator = filteredCatalog.iterator();
		Map<String, ArrayList> categorizedItems = new HashMap<>();
		while(iterator.hasNext()){
			Item currentItem = iterator.next();
			String currentCategory = currentItem.getCategory().toString();
			if(categorizedItems.containsKey(currentCategory)){
				categorizedItems.get(currentCategory).add(currentItem);
			}
			else {
				ArrayList newArrayList = new ArrayList<Item>();
				newArrayList.add(currentItem);
				categorizedItems.put(currentCategory, newArrayList);
			}
		}
		String domainNameLocale = Util.renderDomainName(domain);
		model.addAttribute("domainTitle", domainNameLocale);
		model.addAttribute("catalog", categorizedItems);

		return "catalog";
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