package mampf.user;

import mampf.catalog.CatalogController;
import mampf.catalog.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {
	@Autowired
	UserController userController;
	@Autowired
	MockMvc mvc;

	@Autowired
	UserManagement userManagement;

	private MultiValueMap<String, String> createAllRequestParamsForRegisterNewUser(
			String username,
			String firstname,
			String lastname,
			String password,
			String address,
			String email,
			UserRole role,
			String accessCode,
			String companyName
	) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("username", username);
		map.add("firstname", firstname);
		map.add("lastname", lastname);
		map.add("password", password);
		map.add("address", address);
		map.add("email", email);
		map.add("role", role.name());
		map.add("accessCode", accessCode);
		map.add("companyName", companyName);

		return map;
	}
	@Test
	void testMissingAccessCodeForEmployee() throws Exception{

		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewUser(
				"UserName",
				"MyFirstName",
				"Lastname",
				"topsecret",
				"Hinter dem Busch",
				"test@example.com",
				UserRole.EMPLOYEE,
				"",
				""
		);

		mvc.perform(post("/register")
				.params(map))
				.andDo(print())
				.andExpect(view().name("register"))
				.andExpect(model().attributeHasFieldErrorCode("form", "accessCode", "RegistrationForm.accessCode.NotEmpty"));

	}

	@Test
	void testExistingUsername() throws Exception{

		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewUser(
				"hans",
				"MyFirstName",
				"Lastname",
				"topsecret",
				"Hinter dem Busch",
				"test@example.com",
				UserRole.EMPLOYEE,
				"",
				""
		);

		mvc.perform(post("/register")
				.params(map))
				.andDo(print())
				.andExpect(view().name("register"))
				.andExpect(model().attributeHasFieldErrorCode("form", "username", "RegistrationForm.username.exists"));

	}


	@Test
	void testCreationOfIndividualSuccessful() throws Exception{

		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewUser(
				"UserName",
				"MyFirstName",
				"Lastname",
				"topsecret",
				"Hinter dem Busch",
				"test@example.com",
				UserRole.INDIVIDUAL,
				"",
				""
		);

		mvc.perform(post("/register")
				.params(map))
				.andDo(print())
				.andExpect(view().name("redirect:/"));

	}

	@Test
	void testEmailExistsAlready() throws Exception{

		MultiValueMap<String, String> map = createAllRequestParamsForRegisterNewUser(
				"tester",
				"MyFirstName",
				"Lastname",
				"topsecret",
				"Hinter dem Busch",
				"wurst@example.com",
				UserRole.INDIVIDUAL,
				"",
				""
		);

		mvc.perform(post("/register")
				.params(map))
				.andDo(print())
				.andExpect(view().name("register"))
				.andExpect(model().attributeHasFieldErrorCode("form", "email", "RegistrationForm.email.exists"));

	}

	@Test
	public void testGetAllUsersAsBoss() throws Exception{
		mvc.perform(get("/users"))
				.andExpect(model().size((int) userManagement.findAll().stream().count()));
	}

}
