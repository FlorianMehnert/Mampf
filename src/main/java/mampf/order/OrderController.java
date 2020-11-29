package mampf.order;

import java.util.ArrayList;
import java.util.Optional;

import javax.validation.Valid;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Cart;
import org.salespointframework.order.Order;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;

//import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("cart")
public class OrderController {
	
	private final MampfOrderManager orderManager;
	
	public OrderController(MampfOrderManager orderManager, MampfCatalog catalog) {
		this.orderManager = orderManager;
	}

	@ModelAttribute("cart")
	Cart initializeCart() {
		return new Cart();
	}

	@PostMapping("/cart")
	String addItem(@RequestParam("pid") Item item, @RequestParam("number") int number, @ModelAttribute Cart cart) {


		int amount = number <= 0 || number > 5 ? 1 : number;

		cart.addOrUpdateItem(item, Quantity.of(amount));

		return "redirect:/cart";
	}

	@GetMapping("/cart")
	String basket() {
		return "cart";
	}

	@PostMapping("/checkout")
	String buy(@ModelAttribute Cart cart, @LoggedIn Optional<UserAccount> userAccount) {

		return userAccount.map(account -> {

			MampfOrder order = new MampfOrder(account, Cash.CASH);

			cart.addItemsTo(order);

			orderManager.payOrder(order);
			// orderManager.completeOrder(order);

			cart.clear();

			return "redirect:/";
		}).orElse("redirect:/cart");
	}

	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	String orders(Model model) {

		ArrayList<MampfOrder> orders = orderManager.findAll();
		model.addAttribute("orders", orders);
		return "orders";
	}

	@GetMapping("/orders/detail")
	@PreAuthorize("hasRole('BOSS')")
	String editOrder(Model model, @RequestParam MampfOrder order) {

		model.addAttribute("order", order);
		return "orders_detail";
	}
	
	// @PostMapping("/pay") //, Errors result, result.hasErrors() || 
	// String payOrder(@RequestParam("oid") MampfOrder order, @RequestParam("payment") int choosenPayment, @LoggedIn Optional<UserAccount> useraccount, Model model) {
	// 	//TODO: error mapping
		
		
	// 	if (!useraccount.isPresent()) {
	// 		//TODO: add some fancy errors
	// 		return "buy_order";
	// 	}
		
	// 	//MampfOrder order = oM.findNewestOrder(useraccount.get());
	// 	//model.addAttribute("order", order);
			
	// 	orderManager.payOrder(order);
		
	// 	//TODO: complete order if possible
	// 	//TODO: show order instead of index
	// 	return "redirect:/index";
		
	// }
	
	
	
}
