package mampf.order;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;
import mampf.user.UserController;

import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.catalog.Item;
import mampf.catalog.Item.Domain;
import mampf.catalog.Item.Category;
import mampf.catalog.BreakfastItem;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;

import org.salespointframework.order.OrderStatus;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;


@SpringBootTest
@ContextConfiguration
@WithMockUser(username = "dude", roles = {"INDIVIDUAL"})
public class MampfOrderManagerTests {

	@Autowired
	MampfCatalog catalog;
	@Autowired
	Inventory inventory;
	@Autowired
	MampfOrderManager orderManager;
	@Autowired
	OrderController orderController;
	@Autowired
	UserController userController;
	
	@Autowired
	EmployeeManagement employeeManager;
	@Autowired
	UserManagement userManager;

	private MampfCart cart;
	private LocalDateTime justadate = LocalDateTime.now().plus(OrderController.delayForEarliestPossibleBookingDate).plus(Duration.ofDays(1));
	

	MampfOrderManagerTests() {
		cart = new MampfCart();
	}

	//TODO: test mobile breakfast separat
	//TODO: delete cart check
	void initContext() {

		inventory.deleteAll();
		for (Item item : catalog.findAll()) {
			switch (item.getCategory()) {
				case DECORATION:
				case EQUIPMENT:
					inventory.save(new UniqueMampfItem(item, Quantity.of(10)));
					break;
				default:
					inventory.save(new UniqueMampfItem(item, Quantity.of(-1)));
					break;
			}
		}
		/*
		 * Personal-Stock:
		 * 6 x Cook
		 * 6 x Service
		 */
		for (Employee employee : employeeManager.findAll()) {
			List<EventOrder> booked = employee.getBooked();
			booked.stream().peek(xd -> xd.getEmployees().clear());
		}

		employeeManager.findAll().stream().peek(xdd -> xdd.getBooked().clear());
		
		/*
		 * User-Stock:
		 * ind:
		 * - hans
		 * admin:
		 * - hansWurst
		 * comp:
		 * - dextermorgan
		 * emp:
		 * - trippster
		 */
		userManager.findAll().forEach(
			user->{if(user.getCompany().isPresent())user.getCompany().get().resetCompany();}
		);
		
		/*
		 * Order-Stock
		 * is just empty
		 */
		//orderManager.deleteAll();
	}

	CheckoutForm initForm() {
		List<String> domains = new ArrayList<>();
		List.of(Item.Domain.values()).forEach(d->domains.add(d.name()));
		Map<String, String> allStartDates = new HashMap<>(),allStartTimes = new HashMap<>();
		domains.forEach(d->{allStartDates.put(d, justadate.format(CheckoutForm.dateFormatter));allStartTimes.put(d, justadate.format(CheckoutForm.timeFormatter));});
		CheckoutForm form = new CheckoutForm(allStartDates, "Check", allStartTimes, "",null);
		return form;
	}
	
	User initMB() {		
		userManager.bookMobileBreakfast("dextermorgan");
		return userManager.findUserByUsername("tripster").get();
	}
	
