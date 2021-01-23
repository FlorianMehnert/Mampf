package mampf.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.salespointframework.quantity.Quantity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mampf.catalog.Item.Category;
import mampf.catalog.Item.Domain;
import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;

@SpringBootTest
class MampfCartTests {

	@Autowired MampfCatalog catalog;
	
	private MampfCart cart = new MampfCart();
	private LocalDateTime justadate = LocalDateTime.now().plus(OrderController.delayForEarliestPossibleBookingDate).plus(Duration.ofDays(2)).withHour(0).withMinute(0);
	private CheckoutForm form = initForm();
	void MampfCart() {}
	
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
      return new CheckoutForm(allStartDates, "Check", allStartTimes, allEndTimes, null, null);
  }
	@Test
	void addToCart() {
	    cart.clear();
	    
	    cart.addToCart(catalog.findByName("Tischdecke").stream().findFirst().get(), Quantity.of(1));
	    cart.addToCart(catalog.findByName("Dekoration").stream().findFirst().get(), Quantity.of(23));
	    cart.addToCart(catalog.findByName("Vegane Platte").stream().findFirst().get(), Quantity.of(23));
	    catalog.findByName("Service-Personal").forEach(item->cart.addToCart(item, Quantity.of(4)));
	    
	    assertTrue(cart.getStuff().get(Domain.EVENTCATERING).get().anyMatch(i->i.getProductName().equals("Tischdecke")));
	    assertTrue(cart.getStuff().get(Domain.EVENTCATERING).get().anyMatch(i->i.getProductName().equals("Dekoration")));
	    assertTrue(cart.getStuff().get(Domain.EVENTCATERING).get().anyMatch(i->i.getProductName().equals("Service-Personal")));
	    assertTrue(cart.getStuff().get(Domain.PARTYSERVICE).get().anyMatch(i->i.getProductName().equals("Vegane Platte")));
	    assertTrue(cart.getStuff().get(Domain.RENT_A_COOK).get().anyMatch(i->i.getProductName().equals("Service-Personal")));
	}
	
	
	@Test
	void removeFromCart() {
	    cart.clear();
      cart.addToCart(catalog.findByName("Tischdecke").stream().findFirst().get(), Quantity.of(1));
      cart.addToCart(catalog.findByName("Dekoration").stream().findFirst().get(), Quantity.of(23));
      
      cart.removeFromCart(cart.getDomainCart(Domain.EVENTCATERING).get().filter(i->i.getProductName().equals("Tischdecke")).findFirst().get());
      assertTrue(cart.getDomainCart(Domain.EVENTCATERING).get().filter(i->i.getProductName().equals("Tischdecke")).findFirst().isEmpty(),"cartitem with Tischdecke should no longer exist");
      cart.removeFromCart(cart.getDomainCart(Domain.EVENTCATERING).get().filter(i->i.getProductName().equals("Dekoration")).findFirst().get());
      assertTrue(cart.getStuff().isEmpty(),"cartitme Dekoration should no longer exist");
      
	}
	
	@Test
	void getPrice() {
	    //of a DomainCart
	    cart.clear();
	    /**
	     * form:
	     * start: justadate
	     * end: justadate + 2 hours
	     * cart:
	     * 3 x person 
	     * 3 x basic
	     */
	    Item a = catalog.findByDomain(Domain.EVENTCATERING).stream().filter(i->i.getName().equals("Service-Personal")).findFirst().get();
	    Item b = catalog.findByName("Basic").stream().filter(i->i.getCategory().equals(Category.BUFFET)).findFirst().get();
      
	    Quantity abc = Quantity.of(3);
	    cart.addToCart(a,abc);
	    cart.addToCart(b, abc);
	    cart.updateCart(form);
	    assertEquals(cart.getDomainCart(Domain.EVENTCATERING).getPrice(), 
	            (a.getPrice().multiply(2*3)).add(b.getPrice().multiply(3)),"getPrice does not return the required amount");
	}
}















