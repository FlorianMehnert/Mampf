package mampf.inventory;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class InventoryTest {
	@Autowired
	private MampfCatalog mampfCatalog;

	@Autowired
	private UniqueInventory<UniqueInventoryItem> uniqueInventory;

	@Autowired
	private InventoryController inventory;

	@Test
	public void load() throws Exception{
		assertThat(mampfCatalog).isNotNull();
		assertThat(uniqueInventory).isNotNull();
		assertThat(inventory).isNotNull();
	}
}