package mampf.revenue;

import mampf.order.MampfCart;
import mampf.order.MampfOrderManager;
import org.hamcrest.Matchers;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class RevenueControllerIntegrationTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	MampfOrderManager mampfOrderManager;

	@Test
	void testEmptyRevenue() throws Exception {
		mvc.perform(get("/revenue").with(user("hansWurst").roles("BOSS")))
				.andExpect(view().name("revenue"))
				.andExpect(model().attribute("total", Money.of(0, "EUR")))
				.andExpect(model().attribute("startDateString", LocalDate.now().toString()))
				.andExpect(model().attribute("endDateString", LocalDate.now().plusMonths(1).toString()));
	}

}
