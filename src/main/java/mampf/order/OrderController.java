package mampf.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

import javax.money.MonetaryAmount;
import javax.persistence.criteria.Order;
import javax.validation.Valid;

import mampf.inventory.Inventory;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfCart.DomainCart;
import mampf.user.Company;
import mampf.user.User;
import mampf.user.UserManagement;
import org.salespointframework.order.Cart;
import org.salespointframework.order.CartItem;

import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import mampf.Util;
import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.catalog.Item.Domain;
import mampf.catalog.MampfCatalog;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
@SessionAttributes("mampfCart")
public class OrderController {

    private UserManagement userManagement;
    private final MampfOrderManager orderManager;
    public static final TemporalAmount delayForEarliestPossibleBookingDate = Duration.ofHours(5);

    public class BreakfastMappedItems extends Item {

        private final LocalTime breakfastTime;
        private List<DayOfWeek> weekDays = new ArrayList<>();
        private final String adress;
        private final BreakfastItem beverage, dish;
        private final long amount;

        public BreakfastMappedItems(User user, LocalDateTime startDate, LocalDateTime endDate,
                MobileBreakfastForm form) {

            super("Mobile Breakfast für " + form.getBeverage().getName() + " und " + form.getDish().getName(),
                    BreakfastItem.BREAKFAST_PRICE, Item.Domain.MOBILE_BREAKFAST, Item.Category.FOOD, "temp");

            // get weekdays:
            List<String> days = form.getDays().keySet().stream().filter(k -> form.getDays().get(k)). // get all marked
                                                                                                     // weekdays
            // map(k->DayOfWeek.valueOf(k.toUpperCase())). //convert string to weekday
                    map(String::toUpperCase).collect(Collectors.toList());
            for (DayOfWeek weekDay : DayOfWeek.values()) {
                if (days.contains(weekDay.name())) {
                    weekDays.add(weekDay);
                }
            }
            // get breakfasttime:
            breakfastTime = form.getTime();

            // get start and end Dates:
            Optional<Company> company = userManagement.findCompany(user.getId());
            startDate = LocalDateTime.of(company.get().getBreakfastDate().get(), LocalTime.of(0, 0));
            endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(), LocalTime.of(0, 0));
            setDescription("vom " + startDate.toLocalDate() + " bis " + endDate.toLocalDate() + "je: " + weekDays
                    + " um " + breakfastTime);
            Optional<User> boss = userManagement.findUserById(company.get().getBossId());
            if (boss.isPresent()) {
                adress = boss.get().getAddress();
            } else {
                adress = "err";
            }

            // get items:
            beverage = form.getBeverage();
            dish = form.getDish();

            // set amount:
            List<UniqueMampfItem> totalItems = orderManager.convertToInventoryItems(MBOrder.getItems(startDate, endDate,
                    startDate, endDate, new HashSet<>(weekDays), breakfastTime, List.of(beverage.getId(), dish
                            .getId())));
            if (totalItems.isEmpty()) {
                amount = 0;
            } else {
                amount = totalItems.get(0).getAmount().longValue();
            }

        }

        public LocalTime getBreakfastTime() {
            return breakfastTime;
        }

        public BreakfastItem getDish() {
            return dish;
        }

        public String getAdress() {
            return adress;
        }

        public BreakfastItem getBeverage() {
            return beverage;
        }

        public List<DayOfWeek> getWeekDays() {
            return weekDays;
        }

