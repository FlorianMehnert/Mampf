package mampf.inventory;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.inventory.InventoryItemIdentifier;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest

class InventoryTest {
	@Autowired
	private MampfCatalog mampfCatalog;

	@Autowired
	private UniqueInventory<UniqueInventoryItem> uniqueInventory;

	@Autowired
	private InventoryController inventoryController;

	@Autowired
	private Inventory inventory;

	@Autowired
	MockMvc mvc;

	@Test
	 void load() {
		assertThat(mampfCatalog).isNotNull();
		assertThat(uniqueInventory).isNotNull();
		assertThat(inventoryController).isNotNull();
		assertThat(inventory).isNotNull();

	}

	@Test
	 void itemsIntegrationTest() throws Exception {
		mvc.perform(get("/inventory"))
				.andExpect(status().is3xxRedirection());
	}


	public LinkedMultiValueMap<String, String> getParams(Item item, int amount) {
		LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
		requestParams.add("item", String.valueOf(item));
		requestParams.add("number", String.valueOf(amount));
		return requestParams;
	}

	@Test
	 void addFoodItem() throws Exception {
		UniqueInventoryItem someItem = inventory.findAll().toList().get(0);
		Quantity amountBefore = someItem.getQuantity();
		InventoryItemIdentifier id = someItem.getId();
		System.out.println(someItem.getProduct());
		this.mvc.perform(post("/inventory/add")
				.params(getParams((Item) someItem.getProduct(), 1)));
		Optional<UniqueInventoryItem> inventoryItemOptional = inventory.findById(id);
		Quantity amountAfter = inventoryItemOptional.get().getQuantity();
		boolean decrease = amountBefore.isGreaterThan(amountAfter);
		assertTrue("the amount of the item added did not get reduced", decrease);
	}

	@Test
	void preventsPublicAccessForStockOverview() throws Exception {

		mvc.perform(get("/inventory"))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")));
	}

	@Test
	@WithMockUser(username = "hansWurst", roles = "BOSS", password = "123")
	void stockIsAccessibleForAdmin() throws Exception {

		mvc.perform(get("/inventory"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "hansWurst", roles = "BOSS", password = "123")
	void stockWithNullCategory() throws Exception {

		mvc.perform(get("/inventory"))
				.andExpect(status().isOk());
	}

	@Test
	void InventoryExtendsUniqueInventory() {
		assertTrue("Inventory does not extend UniqueInventory",
				UniqueInventory.class.isAssignableFrom(Inventory.class));
	}

	@Test
	void reduceAmountTest() {
		//init some valid item from inventory
		UniqueInventoryItem uniqueInventoryItem = inventory.findAll().toList().get(0);
		Item item = (Item) uniqueInventoryItem.getProduct();
		Quantity quantity = uniqueInventoryItem.getQuantity();
		assertNotNull(item.getId());
		assertTrue("this item does not exist in inventory",
				inventory.reduceAmount(item, Quantity.of(1)).isPresent());
		Quantity quantity1 = inventory.reduceAmount(item, Quantity.of(1)).get().getQuantity();
		assertTrue("Die Quantity von dem item wird nicht reduziert", (quantity.isGreaterThan(quantity1)));
	}

	@Test
	void findByNameTest() {
		boolean isEmpty = inventory.findByName(null).equals(Optional.empty());
		//findByName using null
		assertTrue("findByName does not return Optional.empty when called with null", isEmpty);

		String someName = inventory.findAll().toList().get(0).getProduct().getName();
		Optional<UniqueInventoryItem> item = inventory.findByName(someName);
		boolean notEmpty = !item.equals(Optional.empty());
		//findByName using valid inputs
		assertTrue("findByName returns an empty Optional", notEmpty);

		item = inventory.findByName("this Item does not exist");
		isEmpty = item.equals(Optional.empty());
		//findByName using wrong inputs
		assertTrue("findByName does not return Optional.empty using some not included", isEmpty);
	}
}