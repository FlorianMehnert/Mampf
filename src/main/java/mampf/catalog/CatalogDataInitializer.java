package mampf.catalog;

import org.javamoney.moneta.Money;
import org.salespointframework.core.DataInitializer;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static mampf.catalog.Item.Domain.EVENTCATERING;

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
		mampfCatalog.save(new Item("Andi", Money.of(5,"USD"), EVENTCATERING, Item.Category.DECORATION,"ist doof"));
		mampfCatalog.save(new Item("Hans", Money.of(5,"USD"), EVENTCATERING, Item.Category.DECORATION,"ist doof"));
		mampfCatalog.save(new Item("Walter", Money.of(5,"USD"), EVENTCATERING, Item.Category.DECORATION,"ist doof"));
		mampfCatalog.save(new Item("Bob", Money.of(5,"USD"), EVENTCATERING, Item.Category.DECORATION,"ist doof"));
		mampfCatalog.save(new Item("Marleen", Money.of(5,"USD"), EVENTCATERING, Item.Category.DECORATION,"ist doof"));
	}
}
