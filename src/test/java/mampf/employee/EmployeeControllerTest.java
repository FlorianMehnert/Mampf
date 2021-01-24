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
			String firstName,
			String lastName,
			Employee.Role role
	) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username", username);
		map.add("firstName", firstName);
		map.add("lastName", lastName);
		map.add("role", role.name());

		return map;
	}

	private MultiValueMap<String, String> createAllRequestParamsForEditNewEmployee(
			String firstName,
			String lastName) {
		MultiValueMap<String, String> map_edit = new LinkedMultiValueMap<>();
		map_edit.add("firstName", firstName);
		map_edit.add("lastName", lastName);

		return map_edit;
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

	@Test
	@WithMockUser(roles = "BOSS")
	void test_registerNew() throws Exception{
		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewEmployee(
				"test_username",
				"test1",
				"test2",
				Employee.Role.COOK
		);
		mvc.perform(post("/intern/employees/add")
				.params(map))
				.andExpect(model().hasNoErrors())
				.andExpect(view().name("redirect:/intern/employees"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void test_edit() throws Exception{
		mvc.perform(get("/intern/employees/{id}", "11"))
				.andExpect(model().attributeExists("employee"))
				.andExpect(model().attributeExists("form"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void test_editEmployee() throws Exception{
		MultiValueMap<String, String> map_edit = createAllRequestParamsForEditNewEmployee(
				"Elsa",
				"Pato"
		);
		mvc.perform(post("/intern/employees/{id}", "11")
				.params(map_edit))
				.andExpect(model().hasNoErrors())
				.andExpect(view().name("redirect:/intern/employees"));
	}

	/*@Test
	@WithMockUser(roles = "BOSS")
	void test_filter() throws Exception{
		String filter = "Anna";
		mvc.perform(get("/intern/employees", filter))
				.andExpect(model().attributeExists("filter"))
				.andExpect(model().attributeExists("employees"))
				.andExpect(status().isOk());
	}*/

	@Test
	@WithMockUser(roles = "BOSS")
	void test_delete() throws Exception{
		mvc.perform(get("/intern/employees/deleteEmployee/{id}", "11"))
				.andExpect(status().is3xxRedirection())
				.andExpect(view().name("redirect:/intern/employees"));
	}
}
