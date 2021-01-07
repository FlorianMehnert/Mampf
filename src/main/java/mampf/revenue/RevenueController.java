package mampf.revenue;

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.order.MampfOrder;
import mampf.order.MampfOrderManager;
import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.springframework.data.util.Pair;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.money.MonetaryAmount;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
public class RevenueController {
	private MampfOrderManager mampfOrderManager;
	private MampfCatalog catalog;

	public RevenueController(MampfOrderManager mampfOrderManager, MampfCatalog catalog) {
		this.mampfOrderManager = mampfOrderManager;
		this.catalog = catalog;
	}

	@GetMapping("/revenue")
	@PreAuthorize("hasRole('BOSS')")
	public String showRevenue(Model model, @RequestParam(required = false) String startDateString, @RequestParam(required = false) String endDateString) {
		LocalDate startDate;
		LocalDate endDate;
		if(startDateString == null) {
			startDate = LocalDate.now();
		}else{
			try {
				startDate = LocalDate.parse(startDateString);
			}catch (DateTimeParseException exception) {
				startDate = LocalDate.now();
			}
		}
		if(endDateString == null) {
			endDate = LocalDate.now().plusMonths(1);
		}else{
			try {
				endDate = LocalDate.parse(endDateString);
			}catch (DateTimeParseException exception) {
				endDate = LocalDate.now().plusMonths(1);
			}
		}
		List<MampfOrder> ordersPerItem = mampfOrderManager.findAll();
		Map<Product, Pair<Quantity, MonetaryAmount>> gains = new HashMap<>();
		for (MampfOrder mampfOrder :ordersPerItem) {
			LocalDate dateOfCreationTheBooking = mampfOrder.getDateCreated().toLocalDate();

			if(dateIsInBorders(dateOfCreationTheBooking, startDate, endDate)) {

				for(OrderLine orderLine: mampfOrder.getOrderLines()) {
					Optional<Item> product = catalog.findById(orderLine.getProductIdentifier());
					if(product.isPresent()) {
						if(gains.containsKey(product.get())) {
							Quantity newQuantity = gains.get(product.get()).getFirst().add(orderLine.getQuantity());
							gains.put(product.get(), Pair.of(newQuantity, gains.get(product.get()).getSecond()));
						}else {
							if(product.get().getDomain() == Item.Domain.MOBILE_BREAKFAST) {
								gains.put(product.get(), Pair.of(orderLine.getQuantity(), BreakfastItem.BREAKFAST_PRICE));
							}else{
								gains.put(product.get(), Pair.of(orderLine.getQuantity(), product.get().getPrice()));
							}
						}
					}
				}
			}
		}
		MonetaryAmount total = Money.of(0, "EUR");
		for(Pair<Quantity, MonetaryAmount> pairs: gains.values()) {
			total = total.add(pairs.getSecond().multiply(pairs.getFirst().getAmount()));
		}
		model.addAttribute("gains", gains);
		model.addAttribute("total", total);
		model.addAttribute("startDateString", startDate.toString());
		model.addAttribute("endDateString", endDate.toString());
		return "revenue";
	}

	private static boolean dateIsInBorders(LocalDate dateToCheck, LocalDate startDate, LocalDate endDate) {
		return (startDate.isBefore(dateToCheck) || startDate.isEqual(dateToCheck))
				&& (endDate.isEqual(dateToCheck) || endDate.isAfter(dateToCheck));
	}
}
