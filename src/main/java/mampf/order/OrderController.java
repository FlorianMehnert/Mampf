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
import org.salespointframework.quantity.Metric;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

/* CART */
	
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
	
	@PostMapping("cart/clear")
	String clearCart(@ModelAttribute Cart cart) {
		cart.clear();
		return "redirect:/cart";
	}
	
	//handles adding and removing the amount of a cartitem
	@PostMapping("cart/add")
	String addCartItem(@RequestParam String cartitemId, @RequestParam int amount, @RequestParam boolean add, @ModelAttribute Cart cart) {
		Optional<CartItem> cartitem = cart.getItem(cartitemId);
		
		if(!cartitem.isPresent()) return "redirect:/cart";
			
		if(!add)amount = -amount;
		if(cartitem.get().getQuantity().getAmount().intValue()+amount < 1)return "redirect:/cart";
		
		cart.addOrUpdateItem(cartitem.get().getProduct(), Quantity.of(amount, Metric.UNIT));
		return "redirect:/cart";
	}
	
	//TODO: find better solution:
	@PostMapping("cart/remove")
	String removeCartItem(@RequestParam String cartitemId, @ModelAttribute Cart cart) {
		
		if(!cart.getItem(cartitemId).isPresent()) return "redirect:/cart";
		cart.removeItem(cartitemId);
		return "redirect:/cart";
	}
	
	@PostMapping("/checkout")
	String buy(@ModelAttribute Cart cart, @Valid DateFormular form, Errors result, @LoggedIn Optional<UserAccount> userAccount, RedirectAttributes redirectAttributes) {
		
		if(userAccount.isEmpty()) {
			return "redirect:/login";
		}
		//formular fehler
		if (result.hasErrors() || form.invalid()) {
			//TODO
			return "redirect:/cart";
		}
		
		MampfOrder order = orderManager.createOrder(cart, form, userAccount.get());
		if(order != null) {
			redirectAttributes.addAttribute("id", order.getId().getIdentifier());
			return "redirect:/orders/detail/{id}";
		}
		//order fehler:
		//TODO
		return "redirect:/cart";
		
	}
	
/* ORDERS */
	
	@GetMapping("/orders")
	@PreAuthorize("hasRole('BOSS')")
	String orders(Model model) {

		ArrayList<MampfOrder> orders = orderManager.findAll();
		model.addAttribute("orders", orders);
		return "orders";
	}

	@GetMapping("/orders/detail/{order}")
	//@PreAuthorize("hasRole('BOSS')")
	String editOrder(@PathVariable MampfOrder order, Model model) {
		
		
		model.addAttribute("order", order);
		model.addAttribute("orderLines", order.getOrderLines());
		model.addAttribute("employees", order.getEmployees());
		
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
