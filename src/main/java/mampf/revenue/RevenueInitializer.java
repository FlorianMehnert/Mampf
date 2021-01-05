package mampf.revenue;

import org.javamoney.moneta.Money;
import org.salespointframework.core.DataInitializer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Component
@Order(20)
public class RevenueInitializer implements DataInitializer {

	private final Revenue revenue;

	RevenueInitializer(Revenue revenue) {

		Assert.notNull(revenue, "VideoCatalog must not be null!");

		this.revenue = revenue;
	}

	@Override
	public void initialize() {

		/*Änderungen:
		 * falls das Angebot eine Anzahl an Personal (Cook/Service) anfordert ist das Item eine Personalinstanz und von
		 * der Kategorie PERSONEN
		 * falls das Angebot eine Anzahl im Inventar betrifft ist das Item von der Kategorie EQUIPMENT
		 * (für reduzierbare gegenstände wie personal eine extra klasse?)*/
		if (revenue.findAll().iterator().hasNext()) {
		}
		revenue.save(new Gain(LocalDateTime.now(), Money.of(10, "EUR")));
	}
}
