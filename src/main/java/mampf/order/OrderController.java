package mampf.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.Order;
import org.salespointframework.payment.Cash;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;

import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("cart")
public class OrderController {
	
	private final MampfOrderManager orderManager;
	//private final Inventory inventory;
	//private final EmployeeManagement employeeManagement;
	
	public OrderController(MampfOrderManager orderManager/*, Inventory inventory, EmployeeManagement employeeManagement*/) {
		this.orderManager = orderManager;/* this.inventory = inventory; this.employeeManagement = employeeManagement;*/
	}

	@ModelAttribute("cart")
	Cart initializeCart() {
		return new Cart();
	}

	@PostMapping("/cart")
	String addItem(@RequestParam("pid") Item item, @RequestParam("number") int number, @ModelAttribute Cart cart) {

		//int amount = number <= 0 || number > 5 ? 1 : number;

		cart.addOrUpdateItem(item, Quantity.of(number));

		return "redirect:/cart";
	}

	@GetMapping("/cart")
	String basket(Model model, DateFormular form) {
		model.addAttribute("form", form);
		return "cart";
	}

	@PostMapping("/checkout")
	String buy(@ModelAttribute Cart cart, @Valid DateFormular form, Errors result, @LoggedIn Optional<UserAccount> userAccount) {
		
		if(userAccount.isEmpty()) {
			return "redirect:/register";
		}
		if (result.hasErrors()) {
			return "redirect:/cart";
		}
		//TODO: starttime > endtime
		
		
		//if(userAccount.isEmpty() || !orderManager.validateCart(cart,form)) {
		//	return "redirect:/catalog";
		//}
		//form.getStartDate()
		
		//MampfDate orderDate = new MampfDate(form.getStartDate(), form.getEndDate(), form.getAddress()); 
		//return userAccount.map(account -> {	
		//MampfOrder order = new MampfOrder(userAccount.get(), Cash.CASH,orderDate);
		//cart.addItemsTo(order);
		//orderManager.payOrder(order);
		//cart.clear();
		MampfOrder order = orderManager.createOrder(cart, form, userAccount.get());
		
		if(order != null)return "redirect:/";
		return "redirect:/cart";
		//}).orElse("redirect:/cart");
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
	
	@GetMapping("/userOrders")
	String orderUser(Model model, @LoggedIn Optional<UserAccount> userAccount) {
		if(userAccount.isEmpty())return "redirect:/";
		List<MampfOrder> orders = orderManager.findByUserAcc(userAccount.get());
		model.addAttribute("orders", orders);
		return "orders";
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
