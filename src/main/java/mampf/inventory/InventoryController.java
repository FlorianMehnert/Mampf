package mampf.inventory;

import mampf.catalog.Item;
import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class InventoryController {

	private final UniqueInventory<UniqueInventoryItem> inventory;

	InventoryController(UniqueInventory<UniqueInventoryItem> inventory){
		this.inventory = inventory;
	}

	public Product findItem(String name){
		for(Product product : listItems()){
			if(product.getName().equals(name)){
				return product;
			}
		}
		return null;
	}

	public void reduceAmount(Product product, Quantity amount){
		assert product.getId() != null;
		Optional<UniqueInventoryItem> theItem = this.inventory.findByProduct(product);

		//TODO: what if theItem is not available
		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(amount));
	}

	public List<Product> listItems(){
		List<Product> list = new ArrayList<>();
		for(Product product: listItems()){
			list.add(product);
		}
		return null;
	}

	@PostMapping("/inventory/add")
	String add(@RequestParam("pid") Item item, @RequestParam("number") int number){

		UniqueInventoryItem currentItem = inventory.findByProduct(item).get();
		inventory.delete(currentItem);
		currentItem.increaseQuantity(Quantity.of(number));
		inventory.save(currentItem);
		return "redirect:/inventory";
	}

	@GetMapping("/inventory")
	// TODO only BOSS
	public String inventory(Model model) {

		model.addAttribute("inventory", inventory.findAll());

		return "inventory";
	}


}
