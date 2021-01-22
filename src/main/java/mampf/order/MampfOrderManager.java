package mampf.order;

import mampf.Util;
import mampf.catalog.Item;
import mampf.catalog.StaffItem;
import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;
import mampf.catalog.MampfCatalog;
import mampf.employee.Employee;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfCart.DomainCart;
import mampf.order.OrderController.BreakfastMappedItems;
import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.payment.Cash;
import org.salespointframework.payment.Cheque;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.LineItemFilter;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;
import org.salespointframework.order.OrderLine;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {

    @Bean
    LineItemFilter filter() {
        return item -> false;
    }

    private final OrderManagement<MampfOrder> orderManagement;
    private final Inventory inventory;
    private final MampfCatalog catalog;
    private final EmployeeManagement employeeManagement;
    private final UserManagement userManagement;

    public MampfOrderManager(OrderManagement<MampfOrder> orderManagement, Inventory inventory,
            EmployeeManagement employeeManagement, UserManagement userManagement, MampfCatalog catalog) {
        this.orderManagement = orderManagement;
        this.inventory = inventory;
        this.employeeManagement = employeeManagement;
        this.catalog = catalog;
        this.userManagement = userManagement;
    }

    /**
     * returns if the given userAccount can book mobile breakfast:
     * Check if the user-(employee)s company has booked Mobile Breakfast
     * false if not so
     *
     * @param userAccount
     * @return
     */
    public boolean hasBookedMB(UserAccount userAccount) {

        //the user is no valid user:
        Optional<User> user = userManagement.findUserByUserAccount(userAccount.getId());
        if (user.isEmpty()) {
            return false;
        }
        //the user is no employee:
        Optional<Company> company = userManagement.findCompany(user.get().getId());
        if (company.isEmpty()) {
            return false;
        }
        Optional<User> boss = userManagement.findUserById(company.get().getBossId());
        //the company of the user has not booked mb:
        if(!company.get().hasBreakfastDate() || company.get().getBreakfastDate().isEmpty() || boss.isEmpty()) {
            return false;
        }
        //the user already has ordered their choice:
        return findByTimeSpan(Optional.of(userAccount), LocalDateTime.of(company.get().getBreakfastDate().get(),LocalTime.MIN), 
                LocalDateTime.of(company.get().getBreakfastEndDate().get(),LocalTime.MIN)).stream().
                noneMatch(order->order instanceof MBOrder&&order.getAdress().equals(boss.get().getAddress()));

    }

    /**
     * Checks if requested items (cart items)/personal are available for the given
     * time
     *  checks if MB is in-time
     *  returns list of validations
     * (domainspec.), never null
     *
     * @param carts
     * @return
     */
    public Map<Item.Domain, List<String>> validateCarts(UserAccount userAccount, Map<Item.Domain, DomainCart> carts) {
        // each domain can have mutliple errormessages:
        Map<Item.Domain, List<String>> validations = new EnumMap<>(Item.Domain.class);

        // get base inventory of all finite items:
        List<UniqueMampfItem> baseInv = new ArrayList<>(inventory.findAll().filter(i -> !Util.infinity.contains(i.getCategory())).toList());
        // init base carts:
        List<DomainCart> baseCarts = new ArrayList<>();
        // init employees:
        Map<Employee.Role, Quantity> baseEmp = new EnumMap<>(Employee.Role.class);
        for(Employee.Role role : Employee.Role.values())
            baseEmp.put(role, employeeManagement.getEmployeeAmount(role));

        for (Map.Entry<Domain, DomainCart> entry : carts.entrySet()) {
            Domain domain = entry.getKey();
            DomainCart cart = entry.getValue();
            LocalDateTime startDate = cart.getStartDate(), endDate = cart.getEndDate();

            // cart should be updated correctly
            if (startDate == null || endDate == null) {
                updateValidations(validations, domain, "fehlende zeitangabe");
                continue;
            }

            // check for MB 'you are too late'-error:
            if (domain.equals(Domain.MOBILE_BREAKFAST) && !hasBookedMB(userAccount)) {
                updateValidations(validations, domain, "");
                continue;
            }

            // get available Items:
            List<UniqueMampfItem> inventorySnapshot = getFreeItems(baseInv, baseCarts, baseEmp, startDate, endDate);

            for (CartItem cartitem : cart) {
                // de-map mapper-cartitems:
                for (CartItem checkitem : createCheckItems(cartitem)) {
                    Optional<Item> catalogItem = catalog.findById(checkitem.getProduct().getId());
                    if (catalogItem.isEmpty()) {
                        updateValidations(validations, domain, "Item nicht vorhanden:" + cartitem.getProductName());
                        continue;
                    }

                    Optional<UniqueMampfItem> inventoryItem = checkForAmount(inventorySnapshot, checkitem);
                    if (inventoryItem.isPresent()) {
                        String validationState = "Keine verfügbare Auswahl von '" + catalogItem.get().getName()+"' : " +
                                " verbleibende Anzahl: "
                                + inventoryItem.get().getQuantity().getAmount();
                        updateValidations(validations, domain, validationState);
                    }
                }
            }
            baseCarts.add(cart);
        }
        return validations;
    }

    /**
     * creates orders for the given items (cart items) and saves them in the SP
     * orderManagement sets personal to the orders
     *
     * @param carts
     * @param form
     * @param user
     * @return
     */
    public List<MampfOrder> createOrders(Map<Item.Domain, DomainCart> carts, CheckoutForm form, User user) {

        List<MampfOrder> orders = new ArrayList<>();
        for (Map.Entry<Domain, DomainCart> entry : carts.entrySet()) {
            Domain domain = entry.getKey();
            DomainCart cart = entry.getValue();

            // get Dates:
            LocalDateTime startDate = cart.getStartDate(), endDate = cart.getEndDate();

            MampfOrder order;
            if (domain.equals(Domain.MOBILE_BREAKFAST)) {
                order = createOrderMB(cart.iterator().next(), startDate, endDate, form, user.getUserAccount());

            } else {
                // create usual order:
                order = new EventOrder(user.getUserAccount(), createPayMethod(form.getPayMethod(), user
                        .getUserAccount()), domain, startDate, endDate, user.getAddress());

                cart.addItemsTo(order);
                if (hasStaff(cart)) {
                    setPersonalBooked((EventOrder) order, getPersonal(startDate, endDate));
                }
            }

            if (!orderManagement.payOrder(order)) {
                return orders;
            }

            orderManagement.completeOrder(order);

            orderManagement.save(order);
            orders.add(order);
        }

        return orders;

    }

    /**
     * creates and returns a "inventory snapshot" (a list of UniqueMampfItems) which
     * represents the inventory for a given time span calculates the snapshot from
     * the actual inventory checks every order for ordered items/amount
     *
     * @param baseInv
     * @param baseCarts
     *
     * @param fromDate
     * @param toDate
     * @return
     */
    public List<UniqueMampfItem> getFreeItems(List<UniqueMampfItem> baseInv, List<DomainCart> baseCarts, Map<Employee.Role, Quantity> baseEmp,
            LocalDateTime fromDate, LocalDateTime toDate) {
        //INIT:
        List<UniqueMampfItem> res = new ArrayList<>();
        // a "copy" of baseInv
        // items which has to be returned needs to be the same structure like baseInv:
        // also, the quantity will be modified during next steps
        baseInv.forEach(i -> res.add(new UniqueMampfItem(i.getItem(), Quantity.of(i.getAmount().longValue()))));
        // a "copy" of baseEmp
        // issue: items("STAFF") are requests which are not unique and refer to a specific resource type (employee),
        //        not a inventory resource
        // idea: calculate over requests the lasting resource,
        //       but update requests(items) depending on lasting resource as inventory item
        Map<Employee.Role, Quantity> personalLeft = new EnumMap<>(Employee.Role.class);
        baseEmp.forEach((role,q)->{personalLeft.put(role, Quantity.of(q.getAmount().longValue()));});
        /*-----------------------------*/
        //GET:
        List<UniqueMampfItem> bookedItems = getBookedItems(fromDate, toDate);
        List<UniqueMampfItem> cartItems = getBookedItemsFromCart(baseCarts,fromDate,toDate);
        /*----------------------------*/
        //CALCULATE:
        Optional<UniqueMampfItem> actionItem;
        List<UniqueMampfItem> actionItems;
        for (UniqueMampfItem resItem : res) {for (int n = 0; n < 2; n++) {
            if(n == 0) {
                actionItems = bookedItems;
            }else {
                actionItems = cartItems;
            }

            actionItem = Optional.empty();
            for (UniqueMampfItem bI : actionItems)if (bI.getProduct().equals(resItem.getProduct())) {actionItem = Optional.of(bI);break;}
            
            if(actionItem.isEmpty()) {
                continue;
            }
            // substract:
            if(actionItem.get().getCategory().equals(Category.STAFF)) {
                Employee.Role role = ((StaffItem)actionItem.get().getProduct()).getType();
                personalLeft.put(role,reduceValidationQuantity(personalLeft.get(role),actionItem.get().getQuantity()));
            }else if (!Util.infinity.contains(resItem.getCategory())) {
                resItem.setQuantity(reduceValidationQuantity(resItem.getQuantity(),actionItem.get().getQuantity()));
            }
        
        }}
        /*----------------------------*/
        //UPDATE STAFF:
        
        res.stream().filter(i->i.getCategory().equals(Category.STAFF)).collect(Collectors.toList()).forEach(j->{
            j.setQuantity(personalLeft.get(((StaffItem)j.getProduct()).getType()));
        });

        return res;
    }
    
    private List<UniqueMampfItem> getBookedItemsFromCart(List<DomainCart> baseCarts, 
            LocalDateTime fromDate, 
            LocalDateTime toDate){
     // each cart has a time span
        //  when time span colliding with given time span
        //   add all conrete items (f.e. BreakfastMappedItem is no conrete item, but the content)
        List<UniqueMampfItem> cartItems = new ArrayList<>();
        for(DomainCart cart: baseCarts) {
            if(!MampfOrder.hasTimeOverlap(fromDate, toDate, cart.getStartDate(), cart.getEndDate())) {
                continue;
            }
            CartItem firstItem = cart.iterator().next(); //there are no empty carts
            if(((Item)firstItem.getProduct()).getDomain().equals(Domain.MOBILE_BREAKFAST)) {
                BreakfastMappedItems bfItem = (BreakfastMappedItems)firstItem.getProduct();
                Quantity mBquantity = Quantity.of(MBOrder.getAmount(fromDate, toDate, cart.getStartDate(), cart.getEndDate(), bfItem.getWeekDays(), bfItem.getBreakfastTime()));
                
                cartItems.add(new UniqueMampfItem(bfItem.getBeverage(), mBquantity));
                cartItems.add(new UniqueMampfItem(bfItem.getDish(), mBquantity));
            }else {
                cart.forEach(cartItem -> 
                cartItems.add(new UniqueMampfItem((Item) cartItem.getProduct(), cartItem.getQuantity())));
            }
            
        }
        return cartItems;
    }
    
    private Quantity reduceValidationQuantity(Quantity origin, Quantity sub) {
        if(origin.isEqualTo(Quantity.of(0))) {
            return origin;
        }
        if(sub.isGreaterThan(origin)) {
            return Quantity.of(0);
        }

        return origin.subtract(sub);
    }
    /**
     * creates and returns a list of all ordered items for a time span
     * 
     * @param fromDate
     * @param toDate
     * @return
     */
    public List<UniqueMampfItem> getBookedItems(LocalDateTime fromDate, LocalDateTime toDate) {
        return convertToInventoryItems(getOrderItems(fromDate, toDate));
    }

    /**
     * converts productidentifiers and quantitys to a list of uniquemampfitems
     *
     * @param itemMap
     * @return
     */
    public List<UniqueMampfItem> convertToInventoryItems(Map<ProductIdentifier, Quantity> itemMap) {
        List<UniqueMampfItem> res = new ArrayList<>();
        itemMap.forEach((id, q) -> {
            Optional<Item> catalogItem = catalog.findById(id);
            if (catalogItem.isPresent()) {
                res.add(new UniqueMampfItem(catalogItem.get(), q));
            }
        });
        return res;
    }

    /**
     * creates and returns a list of every Order of a useraccount
     *
     * @param account
     * @return
     */
    public List<MampfOrder> findByUserAcc(UserAccount account) {
        List<MampfOrder> res = new ArrayList<>();
        for (MampfOrder order : orderManagement.findBy(account)) {
            res.add(order);
        }
        return res;
    }

    /**
     * deletes order
     */
    public void deleteOrder(MampfOrder order) {
        for (MampfOrder order_ : findAll()) {
            if (order.equals(order_)) {
                if (order_ instanceof EventOrder) {
                    order.getEmployees().forEach(e -> e.removeBookedOrder((EventOrder) order));
                }
                orderManagement.delete(order_);
                return;
            }

        }
    }

    public Optional<MampfOrder> findById(String orderId) {
        return orderManagement.findAll(Pageable.unpaged()).filter(order -> order.getId().getIdentifier().equals(
                orderId)).get().findFirst();
    }
    
    public Streamable<MampfOrder> findByTimeSpan(Optional<UserAccount> userAccount, LocalDateTime fromDate, LocalDateTime toDate){
        Streamable<MampfOrder> orders;
        if(userAccount.isEmpty()) {
            orders = orderManagement.findAll(Pageable.unpaged());
        }else {
            orders = orderManagement.findBy(userAccount.get());
        }
        return orders.filter(order->order.hasTimeOverlap(fromDate, toDate));
    }
    public List<MampfOrder> findAll() {
        return orderManagement.findBy(OrderStatus.COMPLETED).toList();
    }
    
    /**
     * only unit testing purpose
     *
     * @return
     */
    public OrderManagement<MampfOrder> getOrderManagement() {
        return orderManagement;
    }

    public MampfCatalog getCatalog() {return catalog;}
    // TODO: createPayMethod needs to be updated
    private PaymentMethod createPayMethod(String payMethod, UserAccount userAccount) {
        PaymentMethod method = Cash.CASH;
        if (payMethod.equals("Check")) {
            method = new Cheque(userAccount.getUsername(), userAccount.getId().getIdentifier(), "checknummer 1",
                    userAccount.getFirstname(), LocalDateTime.now(), "MampfBank", "Lindenallee 12", "1223423478");
        }
        return method;
    }

    private boolean hasStaff(Cart cart) {
        return (cart.get().anyMatch(cartitem -> ((Item) cartitem.getProduct()).getCategory().equals(
                Item.Category.STAFF)));

    }

    private Map<Employee.Role, Integer> getPersonalAmount(LocalDateTime startDate, LocalDateTime endDate) {
        Map<Employee.Role, Integer> personalLeftSize = new HashMap<>();
        for (Map.Entry<Employee.Role, List<Employee>> entry : getPersonal(startDate, endDate).entrySet()) {
            personalLeftSize.put(entry.getKey(), entry.getValue().size());
        }
        return personalLeftSize;
    }

    private Map<Employee.Role, List<Employee>> getPersonal(LocalDateTime startDate, LocalDateTime endDate) {
        Map<Employee.Role, List<Employee>> personalLeft = new HashMap<>();

        for (Employee.Role role : Employee.Role.values()) {
            List<Employee> xcy = employeeManagement.getFreeEmployees(startDate, endDate, role);
            personalLeft.put(role, xcy);

        }
        return personalLeft;
    }

    private List<CartItem> createCheckItems(CartItem cartitem) {

        Item item = ((Item) cartitem.getProduct());
        List<CartItem> checkitems = new ArrayList<>();
        if (item.getDomain().equals(Item.Domain.MOBILE_BREAKFAST)) {
            BreakfastMappedItems bfItem = ((BreakfastMappedItems) item);
            checkitems.add(new Cart().addOrUpdateItem(bfItem.getBeverage(), cartitem.getQuantity()));
            checkitems.add(new Cart().addOrUpdateItem(bfItem.getDish(), cartitem.getQuantity()));
        } else {
            checkitems.add(cartitem);
        }
        return checkitems;
    }

    // checkForAmount returns an empty Optional if the checked quantity is valid
    private Optional<UniqueMampfItem> checkForAmount(List<UniqueMampfItem> inventorySnapshot, CartItem checkitem) {

        Optional<UniqueMampfItem> inventoryItem = inventorySnapshot.stream().filter(i ->
                i.getProduct().equals(checkitem.getProduct())).findFirst();
        Item catalogItem = ((Item) checkitem.getProduct());


        if (inventoryItem.isPresent() &&
            (catalogItem.getCategory().equals(Category.STAFF) || !Util.infinity.contains(catalogItem.getCategory()))&&
            (inventoryItem.get().getQuantity().isLessThan(checkitem.getQuantity()))){
            return inventoryItem;
        }
            /*if (catalogItem.getCategory().equals(Category.STAFF)) {
                // calculate personalAmount:
                Employee.Role staffType = ((StaffItem) catalogItem).getType();
                Integer amountLeft = 0;
                Iterator<UniqueMampfItem> it = inventorySnapshot.stream().filter(i->i.getCategory().equals(Category.STAFF)&&
                        ((StaffItem)i.getItem()).getType().equals(staffType)).iterator();
                while(it.hasNext()) {amountLeft+=it.next().getAmount().intValue();}

                if (checkitem.getQuantity().isGreaterThan(Quantity.of(amountLeft))) {
                    return Optional.of(new UniqueMampfItem(catalogItem, Quantity.of(amountLeft)));
                }

            } else if(!Util.infinity.contains(catalogItem.getCategory())){ // sonarcube logic: 'else if {}' is allowed...
                if (inventoryItem.get().getQuantity().isLessThan(checkitem.getQuantity())) {
                    return inventoryItem;
                }
            }*/
        //}
        return Optional.empty();
    }

    private void setPersonalBooked(EventOrder order, Map<Employee.Role, List<Employee>> personalLeft) {
        Item item;
        Quantity itemQuantity;

        for (OrderLine orderline : order.getOrderLines()) {
            Optional<Item> itemOptional = catalog.findById(orderline.getProductIdentifier());
            if (itemOptional.isEmpty()) {
                continue;
            }
            item = itemOptional.get();
            itemQuantity = orderline.getQuantity();

            if (item.getCategory().equals(Item.Category.STAFF)) {
                List<Employee> freeStaff = personalLeft.get(((StaffItem) item).getType());
                Employee employee;
                for (int i = 0; i < itemQuantity.getAmount().intValue(); i++) {
                    if (!freeStaff.isEmpty()) {
                        employee = freeStaff.remove(0);
                        employee.setBooked(order);
                        order.addEmployee(employee);
                    }
                }
            }
        }
    }

    private MBOrder createOrderMB(CartItem bfCartItem, LocalDateTime startDate, LocalDateTime endDate,
            CheckoutForm form, UserAccount account) {

        BreakfastMappedItems bfItem = (BreakfastMappedItems) bfCartItem.getProduct();
        Cart cart = new Cart();
        for (CartItem checkItem : createCheckItems(bfCartItem)) {
            cart.addOrUpdateItem(checkItem.getProduct(), checkItem.getQuantity());
        }

        MBOrder order = new MBOrder(account, createPayMethod(form.getPayMethod(), account), startDate, endDate, bfItem);

        order.addChargeLine(bfCartItem.getPrice(), "mobile breakfast total");

        cart.addItemsTo(order);
        return order;

    }

    private void updateValidations(Map<Item.Domain, List<String>> validations, Item.Domain domain, String state) {
        if (validations.containsKey(domain)) {
            validations.get(domain).add(state);
        } else {
            List<String> stateList = new ArrayList<>();
            stateList.add(state);
            validations.put(domain, stateList);
        }
    }

    // all ordered items for a time span
    private Map<ProductIdentifier, Quantity> getOrderItems(LocalDateTime fromDate, LocalDateTime toDate) {

        Map<ProductIdentifier, Quantity> res = new HashMap<>();
        orderManagement.findBy(OrderStatus.COMPLETED).forEach(order -> order.getItems(fromDate, toDate).
        // some typical map updating:
                forEach((id, q) -> {
                    if (res.containsKey(id))
                        res.get(id).add(q);
                    else
                        res.put(id, q);
                }));
        return res;
    }

}
