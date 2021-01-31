package mampf.order;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
import mampf.order.MampfCart.DomainCart;
import mampf.order.OrderController.BreakfastMappedItems;

import org.salespointframework.quantity.Quantity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
@Transactional
@WithMockUser(username = "dude", roles = { "INDIVIDUAL" })
class MampfOrderManagerTests {

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

    private MampfCart cart = new MampfCart();
    private LocalDateTime justadate = LocalDateTime.now().plus(OrderController.delayForEarliestPossibleBookingDate)
            .plus(Duration.ofDays(2)).withHour(0).withMinute(0);

    MampfOrderManagerTests() {
    }

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
         * Personal-Stock: 6 x Cook 6 x Service
         */
        for (Employee employee : employeeManager.findAll()) {
            List<EventOrder> booked = employee.getBooked();
            booked.stream().peek(xd -> xd.getEmployees().clear());
        }

        employeeManager.findAll().stream().peek(xdd -> xdd.getBooked().clear());

        /*
         * User-Stock: 
         *  ind:
         *  - hans 
         *  admin: 
         *  - hansWurst 
         *  comp: 
         *  - dextermorgan 
         *  emp: -
         *  tripster
         */
        userManager.findAll().forEach(user -> {
            if (user.getCompany().isPresent())
                user.getCompany().get().resetCompany();
        });

