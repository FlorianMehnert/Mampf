package mampf.catalog;

import javax.persistence.Entity;

import org.javamoney.moneta.Money;
import mampf.employee.Employee.Role;

@Entity
public class StaffItem extends Item {

	private Role type;

	@SuppressWarnings("unused")
	private StaffItem() {
	}

	public StaffItem(
			String name,
			Money price,
			Domain domain,
			Category category,
			String description,
			Role type
	) {
		super(name, price, domain, category, description);

		this.type = type;
	}

	public Role getType() {
		return type;
	}
}
