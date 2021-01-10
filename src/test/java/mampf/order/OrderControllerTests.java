package mampf.order;


import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTests {
	
	@Autowired MockMvc mvc;
	@Autowired OrderController controller;
	@Autowired MampfCatalog catalog;
	
	/**
	 * logged in user will be redirected to catalog when adding items
	 */
	@Test
	@WithMockUser(username="dude",roles={"INDIVIDUAL"})
	void redirectsToCatalog() throws Exception {
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		Item addItem = catalog.findByName("Basic").toList().iterator().next();
		map.add("pid", addItem.getId().toString());
		map.add("number", "2");
		mvc.perform(post("/cart")
						.params(map))
						.andDo(print())
						.andExpect(header().string("Location", endsWith("/catalog/"+addItem.getDomain().toString().toLowerCase())));
	}
	
	
	/**
	 * only logged in user can add stuff to cart /see cart
	 */
	@Test
	void redirectsToLoginPageCart() throws Exception {
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("pid", catalog.findByName("Basic").toList().iterator().next().getId().toString());
		map.add("number", "2");
		mvc.perform(post("/cart")
						.params(map))
						//.andDo(print())
						.andExpect(header().string("Location", endsWith("/login")));
		
		mvc.perform(get("/cart"))
				.andExpect(status().isFound()) //
				.andExpect(header().string("Location", endsWith("/login")));
		
	}
	
	/**
	 * only logged in user can see their orders
	 */
	@Test
	void redirectsToLoginPageForSecuredResource() throws Exception {

		mvc.perform(get("/userOrders")) //
				.andExpect(status().isFound()) //
				.andExpect(header().string("Location", endsWith("/login")));
	}
	
	/**
	 * only admin-user can see all orders
	 */
	@Test
	@WithMockUser(username = "boss", roles = "BOSS")
	void returnsModelAndViewForSecuredUriAfterAuthentication() throws Exception {

		mvc.perform(get("/orders")) //
				.andExpect(status().isOk()) //
				.andExpect(model().attributeExists("stuff"));
	}

	
}
