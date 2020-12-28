package mampf.order;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import mampf.user.User;
import mampf.user.UserManagement;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.catalog.Item;
import mampf.catalog.Item.Domain;
import mampf.catalog.Item.Category;
import mampf.catalog.BreakfastItem;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfOrderManager.ValidationState;

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
	EmployeeManagement employeeManager;
	@Autowired
	UserManagement userManager;

	private MampfCart cart;

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
		 * Employee-Stock:
		 * 6 x Cook
		 * 6 x Service
		 */
		for (Employee employee : employeeManager.findAll()) {
			List<MampfOrder> booked = employee.getBooked();
			booked.stream().peek(xd -> xd.getEmployees().clear());
		}

		employeeManager.findAll().stream().peek(xdd -> xdd.getBooked().clear());

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
		orderController.orderMobileBreakfast(Optional.of(mock(UserAccount.class)),
				new MobileBreakfastForm(
						(BreakfastItem) d.stream().filter(p -> p.getName().equals("Kuchen")).findFirst().get(),
						(BreakfastItem) d.stream().filter(p -> p.getName().equals("Tee")).findFirst().get(),
						"true", "true", "true", "true", "true", LocalDateTime.now().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)), cart);

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
		 *    10 x cook - invalid
		 *    7 x service - invalid
		 */
		List<Item> d = catalog.findByDomain(Domain.EVENTCATERING);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.BUFFET) && p.getName().equals("Luxus")).findFirst().get(), 1, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.EQUIPMENT) && p.getName().equals("Tischdecke")).findFirst().get(), 11, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Service-Personal")).findFirst().get(), 4, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Koch/Köchin pro 10 Personen")).findFirst().get(), 11, cart);


		d = catalog.findByDomain(Domain.RENT_A_COOK);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Koch/Köchin pro 10 Personen")).findFirst().get(), 10, cart);
		orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals("Service-Personal")).findFirst().get(), 7, cart);

	}

	@Test
	void validateCarts() {
		initContext();
		LocalDateTime startDate = LocalDateTime.now();
		TemporalAmount a = OrderController.delayForEarliestPossibleBookingDate;
		startDate = startDate.plus(a);

		CheckoutForm form = new CheckoutForm(startDate.toLocalDate(), "BAR", startDate.toLocalTime(), "", "");
		Map<Domain, List<ValidationState>> validations;

		//valid carts:
		initValidCart();
		validations = orderManager.validateCarts(cart.getDomainItems("_"), form);
		assert validations.isEmpty();

		//invalid carts:
		initInvalidCart();
		// every order:
		validations = orderManager.validateCarts(cart.getDomainItems("_"), form);
		assert validations.get(Domain.EVENTCATERING).contains(ValidationState.NO_STOCK);
		assert validations.get(Domain.EVENTCATERING).contains(ValidationState.NO_PERSONAL);
		assert validations.get(Domain.EVENTCATERING).size() == 2;
		assert validations.get(Domain.RENT_A_COOK).contains(ValidationState.NO_PERSONAL);
		assert validations.get(Domain.RENT_A_COOK).size() == 2;
		assert validations.size() == 2;

		// only spec order:
		validations = orderManager.validateCarts(cart.getDomainItems(Domain.EVENTCATERING.name()), form);
		assert validations.get(Domain.EVENTCATERING).contains(ValidationState.NO_STOCK);
		assert validations.get(Domain.EVENTCATERING).contains(ValidationState.NO_PERSONAL);
		assert validations.get(Domain.EVENTCATERING).size() == 2;
		assert validations.size() == 1;
	}

	@Test
	void createOrders() {
		LocalDateTime startDate = LocalDateTime.now();
		TemporalAmount a = OrderController.delayForEarliestPossibleBookingDate;
		startDate = startDate.plus(a);
		User user = userManager.findUserByUsername("hans").get();
		CheckoutForm form = new CheckoutForm(startDate.toLocalDate(), "BAR", startDate.toLocalTime(), "", "");
		List<MampfOrder> orders;

		//buy all:
		initContext();
		initValidCart();
		orders = orderManager.createOrders(cart.getDomainItems("_"), form, user);
		assert orders.size() == 4;
		assert orders.stream().allMatch(order -> order.isCompleted());
		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.EVENTCATERING) && order.getOrderLines().toList().size() == 3);

		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.PARTYSERVICE) && order.getOrderLines().toList().size() == 2);
		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.RENT_A_COOK) && order.getOrderLines().toList().size() == 1);

		//still available:
		assert employeeManager.getFreeEmployees(startDate, Employee.Role.COOK).size() == 0;
		assert employeeManager.getFreeEmployees(startDate, Employee.Role.SERVICE).size() == 2;
		//employees assigned:
		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.EVENTCATERING) && order.getEmployees().size() == 4);
		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.RENT_A_COOK) && order.getEmployees().size() == 6);
		//reduced inventory items:
		assert inventory.findByName("Tischdecke").get().getQuantity().isEqualTo(Quantity.of(0));


		//rollback:
		initContext();
		initValidCart();
		//buy spec:
		orders = orderManager.createOrders(cart.getDomainItems(Domain.RENT_A_COOK.name()), form, user);
		assert orders.size() == 1;
		assert orders.stream().allMatch(order -> order.isCompleted());
		assert orders.stream().anyMatch(order -> order.getDomain().equals(Item.Domain.RENT_A_COOK) && order.getOrderLines().toList().size() == 1);


	}


}






