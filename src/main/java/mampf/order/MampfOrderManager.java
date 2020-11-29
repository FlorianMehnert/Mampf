package mampf.order;

import mampf.employee.Employee;
import mampf.user.User;

import org.salespointframework.order.OrderManagement;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.useraccount.UserAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.salespointframework.order.Order;
import org.salespointframework.order.OrderIdentifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

@Component
public class MampfOrderManager {

	private OrderManagement<MampfOrder> orderManagement;

	public MampfOrderManager(OrderManagement<MampfOrder> orderManagement) {
		this.orderManagement = orderManagement;
	}

	public Order save(MampfOrder order) {
		return orderManagement.save(order);
	}

	public boolean payOrder(MampfOrder order) {
		return orderManagement.payOrder(order);
	}

	public void completeOrder(MampfOrder order) {
		orderManagement.completeOrder(order);
	}

	// public void addEmployee(MampfOrder order, Employee employee) {
	// 	order.addEmployee(employee);
	// 	// if (order.isDone())
	// 	// 	orderManagement.completeOrder(order);

	// }

	public MampfOrder findOrderById(UserAccount user){
		return this.orderManagement.findBy(user).get().collect(Collectors.toList()).get(0);
	}

	public OrderManagement<MampfOrder> getOrderManagement() {
		return orderManagement;
	}

	public ArrayList<MampfOrder> findAll() {
		Stream<MampfOrder> stream = orderManagement.findAll(Pageable.unpaged()).get();
		List<MampfOrder> list = stream.collect(Collectors.toList());
		return new ArrayList<MampfOrder>(list);
	}

		// // new
	// public List<MampfOrder> findNewest(UserAccount account) {
	// 	List<MampfOrder> res = new ArrayList<>();
	// 	for (MampfOrder order : orderManagement.findBy(account))
	// 		if (order.isOpen())
	// 			res.add(order);
	// 	return res;
	// }

	// public List<MampfOrder> findByUserAcc(UserAccount account) {
	// 	List<MampfOrder> res = new ArrayList<>();
	// 	for (MampfOrder order : orderManagement.findBy(account))
	// 		res.add(order);
	// 	return res;
	// }

	// public List<MampfOrder> findByEmployee(Employee employee) {
	// 	List<MampfOrder> res = new ArrayList<>();
	// 	for (MampfOrder order : orderManagement.findBy(OrderStatus.PAID))
	// 		if (!order.isDone())
	// 			res.add(order);
	// 	return res;
	// }
}
