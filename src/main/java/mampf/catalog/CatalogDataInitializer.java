package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.core.DataInitializer;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import mampf.catalog.StaffItem.Type;

@Component
@Order(20)
class CatalogDataInitializer implements DataInitializer {

	//! Logger is not used so should be used or deleted
	// private static final Logger LOG = LoggerFactory.getLogger(CatalogDataInitializer.class);

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
		mampfCatalog.save(new Item("Basic", Money.of(5,"EUR"), Item.Domain.EVENTCATERING, Item.Category.BUFFET,"pro Person"));
		mampfCatalog.save(new Item("Premium", Money.of(7,"EUR"), Item.Domain.EVENTCATERING, Item.Category.BUFFET,"pro Person"));
		mampfCatalog.save(new Item("Luxus", Money.of(8,"EUR"), Item.Domain.EVENTCATERING, Item.Category.BUFFET,"pro Person"));

		mampfCatalog.save(new Item("Basic", Money.of(12,"EUR"), Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT,"pro Person"));
		mampfCatalog.save(new Item("Premium", Money.of(16,"EUR"), Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT,"pro Person"));
		mampfCatalog.save(new Item("Luxus", Money.of(20,"EUR"), Item.Domain.EVENTCATERING, Item.Category.DINNER_EVENT,"pro Person"));

		mampfCatalog.save(new Item("Dekoration", Money.of(10,"EUR"), Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT,"pro 4 Personen, 10€ Ausleihgebühr und Reinigung"));
		mampfCatalog.save(new Item("Tischdecke", Money.of(5,"EUR"), Item.Domain.EVENTCATERING, Item.Category.EQUIPMENT,"pro 4 Personen, 5€ Ausleihgebür"));

		mampfCatalog.save(new StaffItem("Koch/-öchin pro 10 Personen", Money.of(11.88,"EUR"), Item.Domain.EVENTCATERING, Item.Category.STAFF,"pro Person", Type.COOK));
		mampfCatalog.save(new StaffItem("Service-Personal", Money.of(13.56,"EUR"), Item.Domain.EVENTCATERING, Item.Category.STAFF,"pro 5 Personen", Type.SERVICE));

		mampfCatalog.save(new Item("Schinkenplatte", Money.of(20,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.FOOD,"pro 5 Personen"));
		mampfCatalog.save(new Item("Käseplatte", Money.of(12.50,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.FOOD,"für 3 Personen"));
		mampfCatalog.save(new Item("Vegetarische Platte", Money.of(12,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.FOOD,"5 Personen"));
		mampfCatalog.save(new Item("Vegane Platte", Money.of(10,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.FOOD,"10 Personen"));

		mampfCatalog.save(new Item("Sushiabend", Money.of(90,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS,"10 Personen"));
		mampfCatalog.save(new Item("Tappasabend", Money.of(70,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS,"10 Personen"));
		mampfCatalog.save(new Item("Taccoabend", Money.of(50,"EUR"), Item.Domain.PARTYSERVICE, Item.Category.SPECIAL_OFFERS,"10 Personen"));

		mampfCatalog.save(new Item("Müsli/Brötchen plus Kaffee/Kuchen", Money.of(4.99,"EUR"), Item.Domain.MOBILE_BREAKFAST, Item.Category.FOOD,"nur vor 14 Uhr"));

		mampfCatalog.save(new StaffItem("Koch/-öchin pro 10 Personen", Money.of(11.88,"EUR"), Item.Domain.RENT_A_COOK, Item.Category.STAFF,"pro Person", Type.COOK));
		mampfCatalog.save(new StaffItem("Service-Personal", Money.of(13.56,"EUR"), Item.Domain.RENT_A_COOK, Item.Category.STAFF,"pro 5 Personen", Type.SERVICE));
	}
}
