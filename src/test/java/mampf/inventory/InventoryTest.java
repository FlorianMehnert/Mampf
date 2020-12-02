package mampf.inventory;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InventoryTest {
	@Autowired
	private MampfCatalog mampfCatalog;

	@Autowired
	private UniqueInventory<UniqueInventoryItem> uniqueInventory;

	@Autowired
	private InventoryController inventory;

	@Autowired
	MockMvc mvc;

	@Test
	public void load() throws Exception{
		assertThat(mampfCatalog).isNotNull();
		assertThat(uniqueInventory).isNotNull();
		assertThat(inventory).isNotNull();
	}

	@Test
	public void itemsIntegrationTest () throws Exception {
		mvc.perform(get("/inventory"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	public void addFoodItem() throws Exception {
		Item item = new Item();
		int posAmount = 1;
		int negAmount = -1;
		this.mvc.perform(post("/inventory/add")
				.param("number", String.valueOf(posAmount))
				.param("pid", String.valueOf(item)))
				.andExpect(status().isOk());
	}
}