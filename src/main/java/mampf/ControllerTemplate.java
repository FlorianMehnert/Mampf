package mampf;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControllerTemplate {

	@GetMapping("/")
	public String index() {
		return "welcome";
	}
}
