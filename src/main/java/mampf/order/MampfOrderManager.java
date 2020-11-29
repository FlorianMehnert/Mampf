package mampf.order;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.useraccount.UserAccount;

import java.util.ArrayList;
import java.util.List;

import org.salespointframework.order.Order;

import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager{
	
	private OrderManagement<MampfOrder> oM;
	
	public MampfOrderManager(OrderManagement<MampfOrder> oM) {
		this.oM = oM;
	}
	
	
	public Order save(MampfOrder order) {
		return oM.save(order);
	}
	public boolean payOrder(MampfOrder order) {
		return oM.payOrder(order);
	}
	
	//new
	public List<MampfOrder> findNewest(UserAccount account){
		List<MampfOrder> res = new ArrayList<>();
		for(MampfOrder order: oM.findBy(account))if(order.isOpen())res.add(order);
		return res;
	}
	
	public List<MampfOrder> findByUserAcc(UserAccount account){
		List<MampfOrder> res = new ArrayList<>();
		for(MampfOrder order: oM.findBy(account))res.add(order);
		return res;
	}
	
	
	public OrderManagement<MampfOrder> getOM() {return oM;}
}
