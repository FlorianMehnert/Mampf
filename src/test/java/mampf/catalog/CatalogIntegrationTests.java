package mampf.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CatalogIntegrationTests {

	@Autowired
	MampfCatalog catalog;

	@Test
	void onLoad(){
		assertThat(catalog).isNotNull();
	}

	@Test
	void catalogSave(){
		addItem();
		assertThat(catalog.count()).isGreaterThan(1);
	}

	@Test
	void findByDomain(){
		assertThat(catalog.findByDomain(Item.Domain.EVENTCATERING).size()).isGreaterThan(1);
	}

	@Test
	void findByCategory(){
		assertThat(catalog.findByCategory(Item.Category.BUFFET).size()).isGreaterThan(1);
	}

	private void addItem(){
		Item item = createItem();
		catalog.save(item);
	}

	private Item createItem() {
		return new Item(
			"Test", 
			Money.of(2, "EUR"), 
			Item.Domain.EVENTCATERING, 
			Item.Category.BUFFET, 
			"Description"
		);
	}
}
