package mampf.inventory;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.Product;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest

public class InventoryTest {
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
	public void load() {
		assertThat(mampfCatalog).isNotNull();
		assertThat(uniqueInventory).isNotNull();
		assertThat(inventoryController).isNotNull();
		assertThat(inventory).isNotNull();

	}

	@Test
	public void itemsIntegrationTest() throws Exception {
		mvc.perform(get("/inventory"))
				.andExpect(status().is3xxRedirection());
	}


//	public LinkedMultiValueMap<String, String> getParams() {
//		LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
//		Item item = (Item) inventory.getInventory().findAll().toList().get(0).getProduct();
//		requestParams.add("item", String.valueOf(item));
//		requestParams.add("number", "1");
//		return requestParams;
//	}
//
//	@Test
//	public void addFoodItem() throws Exception {
//		this.mvc.perform(post("/inventory/add")
//				.params(getParams())).andDo(print())
//				.andExpect(status().is3xxRedirection());
//	}

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
	void InventoryExtendsUniqueInventory(){
		assertTrue("d", inventory != null);
	}

	@Test
	void reduceAmountTest(){
		Product product = inventory.findAll().toList().get(0).getProduct();
		String name = product.getName();
		Quantity quantity = Quantity.of(1);
		Optional<UniqueInventoryItem> theItem = inventory.findByProduct(product);
		theItem.ifPresent(uniqueInventoryItem -> uniqueInventoryItem.decreaseQuantity(quantity));
		assertTrue("Die Quantity von dem item wird nicht reduziert", inventory.findByName(name).get().getQuantity().isGreaterThan(Quantity.of(0)));
	}

}