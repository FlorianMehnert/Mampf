package mampf.order;

import java.util.Optional;

import javax.validation.Valid;

import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

//import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@PreAuthorize("isAuthenticated()")
public class OrderController {
	
	private final MampfOrderManager oM;
	
	public OrderController(MampfOrderManager oM) {
		this.oM = oM;
	}
	
	@PostMapping("/pay") //, Errors result, result.hasErrors() || 
	String payOrder(@RequestParam("oid") MampfOrder order, @RequestParam("payment") int choosenPayment, @LoggedIn Optional<UserAccount> useraccount, Model model) {
		//TODO: error mapping
		
		
		if (!useraccount.isPresent()) {
			//TODO: add some fancy errors
			return "buy_order";
		}
		
		//MampfOrder order = oM.findNewestOrder(useraccount.get());
		//model.addAttribute("order", order);
			
		oM.payOrder(order);
		
		//TODO: complete order if possible
		//TODO: show order instead of index
		return "redirect:/index";
		
	}
	
	
	@GetMapping("/orders")
	public String viewOrders(Model model,@LoggedIn Optional<UserAccount> userAccount) {
		//TODO: nullcheck
		model.addAttribute("orders", oM.findByUserAcc(userAccount.get()));
		return "orders";
	}
	
	
	
}