        /*
         * Order-Stock is just empty
         */
        orderManager.findAll().forEach(order -> {
            orderManager.deleteOrder(order);
        });

    }

    CheckoutForm initForm() {
        List<String> domains = new ArrayList<>();
        List.of(Item.Domain.values()).forEach(d -> domains.add(d.name()));

        Map<String, String> allStartDates = new HashMap<>(), allStartTimes = new HashMap<>(),
                allEndTimes = new HashMap<>();
        domains.forEach(d -> {
            allStartDates.put(d, justadate.format(CheckoutForm.dateFormatter));
            allStartTimes.put(d, justadate.format(CheckoutForm.timeFormatter));
            allEndTimes.put(d, justadate.plus(Duration.ofHours(2)).format(CheckoutForm.timeFormatter));
        });
        return new CheckoutForm(allStartDates, "Check", allStartTimes, allEndTimes, null,null);
    }

    User initMB() {
        userManager.bookMobileBreakfast("dextermorgan");
        return userManager.findUserByUsername("tripster").get();
    }

    void initValidCart() {
        orderController.clearCart(cart);
        /**
         * cart contains: 
         * - event: 1 x buffet 10 x deco (tischdecke) 4 x service 
         * - mb: 1x choice 
         * - party: 3 x sp 5 x Food 
         * - rca: 6 x cook
         */

        List<Item> d = catalog.findByDomain(Domain.EVENTCATERING);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.BUFFET) && p.getName().equals(
                "Luxus")).findFirst().get(), 1, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.EQUIPMENT) && p.getName().equals(
                "Tischdecke")).findFirst().get(), 10, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Service-Personal")).findFirst().get(), 4, cart);

        d = catalog.findByDomain(Domain.MOBILE_BREAKFAST);

        //orderController.orderMobileBreakfast can only add valid mobilebreakfast orders
        //but for making sure that there can be a breakfastmappeditems instance, the object needs to be added manuell to the cart
        User user = initMB();
        Optional<Company> company = userManager.findCompany(user.getId());
        LocalDateTime startDate = LocalDateTime.of(company.get().getBreakfastDate().get(), LocalTime.of(0, 0));
        LocalDateTime endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(), LocalTime.of(0, 0));
        
        BreakfastMappedItems mbItem = orderController.new BreakfastMappedItems(startDate, endDate, 
                userManager.findUserById(company.get().getBossId()).get().getAddress(),
                new MobileBreakfastForm(
                        (BreakfastItem) d.stream().filter(p -> p.getName().equals("Kuchen")).findFirst().get(),
                        (BreakfastItem) d.stream().filter(p -> p.getName().equals("Tee")).findFirst().get(), "true", "true",
                        "false", "true", "true"));
        
        cart.addToCart(mbItem, Quantity.of(mbItem.getAmount()));
        cart.updateMBCart(startDate, endDate);

        d = catalog.findByDomain(Domain.PARTYSERVICE);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.SPECIAL_OFFERS) && p.getName()
                .equals("Sushi Abend")).findFirst().get(), 3, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.FOOD) && p.getName().equals(
                "Vegane Platte")).findFirst().get(), 5, cart);

        d = catalog.findByDomain(Domain.RENT_A_COOK);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Koch/Köchin pro 10 Personen")).findFirst().get(), 6, cart);

    }

    void initInvalidCart() {
        orderController.clearCart(cart);
        /**
         * cart contains: 
         * event: 
         * - 1 x buffet 11 x deco (tischdecke) -invalid 4 x
         * service 
         * - 11 x cook - invalid
         * rac: 6 x cook 7 x service - invalid
         */
        List<Item> d = catalog.findByDomain(Domain.EVENTCATERING);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.BUFFET) && p.getName().equals(
                "Luxus")).findFirst().get(), 1, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.EQUIPMENT) && p.getName().equals(
                "Tischdecke")).findFirst().get(), 11, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Service-Personal")).findFirst().get(), 4, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Koch/Köchin pro 10 Personen")).findFirst().get(), 11, cart);

        d = catalog.findByDomain(Domain.RENT_A_COOK);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Koch/Köchin pro 10 Personen")).findFirst().get(), 6, cart);
        orderController.addItem(d.stream().filter(p -> p.getCategory().equals(Category.STAFF) && p.getName().equals(
                "Service-Personal")).findFirst().get(), 7, cart);
    }

    @Test
    void validateCarts() {
        initContext();
        User user = userManager.findUserByUsername("tripster").get();
        CheckoutForm form = initForm();
        Map<Domain, List<String>> validations;

        // valid carts:
        initValidCart();
        cart.updateCart(form);
        validations = orderManager.validateCarts(user.getUserAccount(),cart.getDomainItems(null));
        assertTrue(validations.isEmpty(),"validCart should return empty validations");

        // invalid carts:
        user = userManager.findUserByUsername("hans").get();
        initInvalidCart();
        cart.updateCart(form);
        // every order:
        validations = orderManager.validateCarts(user.getUserAccount(),cart.getDomainItems(null));
        assertTrue(validations.get(Domain.EVENTCATERING).stream().anyMatch(s -> s.contains("Koch")));
        assertTrue(validations.get(Domain.EVENTCATERING).stream().anyMatch(s -> s.contains("Tischdecke")));
        assertEquals(validations.get(Domain.EVENTCATERING).size(),2,"Eventcatering should not be bookable");

        assertTrue(validations.get(Domain.RENT_A_COOK).stream().anyMatch(s -> s.contains("Personal")));
        assertTrue(validations.get(Domain.RENT_A_COOK).stream().anyMatch(s -> s.contains("Koch")));
        assertEquals(validations.get(Domain.RENT_A_COOK).size(),2,"Rentacook should not be bookale");
        assertEquals(2,validations.size());
        
        // only spec order:
        validations = orderManager.validateCarts(user.getUserAccount(),cart.getDomainItems(Domain.EVENTCATERING));
        assertTrue(validations.get(Domain.EVENTCATERING).stream().anyMatch(s -> s.contains("Koch")));
        assertTrue(validations.get(Domain.EVENTCATERING).stream().anyMatch(s -> s.contains("Tischdecke")));
        assertEquals(validations.get(Domain.EVENTCATERING).size(),2,"Eventcatering should not be bookable (domainCHoosen)");
        assertEquals(1,validations.size(),"only one not bookable when domainChoosen");

    }

    @Test
    void createOrders() {

        CheckoutForm form = initForm();
        User user = userManager.findUserByUsername("hans").get();

        MampfOrder order;
        List<MampfOrder> orders;

        // buy all:
        initContext();
        initValidCart();
        cart.updateCart(form);
        orders = orderManager.createOrders(cart.getDomainItems(null), form, user);

        assertEquals(orders.size(),4);
        assertTrue(orders.stream().allMatch(o -> o.isCompleted()));
        order = orders.stream().filter(o -> o.getDomain().equals(Item.Domain.EVENTCATERING)).findFirst().get();
        // eventorder:
        assertTrue( order instanceof EventOrder);
        assertEquals(order.getOrderLines().toList().size(),3,"orderlines should be assigned");
        assertEquals(order.getEmployees().size(),4,"employees should be assigned");
        assertTrue(order.getEmployees().get(0).getBooked().contains(order));
        assertEquals(order.getStartDate(),form.getStartDateTime(Item.Domain.EVENTCATERING));
        assertEquals(order.getAddress(),user.getAddress());

        // party:
        order = orders.stream().filter(o -> o.getDomain().equals(Item.Domain.PARTYSERVICE)).findFirst().get();
        assertTrue( order instanceof EventOrder);
        assertEquals(order.getOrderLines().toList().size(),2);
        

        // mb:
        order = orders.stream().filter(o -> o.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)).findFirst().get();
        assertTrue(order instanceof MBOrder,"mb orders should be a instance of mborder");
        assertEquals(order.getAddress(),userManager.findUserByUsername("dextermorgan").get().getAddress());
        assertTrue(order.getOrderLines().get().anyMatch(ol -> ol.getProductName().equals("Kuchen")));
        assertTrue(order.getOrderLines().get().anyMatch(ol -> ol.getProductName().equals("Tee")));
        assertEquals(order.getOrderLines().toList().size(),2,"mb orders should have the breakfastitems as orderlines");
        
        // still available:
        List<Employee> cooks = employeeManager.getFreeEmployees(justadate, justadate.plusHours(1), Employee.Role.COOK);
        List<Employee> others = employeeManager.getFreeEmployees(justadate, justadate.plusHours(2),
                Employee.Role.SERVICE);

        assertEquals(0,cooks.size(),"there should no free cook left for the given time");
        assertEquals(2,others.size(),"there should be two service left for the given time");
        
        initContext();
        initValidCart();
        cart.updateCart(form);
        // buy spec:
        orders = orderManager.createOrders(cart.getDomainItems(Domain.RENT_A_COOK), form, user);
        assertEquals(1,orders.size());
        assertTrue(orders.stream().allMatch(o -> o.isCompleted()));
        assertTrue(orders.stream().anyMatch(o -> o.getDomain().equals(Item.Domain.RENT_A_COOK) && o.getOrderLines().toList().size() == 1));

    }

    @Test
    void hasBookedMB() {
        initContext();
        Optional<User> emp = userManager.findUserByUsername("tripster");

        assertFalse(orderManager.hasBookedMB(emp.get().getUserAccount()), "mb was not booked for employee");
        initMB();
        assertTrue(orderManager.hasBookedMB(emp.get().getUserAccount()), "mb was booked for employee");
        initValidCart();
        CheckoutForm form = initForm();
        cart.updateCart(form);
        orderManager.createOrders(cart.getDomainItems(Item.Domain.MOBILE_BREAKFAST), form, emp.get());
        assertFalse(orderManager.hasBookedMB(emp.get().getUserAccount()), "the emp should not buy another mb order");
        
    }
    
    @Test 
    void getFreeItems() {
        initContext();
        
        // get base inventory of all finite items:
        List<UniqueMampfItem> baseInv = new ArrayList<>(inventory.findAll().filter(i -> !Inventory.infinity.contains(i.getCategory())).toList());
        // init base carts:
        List<DomainCart> baseCarts = new ArrayList<>();
        // init employees:
        Map<Employee.Role, Quantity> baseEmp = new EnumMap<>(Employee.Role.class);
        for(Employee.Role role : Employee.Role.values()) { 
            baseEmp.put(role, employeeManager.getEmployeeAmount(role));
        }
        
        
        assertEquals(orderManager.getFreeItems(baseInv, baseCarts, baseEmp, LocalDateTime.now(),LocalDateTime.now().plus(Duration.ofHours(1))).size(),
                baseInv.size(),
                "getFreeItems should keep track of the given inventory base");
        initValidCart();
        CheckoutForm form = initForm();
        cart.updateCart(form);
        orderManager.createOrders(cart.getDomainItems(null), form, userManager.findUserByUsername("hans").get());
        
        /*(still colliding)
         * [ ] (justadate)
         *   [ ] (last orders)
         */
        List<UniqueMampfItem> inventorySnapshot = orderManager.getFreeItems(baseInv, baseCarts, baseEmp, justadate.minus(Duration.ofDays(4)), justadate.plus(Duration.ofMinutes(1)));
        
        assertTrue(inventorySnapshot.stream().anyMatch(i->i.getQuantity().equals(Quantity.of(0))&&i.getProduct().getName().contains("Tischdecke")),"getFreeItems snapshot,Tischdecke should be 0");
        assertTrue(inventorySnapshot.stream().anyMatch(i->i.getQuantity().equals(Quantity.of(10))&&i.getProduct().getName().contains("Deko")),"getFreeItems snapshot,deko should be 10");
        assertTrue(inventorySnapshot.stream().anyMatch(i->i.getQuantity().equals(Quantity.of(2))&&i.getProduct().getName().contains("Personal")),"getFreeItems snapshot,Service should be 2");
        assertTrue(inventorySnapshot.stream().anyMatch(i->i.getQuantity().equals(Quantity.of(0))&&i.getProduct().getName().contains("Koch")),"getFreeItems snapshot,there should be no Cook left!");
        
    }
}