        public long getAmount() {
            return amount;
        }
    }

    public OrderController(MampfOrderManager orderManager, UserManagement userManagement) {
        this.orderManager = orderManager;
        this.userManagement = userManagement;
    }

    /* CART */

    @ModelAttribute("mampfCart")
    MampfCart initializeCart() {
        MampfCart cart = new MampfCart();
        /*MampfCatalog catalog = orderManager.getCatalog();
        cart.addToCart(catalog.findByName("Dekoration").get().findFirst().get(), Quantity.of(10));
        cart.addToCart(catalog.findByName("Tischdecke").get().findFirst().get(), Quantity.of(10));
        catalog.findByName("Koch/Köchin pro 10 Personen").forEach(i->cart.addToCart(i, Quantity.of(3)));
        catalog.findByName("Service-Personal").forEach(i->cart.addToCart(i, Quantity.of(4)));
        cart.addToCart(catalog.findByName("Luxus").get().findFirst().get(), Quantity.of(2));
        */
        return cart;
        
    }

    /**
     * adds item to cart
     */
    @PostMapping("/cart")
    public String addItem(@RequestParam("pid") Item item, @RequestParam("number") int number,
            @ModelAttribute("mampfCart") MampfCart mampfCart) {
        if (number > 0) {
            mampfCart.addToCart(item, Quantity.of(number));
        }
        return "redirect:/catalog/" + item.getDomain().toString().toLowerCase();
    }

    /**
     * view cart
     */
    @GetMapping("/cart")
    public String basket(Model model, @ModelAttribute("mampfCart") MampfCart mampfCart) {

        mampfCart.resetCartDate();
        Map<Item.Domain, DomainCart> domains = mampfCart.getStuff();
        model.addAttribute("domains", domains);
        model.addAttribute("total", mampfCart.getTotal(null));
        return "cart";
    }

    /**
     * clears cart
     */
    @PostMapping("cart/clear")
    public String clearCart(@ModelAttribute("mampfCart") MampfCart mampfCart) {
        mampfCart.clear();
        return "redirect:/cart";
    }

    /**
     * handles adding and removing the amount of a cartitem
     */
    @PostMapping("cart/setNewAmount")
    public String addCartItem(@RequestParam String cartItemId, @RequestParam int newAmount,
            @ModelAttribute("mampfCart") MampfCart mampfCart) {

        CartItem cartItem = mampfCart.getCartItem(cartItemId);
        if (cartItem != null) {
            mampfCart.updateCart(cartItem, newAmount);
        }
        return "redirect:/cart";
    }

    /**
     * adds breakfast choice to cart as one cartitem
     */
    @PostMapping("/cart/add/mobile-breakfast")
    public String orderMobileBreakfast(@LoggedIn Optional<UserAccount> userAccount, @Valid MobileBreakfastForm form,
            @ModelAttribute("mampfCart") MampfCart mampfCart, RedirectAttributes redirectAttributes) {

        String redirect = "redirect:/mobile-breakfast";
        if (userAccount.isEmpty()) {
            return "redirect:/login";
        }
        // ERRORS:
        String error = "error";
        if (form.getBeverage() == null) {
            redirectAttributes.addFlashAttribute(error, "Kein Getränk ausgewählt");
            return redirect;
        }

        if (form.getDish() == null) {
            redirectAttributes.addFlashAttribute(error, "Nichts zum Essen ausgewählt");
            return redirect;
        }

        if (!orderManager.hasBookedMB(userAccount.get())) {
            redirectAttributes.addFlashAttribute(error, "für diesen Monat wurde kein Mobile Breakfast bestellt");
            return redirect;
        }

        // TODO: MB error: mb is already booked
        // TODO: MB error: outdated (duplicate code from BreakfastmappedItems
        // constructor...)(check if time now is after choiceTimeEnd)

        User user = userManagement.findUserByUserAccount(userAccount.get().getId()).get();
        Optional<Company> company = userManagement.findCompany(user.getId());
        LocalDateTime startDate = LocalDateTime.of(company.get().getBreakfastDate().get(), LocalTime.of(0, 0));
        LocalDateTime endDate = LocalDateTime.of(company.get().getBreakfastEndDate().get(), LocalTime.of(0, 0));

        BreakfastMappedItems mbItem = new BreakfastMappedItems(user, startDate, endDate, form);

        mampfCart.addToCart(mbItem, Quantity.of(mbItem.getAmount()));
        mampfCart.updateMBCart(startDate, endDate);

        return "redirect:/cart";
    }

    /**
     * view buying site
     */
    @GetMapping("/pay/{domain}")
    public String chooseToBuy(Model model, @PathVariable String domain, @ModelAttribute("form") CheckoutForm form,
            @ModelAttribute("mampfCart") MampfCart mampfCart) {

        if (mampfCart.isEmpty()) {
            return "redirect:/cart";
        }

        Item.Domain domainChoosen = null;
        String domainStr = Util.renderDomainName(domain);
        for (Item.Domain d : Item.Domain.values()) {
            if (d.name().equals(domain) || domainStr.equals(Util.renderDomainName(d.name()))) {
                domainChoosen = d;
                break;
            }
        }
        mampfCart.resetCartDate();
        model.addAttribute("validations", new HashMap<String, List<String>>());
        return buyCart(domainChoosen, model, mampfCart, form);
    }

    /**
     * buy cart(s)
     */
    @PostMapping("/checkout")
    public String buy(Model model, @RequestParam(name = "reload") Optional<Boolean> reload,
            @Valid @ModelAttribute("form") CheckoutForm form, Errors result, Authentication authentication,
            @ModelAttribute("mampfCart") MampfCart mampfCart) {

        mampfCart.updateCart(form);
        Map<String, List<String>> validationsStr = new HashMap<>();

        if (reload.isPresent()) {
            model.addAttribute("validations", validationsStr);
            return buyCart(form.getDomainChoosen(), model, mampfCart, form);
        }

        for (Item.Domain domain : form.getDomains()) {
            if (!CheckoutForm.domainsWithoutForm.contains(domain.name())) {
                LocalDateTime startDate = form.getStartDateTime(domain);
                LocalDateTime endDate = form.getEndDateTime(domain);
                String errVar = "allStartDates[" + domain.name() + "]";
                String errDomain = "CheckoutForm.startDate";

                if (startDate == null || endDate == null) {
                    result.rejectValue(errVar, errDomain + ".Invalid", "Bitte Datum eingeben!");
                    continue;
                }
                if (startDate.isBefore(LocalDateTime.now().plus(delayForEarliestPossibleBookingDate))) {
                    result.rejectValue(errVar, errDomain + ".NotFuture", "Das Datum muss in der Zukunft liegen!");
                }
                if (startDate.isAfter(endDate)) {
                    result.rejectValue(errVar, errDomain + ".idk", "keine negativen Bestellungen erlaubt!");
                }
            }
        }

        Map<Item.Domain, DomainCart> carts = mampfCart.getDomainItems(form.getDomainChoosen());
        Map<Item.Domain, List<String>> validations = new HashMap<>();
        if (!result.hasErrors()) {
            validations = orderManager.validateCarts(carts);
        }

        if (!validations.isEmpty()) {
            result.rejectValue("generalError", "CheckoutForm.generalError.NoStuffLeft",
                    "There is no free stuff or personal for the selected time left!");
            // TODO: append errors to form instead of to model
            validations.forEach((domain, list) -> validationsStr.put(domain.name(), list));
        }

        Optional<User> user = userManagement.findUserByUsername(authentication.getName());
        if (user.isEmpty()) {
            result.rejectValue("generalError", "CheckoutForm.generalError.NoLogin",
                    "There was an error during your login process");
        }

        if (result.hasErrors()) {
            model.addAttribute("validations", validationsStr);
            return buyCart(form.getDomainChoosen(), model, mampfCart, form);
        }

        orderManager.createOrders(carts, form, user.get());

        List<Item.Domain> domains = new ArrayList<>();
        for (Item.Domain domain : carts.keySet()) {
            domains.add(domain);
        }
        for (Item.Domain domain : domains) {
            mampfCart.removeCart(domain);
        }
        // TODO: success handling (some fancy stuff)

        return "redirect:/userOrders";
    }

    private String buyCart(Item.Domain domain, Model model, MampfCart mampfCart, CheckoutForm form) {
        form.setDomainChoosen(domain);
        Map<Item.Domain, DomainCart> domains = mampfCart.getDomainItems(domain);
        model.addAttribute("canSubmit", domains.values().stream().allMatch(
                cart -> (cart.getStartDate() != null && cart.getEndDate() != null)));
        model.addAttribute("domains", domains);
        model.addAttribute("total", mampfCart.getTotal(domain));
        model.addAttribute("form", form);
        return "buy_cart";
    }

    /* ORDERS */

    /**
     * lists every orders ever made for adminuser
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('BOSS')")
    public String orders(Model model) {

        model.addAttribute("stuff", getSortedOrders(Optional.empty(), Optional.empty()));
        return "orders";
    }

    /**
     * shows Order
     */
    @GetMapping("/orders/detail/{orderId}")
    public String editOrder(@PathVariable String orderId, Model model) {

        Optional<MampfOrder> order = orderManager.findById(orderId);
        if (order.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("order", order.get());
        model.addAttribute("isMB", order.get() instanceof MBOrder);
        return "ordersDetail";
    }

    /**
     * lists orders of a user
     */
    @GetMapping("/userOrders")
    public String orderUser(Model model, @LoggedIn Optional<UserAccount> userAccount) {
        if (userAccount.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("stuff", getSortedOrders(Optional.of(MampfOrder.comparatorSortByCreation), userAccount));
        return "orders";
    }

    /**
     * boss: delete a order
     */
    @PreAuthorize("hasRole('BOSS')")
    // @DeleteMapping(value = "/orders/delete?id={orderId}")
    @GetMapping("/orders/delete/{orderId}")
    public String deleteOrder(@PathVariable Optional<String> orderId) {
        String redirect = "redirect:/";
        if (orderId.isEmpty()) {
            return redirect;
        }
        Optional<MampfOrder> order = orderManager.findById(orderId.get());
        if (order.isEmpty()) {
            return redirect;
        }
        orderManager.deleteOrder(order.get());
        return "redirect:/orders";
    }

    private Map<String, List<MampfOrder>> getSortedOrders(Optional<Comparator<MampfOrder>> comp,
            Optional<UserAccount> userAccount) {

        Map<String, List<MampfOrder>> stuff = new LinkedHashMap<>();
        List<MampfOrder> orders = new ArrayList<>();
        if (userAccount.isPresent()) {
            orders = orderManager.findByUserAcc(userAccount.get());
        } else {
            orders = orderManager.findAll();
        }
        if (comp.isPresent()) {
            orders.stream().sorted(comp.get());
        } else {
            orders.stream().sorted();
        }
        //inserted sorted keys:
        for (MampfOrder order : orders) {
            String insertTo;
            if (comp.isPresent()) {
                insertTo = order.getDateCreated().toLocalDate().toString();
                if (order.getDateCreated().isAfter(LocalDateTime.now().minus(Duration.ofHours(1)))) {
                    insertTo = "soeben erstellt";
                }
            } else {
                insertTo = order.getStartDate().toLocalDate().toString();
            }

            if (stuff.containsKey(insertTo)) {
                stuff.get(insertTo).add(order);
            } else {
                List<MampfOrder> newList = new LinkedList<>();
                newList.add(order);
                stuff.put(insertTo, newList);
            }
        }
        //check
        /*if (comp.isPresent()) {
            stuff.forEach((k, list) -> list.stream().sorted(comp.get()));
        } else {
            stuff.forEach((k, list) -> list.stream().sorted());
        }*/
        return stuff;
    }
}
