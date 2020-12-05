package mampf;

import org.salespointframework.EnableSalespoint;
import org.salespointframework.SalespointSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableSalespoint
public class Mampf {

	private static final String LOGIN_ROUTE = "/login";

	@Configuration
	static class MampfWebConfiguration implements WebMvcConfigurer {

		/**
		 * Anstatt einen extra Controller zu implementieren wird die Route {@code /login}
		 * direkt auf das Template {@code login} verwiesen,
		 * was ein Spring eigenes Template plus Controller ist.
		 * 
		 * @see org.springframework.web.servlet.config.annotation
		 * .WebMvcConfigurerAdapter#addViewControllers(org.springframework
		 * .web.servlet.config.annotation.ViewControllerRegistry)
		 */
		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController(LOGIN_ROUTE).setViewName("login");
			registry.addViewController("/").setViewName("index");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Mampf.class, args);
	}

	@Configuration
	static class WebSecurityConfiguration extends SalespointSecurityConfiguration {

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.csrf().disable();  // for lab purposes, that's ok!

			http.authorizeRequests().antMatchers("/**").permitAll().and()
					.formLogin().loginPage(LOGIN_ROUTE).loginProcessingUrl(LOGIN_ROUTE).and()
					.logout().logoutUrl("/logout").logoutSuccessUrl("/");
		}
	}
}
