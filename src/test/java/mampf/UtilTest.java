package mampf;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.employee.EmployeeManagement;
import mampf.inventory.Inventory;
import mampf.inventory.InventoryController;
import mampf.inventory.UniqueMampfItem;
import mampf.order.MampfOrderManager;
import mampf.order.OrderController;
import mampf.user.UserController;
import mampf.user.UserManagement;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.util.AssertionErrors.assertTrue;

public class UtilTest {
	Item i1 = new Item("a", Money.of(BigDecimal.valueOf(10), "EUR"), Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT, "a");
	Item i2 = new Item("aa", Money.of(BigDecimal.valueOf(10), "EUR"), Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT, "a");
	Item i3 = new Item("ab", Money.of(BigDecimal.valueOf(10), "EUR"), Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT, "a");


	@Test
	void compareTwoToStringsTest(){
		assertTrue("two identical Strings should return 0 but " + Util.compareTwoToStrings(i1, i1)
				+ " was returned", Util.compareTwoToStrings(i1, i1) == 0);
		assertTrue("a compared with ab should return any int below 0 but "
				+ Util.compareTwoToStrings(i1, i2) + " was returned", Util.compareTwoToStrings(i1, i2) < 0);
		assertTrue("aa compared with a should return any int above 0 but "
				+ Util.compareTwoToStrings(i2, i1) + " was returned", Util.compareTwoToStrings(i2, i1) > 0);
		assertTrue("a compared with ab should return any int below 0 but "
				+ Util.compareTwoToStrings(i1, i3) + " was returned", Util.compareTwoToStrings(i1, i3) < 0);
		assertTrue("ab compared with aa should return 1 but "
				+ Util.compareTwoToStrings(i3, i2) + " was returned", Util.compareTwoToStrings(i3, i2) == 1);
		assertTrue("aa compared with ab should return -1 but "
				+ Util.compareTwoToStrings(i3, i2) + " was returned", Util.compareTwoToStrings(i2, i3) == -1);
	}

	String d1 = "";
	String d2 = "aasd";
	String d3 = "eventcatering";
	String d4 = "partyservice";
	String d5 = "mobile-breakfast";
	String d6 = "rent-a-cook";

	@Test
	void parseDomainTest(){
		assertTrue("empty Strings should return null", Util.parseDomainEnum(d1) == null);
		assertTrue("invalid domains should return null", Util.parseDomainEnum(d2) == null);
		assertTrue(d3 + " should be " + Item.Domain.EVENTCATERING, Util.parseDomainEnum(d3) == Item.Domain.EVENTCATERING);
		assertTrue(d4 + " should be " + Item.Domain.PARTYSERVICE, Util.parseDomainEnum(d4) == Item.Domain.PARTYSERVICE);
		assertTrue(d5 + " should be " + Item.Domain.MOBILE_BREAKFAST, Util.parseDomainEnum(d5) == Item.Domain.MOBILE_BREAKFAST);
		assertTrue(d6 + " should be " + Item.Domain.RENT_A_COOK, Util.parseDomainEnum(d6) == Item.Domain.RENT_A_COOK);
	}

	@Test
	void renderDomainNameTest(){
		assertTrue("renderDomainName should return an empty String when null is given",
				Util.renderDomainName(null).equals(""));
		assertTrue("renderDomainName should return an empty String when the given String has length 0",
				Util.renderDomainName("").equals(""));
	}

	@Test
	void listToStringTest(){
		List<String> list = List.of("a", "b", "c");
		assertTrue("some List should be propperly converted to a String using listToString method but returned "
						+ Util.listToString(list)
				, Util.listToString(list).equals("\na\nb\nc"));
	}


}
