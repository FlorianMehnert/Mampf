package mampf.user;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
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
		map.add("confirmPassword", password);
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
				.andExpect(view().name("register"))
				.andExpect(model().attributeHasFieldErrorCode("form", "accessCode", "RegistrationForm.accessCode.wrong"));

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
				.andExpect(view().name("register"))
				.andExpect(model().attributeHasFieldErrorCode("form", "email", "RegistrationForm.email.exists"));

	}

	@Test
	void testGetAllUsersAsBoss() throws Exception{
		mvc.perform(get("/users").with(user("hansWurst").roles("BOSS")))
				.andExpect(view().name("users"))
				.andExpect(model().attribute("pairs", Matchers.hasSize((int)userManagement.findAll().stream().count())));
	}

	@Test
	void testGetProfile() throws Exception{
		Optional<User> user = userManagement.findUserByUsername("hans");

		assertThat(user).isPresent();

		MvcResult result = mvc.perform(get("/userDetails/").with(user("hans").roles("INDIVIDUAL")))
				.andExpect(view().name("userDetails"))
				.andReturn();

		assertThat(result.getModelAndView().getModel().get("user").toString()).hasToString(user.get().toString());
	}

	@Test
	void testChangePassword() throws Exception{
		Optional<User> user = userManagement.findUserByUsername("hans");

		assertThat(user).isPresent();

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("password","hallo1234");
		map.add("confirmPassword","hallo1234");

		mvc.perform(get("/userDetails/").with(user("hans"))
				.params(map))
				.andExpect(view().name("userDetails"));

		mvc.perform(post("/change_password/").with(user("hans"))
				.params(map))
				.andExpect(view().name("/login"));
	}


	@Test
	void testGetProfileAsBoss() throws Exception{

		Optional<User> user = userManagement.findUserByUsername("hans");

		assertThat(user).isPresent();

		MvcResult result = mvc.perform(get("/userDetailsAsBoss/"+user.get().getId()).with(user("hansWurst").roles("BOSS")))
				.andExpect(view().name("userDetails"))
				.andReturn();

		assertThat(result.getModelAndView().getModel().get("user").toString()).hasToString(user.get().toString());
	}

	@Test
	void testDenyAuthenticationAndFurtherLogin() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("username", "klabrovsky");
		params.add("password", "123");
		mvc.perform(post("/login").params(params))
				.andExpect(redirectedUrl("/"));

		Optional<User> user = userManagement.findUserByUsername("klabrovsky");

		assertThat(user).isPresent();

		mvc.perform(post("/deleteUser")
					.param("userId", String.valueOf(user.get().getId()))
					.with(user("hansWurst").roles("BOSS")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/users"));

		userManagement.denyAuthenticationById(user.get().getId());

		mvc.perform(post("/login").params(params))
				.andExpect(redirectedUrl("/login?error"));

	}

}
