package mampf.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import mampf.catalog.MampfCatalog;

@SpringBootTest
public class MampfCartTests {

	@Autowired MampfCatalog catalog;
	
	private MampfCart cart;
	void MampfCart() {
		cart = new MampfCart();
	}
	
	/*...*/
}
