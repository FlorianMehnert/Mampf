package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.core.DataInitializer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import mampf.employee.Employee.Role;

@Component
@Order(20)
class CatalogDataInitializer implements DataInitializer {

	private final MampfCatalog mampfCatalog;

	CatalogDataInitializer(MampfCatalog mampfCatalog) {

		Assert.notNull(mampfCatalog, "VideoCatalog must not be null!");

		this.mampfCatalog = mampfCatalog;
	}

	@Override
	public void initialize() {

		if (mampfCatalog.findAll().iterator().hasNext()) {
			return;
		}
		String perPerson = "pro Person";
		String perFive = "pro 5 Personen";
		String ten = "10 Personen";
		String time = "nur vor 14 Uhr";
		mampfCatalog.save(new Item("Basic", Money.of(5, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.BUFFET, perPerson));
		mampfCatalog.save(new Item("Premium", Money.of(7, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.BUFFET, perPerson));
		mampfCatalog.save(new Item("Luxus", Money.of(8, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.BUFFET, perPerson));

		mampfCatalog.save(new Item("Basic", Money.of(12, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT, perPerson));
		mampfCatalog.save(new Item("Premium", Money.of(16, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT, perPerson));
		mampfCatalog.save(new Item("Luxus", Money.of(20, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT, perPerson));

		mampfCatalog.save(new Item("Dekoration", Money.of(10, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT, "pro 4 Personen, 10€ Ausleihgebühr und Reinigung"));
		mampfCatalog.save(new Item("Tischdecke", Money.of(5, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT, "pro 4 Personen, 5€ Ausleihgebühr"));

		mampfCatalog.save(new StaffItem("Koch/Köchin pro 10 Personen", Money.of(11.88, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.STAFF, perPerson, Role.COOK));
		mampfCatalog.save(new StaffItem("Service-Personal", Money.of(13.56, "EUR"),
				Item.Domain.EVENTCATERING, Item.Category.STAFF, perFive, Role.SERVICE));

		mampfCatalog.save(new Item("Schinkenplatte", Money.of(20, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.FOOD, perFive));
		mampfCatalog.save(new Item("Käseplatte", Money.of(12.50, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.FOOD, "für 3 Personen"));
		mampfCatalog.save(new Item("Vegetarische Platte", Money.of(12, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.FOOD, "5 Personen"));
		mampfCatalog.save(new Item("Vegane Platte", Money.of(10, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.FOOD, ten));

		mampfCatalog.save(new Item("Sushi Abend", Money.of(90, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS, ten));
		mampfCatalog.save(new Item("Tapas Abend", Money.of(70, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS, ten));
		mampfCatalog.save(new Item("Taco Abend", Money.of(50, "EUR"),
				Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS, ten));

		mampfCatalog.save(new BreakfastItem("Müsli", time,
				Money.of(0, "EUR"), BreakfastItem.Type.DISH));
		mampfCatalog.save(new BreakfastItem("Brötchen", time,
				Money.of(0, "EUR"), BreakfastItem.Type.DISH));
		mampfCatalog.save(new BreakfastItem("Kuchen", time,
				Money.of(0, "EUR"), BreakfastItem.Type.DISH));
		mampfCatalog.save(new BreakfastItem("Kaffee", time,
				Money.of(0, "EUR"), BreakfastItem.Type.BEVERAGE));
		mampfCatalog.save(new BreakfastItem("Tee", time,
				Money.of(0, "EUR"), BreakfastItem.Type.BEVERAGE));

		mampfCatalog.save(new StaffItem("Koch/Köchin pro 10 Personen",
				Money.of(11.88, "EUR"), Item.Domain.RENT_A_COOK, Item.Category.STAFF, perPerson, Role.COOK));
		mampfCatalog.save(new StaffItem("Service-Personal",
				Money.of(13.56, "EUR"), Item.Domain.RENT_A_COOK, Item.Category.STAFF, perFive, Role.SERVICE));
	}
}
