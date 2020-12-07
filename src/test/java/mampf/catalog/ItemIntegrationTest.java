package mampf.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ItemIntegrationTest {
	private Item item;

	ItemIntegrationTest(){
		this.item = new Item("Test", Money.of(2, "EUR"), Item.Domain.EVENTCATERING, Item.Category.BUFFET, "Description");
	}

	@Test
	void getDomain(){
		assertThat(item.getDomain()).isEqualTo(Item.Domain.EVENTCATERING);
	}

	@Test
	void getCategory(){
		assertThat(item.getCategory()).isEqualTo(Item.Category.BUFFET);
	}
}
