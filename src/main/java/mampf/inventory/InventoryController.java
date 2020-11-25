package mampf.inventory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InventoryController {
	@GetMapping("/inventory")
	public String inventory() {
		return "inventory";
	}
}
