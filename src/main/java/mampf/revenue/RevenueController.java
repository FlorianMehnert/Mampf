package mampf.revenue;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

@Controller
public class RevenueController {
	private Revenue revenue;

	public RevenueController(Revenue revenue) {
		this.revenue = revenue;
	}

	@GetMapping("/revenue")
	@PreAuthorize("hasRole('BOSS')")
	public String showRevenue(Model model) {
		ArrayList<Gain> gains = new ArrayList<>();
		for (Gain gain : revenue.findAll()) {
			gains.add(gain);
		}
		model.addAttribute("gains", gains);
		return "revenue";
	}
}
