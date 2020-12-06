package mampf.order;

import com.mysema.commons.lang.Pair;
import mampf.Util;
import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import org.javamoney.moneta.Money;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.quantity.Metric;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.validation.Valid;
import java.util.*;


@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("cart")
public class OrderController {
	
	//TODO: find better solution for saving dates:

	//needed when FOOD is a reducable item:
	public class BreakfastMappedItems extends Item
		{
		//@Transient
		private MobileBreakfastForm form;
		public BreakfastMappedItems(String name,
										  Money price,
										  String description,
										  MobileBreakfastForm form) 
			{super(name, price, 
				   Item.Domain.MOBILE_BREAKFAST,
				   Item.Category.FOOD,
				   description);this.form=form;}
		public MobileBreakfastForm getForm() {return form;}
		}
	
	private final MampfOrderManager orderManager;

	public OrderController(MampfOrderManager orderManager) {
		this.orderManager = orderManager;
	}

/* CART */
	
	@ModelAttribute("cart")
	Cart initializeCart() {
		return new Cart();
	}

	@PostMapping("/cart")
	String addItem(@RequestParam("pid") Item item, @RequestParam("number") int number, @ModelAttribute Cart cart) {

		//int amount = number <= 0 || number > 5 ? 1 : number;
		//TODO: negativ amount, xyz...
		cart.addOrUpdateItem(item, Quantity.of(number));
		return "redirect:/catalog/" + item.getDomain();
	}

	@GetMapping("/cart")
	String basket(Model model/*, DateFormular form*/,@ModelAttribute Cart cart) {
		//model.addAttribute("form", form);
		model.addAttribute("domains", getDomainItems(cart,"none"));
		return "cart";
	}
	
	@PostMapping("cart/clear")
	String clearCart(@ModelAttribute Cart cart) {
		cart.clear();
		return "redirect:/cart";
	}

	/**
	 * 	handles adding and removing the amount of a cartitem
	 */
	@PostMapping("cart/setNewAmount")
	String addCartItem(@RequestParam String cartitemId, @RequestParam int newAmount, @ModelAttribute Cart cart) {
		Optional<CartItem> cartitem = cart.getItem(cartitemId);
		
		if(cartitem.isEmpty()) {
			return "redirect:/cart";
		}
		if(newAmount < 1) {
			cart.removeItem(cartitemId);
			return "redirect:/cart";
		}
		int diffAmount = newAmount - cartitem.get().getQuantity().getAmount().intValue();

		cart.addOrUpdateItem(cartitem.get().getProduct(), Quantity.of(diffAmount, Metric.UNIT));
		return "redirect:/cart";
	}

	@PostMapping("/cart/add/mobile-breakfast")
	String orderMobileBreakfast(@ModelAttribute Cart cart, @LoggedIn Optional<UserAccount> userAccount, @Valid MobileBreakfastForm form){

		if(userAccount.isEmpty()) 
			{return "redirect:/login";}
		
		/*BreakfastItem b = null, d = null;
		for(Item item: catalog.findByDomain(Item.Domain.MOBILE_BREAKFAST)) 
			{if(item.getName().equals(form.getBeverage()))b=((BreakfastItem)item);
			if(item.getName().equals(form.getDish()))d=((BreakfastItem)item);}
		if(b != null && d != null)*/
		//TODO check for valid form
		cart.addOrUpdateItem(new 
				BreakfastMappedItems(
						"Mobile Breakfast Choice: "+form.getBeverage().getName()+", "+form.getDish().getName(),
						BreakfastItem.BREAKFAST_PRICE,
						"Employee Choice of beverage and dish",
						form),
				Quantity.of(1));
		
		return "redirect:/cart";
	}

	
	
	@PostMapping("cart/remove")
	String removeCartItem(@RequestParam String cartitemId, @ModelAttribute Cart cart) {
		
		if(!cart.getItem(cartitemId).isPresent()) return "redirect:/cart";
		cart.removeItem(cartitemId);
		return "redirect:/cart";
	}
	
	//choose to buy:
	@PostMapping("/pay")
	public String chooseToBuy(Model model, @ModelAttribute Cart cart, @RequestParam String domain, DateFormular form) {
		model.addAttribute("events", getDomainItems(cart, Util.parseDomainName(domain).toString()));
		model.addAttribute("domainChoosen", domain);
		model.addAttribute("form", form);
		return "buy_cart";
	}	
	
	//TODO: replace domain with optionals
	Map<String,List<CartItem>> getDomainItems(Cart cart, String domain){
		Map<String,List<CartItem>> events = new HashMap<>();
		boolean checkForDomain = (!domain.equals("none")); 
		for(CartItem cartitem: cart){
			Item item = (Item) cartitem.getProduct();
			Item.Domain itemDomain = item.getDomain();
			
			//skip item if not of requested domain
			if(checkForDomain && !itemDomain.equals(Item.Domain.valueOf(domain))){
				continue;
			}

			if(events.containsKey(itemDomain)){
				//add to list
				events.get(Util.renderDomainName(itemDomain.toString())).add(cartitem);
			}else {
				//just create new list
				List<CartItem> event = new ArrayList<>();
				event.add(cartitem);
				events.put(Util.renderDomainName(itemDomain.toString()), event);
			}
		}
		return events;
	}
	
	
	
	@PostMapping("/checkout")
	String buy(@RequestParam String domainChoosen, @ModelAttribute Cart cart, @Valid DateFormular form, Errors result, @LoggedIn Optional<UserAccount> userAccount, RedirectAttributes redirectAttributes) {
		
		if(userAccount.isEmpty()) 
			{return "redirect:/login";}
		
		//formular fehler
		if (result.hasErrors() /*|| form.invalid()*/) {
			/*TODO*/return "redirect:/cart";
		}
		
		Map<String, List<CartItem>> orders = getDomainItems(cart, domainChoosen);
		//MobileBreakfastForm mbForm;
		//List<MampfOrder> createdOrders = new ArrayList<>();
		for(String domainStr : orders.keySet()) {
			Item.Domain domain = Util.parseDomainName(domainStr);
			//create new cart:
			Cart orderCart = new Cart();
			for(CartItem cartitem: orders.get(domain)) {
				orderCart.addOrUpdateItem(cartitem.getProduct(), cartitem.getQuantity());
			}
			
			//create order:
			MampfOrder order = orderManager.createOrder(orderCart, form, userAccount.get());
			
			//remove cartitem and save the created orders:
			if(order != null)
				//remove items from main cart:
				{for(CartItem cartitem: orderCart) 
					{cart.removeItem(cartitem.getProduct().getId().getIdentifier());} 
				}	
			}
		
		//TODO: mark new created order
		return "redirect:/userOrders";
		/*if(order != null) {
			redirectAttributes.addAttribute("id", order.getId().getIdentifier());
			return "redirect:/orders/detail/{id}";
		}
		//order fehler:
		//TODO
		return "redirect:/cart";*/
		
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
