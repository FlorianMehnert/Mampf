package mampf.revenue;

import mampf.catalog.BreakfastItem;
import mampf.catalog.Item;
import mampf.catalog.MampfCatalog;
import mampf.order.EventOrder;
import mampf.order.MampfOrder;
import mampf.order.MampfOrderManager;
import org.javamoney.moneta.Money;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.springframework.data.util.Pair;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class RevenueController {
	private final MampfOrderManager mampfOrderManager;
	private final MampfCatalog catalog;

	public RevenueController(MampfOrderManager mampfOrderManager, MampfCatalog catalog) {
		this.mampfOrderManager = mampfOrderManager;
		this.catalog = catalog;
	}


	/**
	 * Calculates the sum of each Product that got sold in the given time and hands over a list with all Products
	 * and their corresponding Quantity + Monetary Amount. The Start- and EndDateString are also transmitted.
	 * @param model used to transmit
	 * @param startDateString StartDate for the filter function
	 * @param endDateString EndDate for the filter function
	 * @return /revenue
	 */

	@GetMapping("/revenue")
	@PreAuthorize("hasRole('BOSS')")
	public String showRevenue(Model model,
							  @RequestParam(required = false) String startDateString,
							  @RequestParam(required = false) String endDateString) {

		LocalDate startDate = parseDateStringIntoLocalDate(startDateString, true);
		LocalDate endDate = parseDateStringIntoLocalDate(endDateString, false);

		List<MampfOrder> ordersPerItem = mampfOrderManager.findAll();

		Map<Item, Pair<Quantity, MonetaryAmount>> gains = new HashMap<>();

		/*
		 * we calculate the sum of each Product that we sold in the given time
		 */
		for (MampfOrder mampfOrder : ordersPerItem) {
			LocalDate dateOfCreationTheBooking = mampfOrder.getDateCreated().toLocalDate();
			if(dateIsInBorders(dateOfCreationTheBooking, startDate, endDate)) {
				int duration = 1;
				if(mampfOrder instanceof EventOrder) {
					duration = ((EventOrder) mampfOrder).durationOfEvent();
				}
				for(OrderLine orderLine: mampfOrder.getOrderLines()) {
					calculateSumPerOrderLine(orderLine, gains, duration);
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

	/**
	 * used in showRevenue to calculate a list of products and their corresponding Quantity + MonetaryAmounts
	 * @param orderLine required to extract orderLine information from
	 * @param gains puts extracted information in gains
	 */

	private void calculateSumPerOrderLine(OrderLine orderLine,
										  Map<Item, Pair<Quantity, MonetaryAmount>> gains, int duration) {
		Optional<Item> product = catalog.findById(orderLine.getProductIdentifier());
		if(product.isPresent()) {
			if(gains.containsKey(product.get())) {
				Quantity newQuantity = gains.get(product.get()).getFirst().add(calculateNewQuantity(product.get(),
						orderLine, duration));
				gains.put(product.get(), Pair.of(newQuantity, gains.get(product.get()).getSecond()));
			}else {
				if(product.get().getDomain() == Item.Domain.MOBILE_BREAKFAST) {
					gains.put(product.get(),
							Pair.of(orderLine.getQuantity(), BreakfastItem.BREAKFAST_PRICE.divide(2)));
				}else{
					gains.put(product.get(), Pair.of(calculateNewQuantity(product.get(), orderLine, duration),
							product.get().getPrice()));
				}
			}
		}
	}

	private Quantity calculateNewQuantity(Item product, OrderLine orderLine, int duration) {
		if(product.getCategory() == Item.Category.STAFF) {
			Quantity quantityOfCurrentOrderLine = orderLine.getQuantity();
			return Quantity.of(quantityOfCurrentOrderLine.getAmount().
					multiply(BigDecimal.valueOf(duration)).longValue());
		}else{
			return orderLine.getQuantity();
		}

	}

	/**
	 * checks weather a given LocalDate {@param dateToCheck} is between {@param startDate} and {@param endDate}
	 * @param dateToCheck date which should be checked for
	 * @param startDate the date which lies furthest in the past
	 * @param endDate the date which lies furthest in the future
	 * @return true when {@param dateToCheck} is between {@param startDate} and {@param endDate}
	 */

	private static boolean dateIsInBorders(LocalDate dateToCheck, LocalDate startDate, LocalDate endDate) {
		return (startDate.isBefore(dateToCheck) || startDate.isEqual(dateToCheck))
				&& (endDate.isEqual(dateToCheck) || endDate.isAfter(dateToCheck));
	}

	/**
	 * takes String and converts it to a LocalDate
	 * @param dateString input String
	 * @param isStartDate determines weather the output for a wrong dateString parsing is the momentary time or
	 *                    now plus one month
	 * @return parsed LocalDate
	 */

	private LocalDate parseDateStringIntoLocalDate(String dateString, boolean isStartDate) {
		if(dateString == null) {
			if(isStartDate) {
				return LocalDate.now();
			}
			return LocalDate.now().plusMonths(1);
		}else{
			try {
				return LocalDate.parse(dateString);
			}catch (DateTimeParseException exception) {
				if(isStartDate) {
					return LocalDate.now();
				}
				return LocalDate.now().plusMonths(1);
			}
		}
	}
}
