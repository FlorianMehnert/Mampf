package mampf.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;


@AutoConfigureMockMvc
@SpringBootTest
public class EmployeeControllerTest {

	@Autowired
	MockMvc mvc;

	private MultiValueMap<String, String> createAllRequestParamsForRegisterNewEmployee(
			String username,
			String firstname,
			String lastname,
			String password,
			Employee.Role role
	) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username", username);
		map.add("firstname", firstname);
		map.add("lastname", lastname);
		map.add("password", password);
		map.add("role", role.name());

		return map;
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void test_createEmployee() throws Exception{
		mvc.perform(get("/intern/employees/add"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void test_register() throws Exception{
		mvc.perform(get("/intern/employees"))
				.andExpect(model().attributeExists("employees"))
				.andExpect(status().isOk());
	}

	/*@Test
	@WithMockUser(roles = "BOSS")
	void test_registerNew() throws Exception {
		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewEmployee(
				"test_username",
				"test_firstname",
				"test_lastname",
				"123",
				Employee.Role.COOK
		);
		mvc.perform(post("/intern/employees/add"))
				//.params(map)
				.andExpect(model().hasNoErrors())
				.andExpect(status().is3xxRedirection());
		//TODO create employee to test the form -> find out how to connect to Controller
	}*/
}
