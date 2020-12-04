package mampf.inventory;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import org.salespointframework.core.DataInitializer;
import org.salespointframework.inventory.UniqueInventory;
import org.salespointframework.inventory.UniqueInventoryItem;
import org.salespointframework.quantity.Quantity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Order(20)
class InventoryInitializer implements DataInitializer {

	private final UniqueInventory<UniqueInventoryItem> inventory;
	private final MampfCatalog catalog;

	InventoryInitializer(UniqueInventory<UniqueInventoryItem> inventory, MampfCatalog catalog) {

		Assert.notNull(inventory, "Inventory must not be null!");
		Assert.notNull(catalog, "VideoCatalog must not be null!");

		this.inventory = inventory;
		this.catalog = catalog;
	}

	@Override
	public void initialize() {
		catalog.findAll().forEach(item -> {

					if (item.getName().equals("Dekoration")) {
						inventory.save(new UniqueInventoryItem(item, Quantity.of(20)));
					} else if (item.getName().equals("Tischdecke")) {
						inventory.save(new UniqueInventoryItem(item, Quantity.of(25)));
					} else if (item.getCategory() == Item.Category.FOOD) {
						inventory.save(new UniqueInventoryItem(item, Quantity.of(-1)));
					} else {
						inventory.save(new UniqueInventoryItem(item, Quantity.of(10)));
					}

				}
		);
	}
}