	void initValidCart() {
		orderController.clearCart(cart);
		/**
		 * cart contains:
		 * - event:
		 *    1 x buffet
		 *    10 x deco (tischdecke)
		 *    4 x service
		 * - mb:
		 * 	  1 x choice
		 * - party:
		 *    3 x sp
		 *    5 x Food
		 * - rca:
		 *    6 x cook
		 */
		
		List<Item> d = catalog.findByDomain(Domain.EVENTCATERING);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.BUFFET) && p.getName().equals("Luxus")).findFirst().get(), 1, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.EQUIPMENT) && p.getName().equals("Tischdecke")).findFirst().get(), 10, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Service-Personal")).findFirst().get(), 4, cart);


		d = catalog.findByDomain(Domain.MOBILE_BREAKFAST);
		 
		orderController.orderMobileBreakfast(Optional.of(initMB().getUserAccount()),
				new MobileBreakfastForm(
						(BreakfastItem) d.stream().filter(p -> p.getName().equals("Kuchen")).findFirst().get(),
						(BreakfastItem) d.stream().filter(p -> p.getName().equals("Tee")).findFirst().get(),
						"true", "true", "true", "true", "true", LocalTime.of(7, 30).format(DateTimeFormatter.ISO_LOCAL_TIME)), cart, null);

		d = catalog.findByDomain(Domain.PARTYSERVICE);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.SPECIAL_OFFERS) && p.getName().equals("Sushi Abend")).findFirst().get(), 3, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.FOOD) && p.getName().equals("Vegane Platte")).findFirst().get(), 5, cart);

		d = catalog.findByDomain(Domain.RENT_A_COOK);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Koch/Köchin pro 10 Personen")).findFirst().get(), 6, cart);
		
	}

	void initInvalidCart() {
		orderController.clearCart(cart);
		/**
		 * cart contains:
		 * - event:
		 *    1 x buffet
		 *    11 x deco (tischdecke) -invalid
		 *    4 x service
		 *    11 x cook - invalid
		 * - rac:
		 *    6 x cook
		 *    7 x service - invalid
		 */
		List<Item> d = catalog.findByDomain(Domain.EVENTCATERING);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.BUFFET) && p.getName().equals("Luxus")).findFirst().get(), 1, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.EQUIPMENT) && p.getName().equals("Tischdecke")).findFirst().get(), 11, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Service-Personal")).findFirst().get(), 4, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Koch/Köchin pro 10 Personen")).findFirst().get(), 11, cart);


		d = catalog.findByDomain(Domain.RENT_A_COOK);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Koch/Köchin pro 10 Personen")).findFirst().get(), 6, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Service-Personal")).findFirst().get(), 7, cart);
	}

	@Test
	void validateCarts() {
		initContext();
		
		CheckoutForm form = initForm();
		Map<Domain, List<String>> validations;
	
		//valid carts:
		initValidCart();
		validations = orderManager.validateCarts(cart.getDomainItems("_"), form);
		assert validations.isEmpty();

		//invalid carts:
		initInvalidCart();
		// every order:
		validations = orderManager.validateCarts(cart.getDomainItems("_"), form);
		assert validations.get(Domain.EVENTCATERING).stream().anyMatch(s->s.contains("6")&&s.contains("Koch")); 
		assert validations.get(Domain.EVENTCATERING).stream().anyMatch(s->s.contains("10")&&s.contains("Tischdecke")); 
		assert validations.get(Domain.EVENTCATERING).size() == 2;
		
		assert validations.get(Domain.RENT_A_COOK).stream().anyMatch(s->s.contains("6")&&s.contains("Personal")); 
		assert validations.get(Domain.RENT_A_COOK).size() == 1;
		assert validations.size() == 2;

		// only spec order:
		validations = orderManager.validateCarts(cart.getDomainItems(Domain.EVENTCATERING.name()), form);
		assert validations.get(Domain.EVENTCATERING).stream().anyMatch(s->s.contains("6")&&s.contains("Koch")); 
		assert validations.get(Domain.EVENTCATERING).stream().anyMatch(s->s.contains("10")&&s.contains("Tischdecke")); 
		assert validations.get(Domain.EVENTCATERING).size() == 2;
		assert validations.size() == 1;
	
	}

	@Test
	void createOrders() {
		
		CheckoutForm form = initForm();
		User user = userManager.findUserByUsername("hans").get();
		
		MampfOrder order;
		List<MampfOrder> orders;

		//buy all:
		initContext();
		initValidCart();
		orders = orderManager.createOrders(cart.getDomainItems("_"), form, user);
		
		
		assert orders.size() == 4;
		assert orders.stream().allMatch(o -> o.isCompleted());
		order = orders.stream().filter(o->o.getDomain().equals(Item.Domain.EVENTCATERING)).findFirst().get();
		//eventorder:
		assert order.getOrderLines().toList().size() == 3;
		assert order.getEmployees().size() == 4;
		assert order.getEmployees().get(0).getBooked().contains(order);
		assert order.getStartDate().equals(form.getStartDateTime(Item.Domain.EVENTCATERING));
		//assert order.getEndDate().equals(form.getStartDateTime(Item.Domain.EVENTCATERING))
		assert order.getAdress().equals(user.getAddress());
		assert order instanceof EventOrder;
		
		//party:
		order = orders.stream().filter(o->o.getDomain().equals(Item.Domain.PARTYSERVICE)).findFirst().get();
		assert order.getOrderLines().toList().size() == 2;
		assert order instanceof EventOrder;
		
		//mb:
		//Item.Domain d= Item.Domain.MOBILE_BREAKFAST;
		//User boss = userManager.findUserByUsername("dextermorgan").get();
		order = orders.stream().filter(o->o.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)).findFirst().get();
		assert order.getAdress().equals(userManager.findUserByUsername("dextermorgan").get().getAddress());
		assert order.getEmployees().isEmpty();
		assert order.getOrderLines().get().anyMatch(ol->ol.getProductName().equals("Kuchen"));
		assert order.getOrderLines().get().anyMatch(ol->ol.getProductName().equals("Tee"));
		assert order.getOrderLines().toList().size() == 2;
		assert order instanceof MBOrder;
		
		//still available:
		assert employeeManager.getFreeEmployees(justadate, justadate.plus(EventOrder.EVENTDURATION), Employee.Role.COOK).size() == 0;
		assert employeeManager.getFreeEmployees(justadate, justadate.plus(EventOrder.EVENTDURATION),Employee.Role.SERVICE).size() == 2;
		//employees assigned:
		assert orders.stream().anyMatch(o -> o.getDomain().equals(Item.Domain.EVENTCATERING) && o.getEmployees().size() == 4);
		assert orders.stream().anyMatch(o -> o.getDomain().equals(Item.Domain.RENT_A_COOK) && o.getEmployees().size() == 6);
		

		initContext();
		initValidCart();
		//buy spec:
		orders = orderManager.createOrders(cart.getDomainItems(Domain.RENT_A_COOK.name()), form, user);
		assert orders.size() == 1;
		assert orders.stream().allMatch(o -> o.isCompleted());
		assert orders.stream().anyMatch(o -> o.getDomain().equals(Item.Domain.RENT_A_COOK) && o.getOrderLines().toList().size() == 1);
		

	}
	
	@Test
	void deleteAll() {
		CheckoutForm form = initForm();
		User user = userManager.findUserByUsername("hans").get();
		
		MampfOrder order;
		List<MampfOrder> orders;

		//buy all:
		initContext();
		initValidCart();
		orders = orderManager.createOrders(cart.getDomainItems("_"), form, user);
		orderManager.deleteOrder(orders.remove(0));
		
		List<MampfOrder> delOrders = orderManager.getOrderManagement().findBy(OrderStatus.CANCELLED).toList();
		int a=1;
	}
	
}






