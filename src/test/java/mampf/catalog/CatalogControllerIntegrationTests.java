package mampf.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@AutoConfigureMockMvc
public class CatalogControllerIntegrationTests {

	@Autowired
	CatalogController catalogController;
	@Autowired
	MockMvc mvc;

	@Test
	public void CatalogIntegrationTest() throws Exception {
		mvc.perform(get("/catalog")).andExpect(status().isOk());
	}

	@Test
	public void DomainIntegrationTest() throws Exception {
		mvc.perform(get("/catalog/eventcatering"))
			.andExpect(status().isOk())
			.andExpect(view().name("catalog"))
			.andExpect(model().attributeExists("domainTitle"))
			.andExpect(model().attributeExists("catalog"));
	}

	@Test 
	void MobileBreakfastIntegrationTest() throws Exception {
		mvc.perform(get("/mobile-breakfast"))
			.andExpect(status().isOk())
			.andExpect(view().name("mobile-breakfast"))
			.andExpect(model().attributeExists("domainTitle"))
			.andExpect(model().attributeExists("categories"));
	}

}
