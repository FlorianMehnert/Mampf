package mampf.order;

import org.salespointframework.order.OrderManagement;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager{
	
	private OrderManagement<MampfOrder> oM;
	public MampfOrderManager(OrderManagement<MampfOrder> oM) {
		this.oM = oM;
	}
}
