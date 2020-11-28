package mampf.cart;


import mampf.catalog.Item;
import mampf.cart.MampfCart;

import java.util.Optional;

import org.salespointframework.core.AbstractEntity;
import org.salespointframework.order.Cart;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("cart")
public class CartController {
	
	private final OrderManager oM;
	private final EmployeeManager eM;
	
	
	public CartController(OrderManager oM, EmployeeManager eM) {
		//TODO: nullcheck
		//Assert.notNull(orderManagement, "OrderManagement must not be null!");
		this.oM = oM; this.eM = eM;
	}
	
	
	@ModelAttribute("cart")
	Cart initializeCart() {
		return new MampfCart();
	}
	
	//--------------
	
	//buy all
	@GetMapping("/checkout")
	String buyAll(@ModelAttribute MampfCart cart, @LoggedIn Optional<UserAccount> userAccount) {
		return buy(-1, cart, userAccount);
	}
	
	//buy spec item
	@GetMapping("/checkout/{index}")
	String buyIndex(@PathVariable int index, @ModelAttribute MampfCart cart, @LoggedIn Optional<UserAccount> userAccount) {
		return buy(index, cart, userAccount);
	}
	
	//buy in generell:
	String buy(int index, MampfCart cart, Optional<UserAccount> userAccount) {
		//TODO: redirect if not logged in
		
	}
	
	@GetMapping("/cart")
	String basket(@ModelAttribute MampfCart cart, Model model) {
		model.addAttribute("events", cart.getEvents());
		return "cart";
	}
	
}
