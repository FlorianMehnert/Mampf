package mampf.order;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.Order;
import org.salespointframework.order.OrderLine;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import mampf.catalog.Item;
import mampf.employee.Employee;
import mampf.inventory.UniqueMampfItem;
import mampf.order.OrderController.BreakfastMappedItems;

@Entity
public class MBOrder extends MampfOrder{
	
	@ElementCollection(fetch = FetchType.EAGER)
	private List<DayOfWeek> weekDays;
	private LocalTime time;
	private LocalDateTime endDate;
	
	@SuppressWarnings("unused")
	public MBOrder() {}
	public MBOrder(UserAccount account,
 				   PaymentMethod paymentMethod,
				   BreakfastMappedItems bfItem,
				   String adress) {
		super(account, paymentMethod,Item.Domain.MOBILE_BREAKFAST,bfItem.getStartDate(),adress);
		this.endDate = bfItem.getEndDate();
		this.time = bfItem.getBreakfastTime();
		this.weekDays = bfItem.getWeekDays();
	}
	
	public static Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, 
														   LocalDateTime toDate,
														   LocalDateTime startDate, 
														   LocalDateTime endDate,
														   List<DayOfWeek> weekDays,
														   LocalTime time,
														   List<ProductIdentifier> orderLinesProductIds){
		Map<ProductIdentifier,Quantity> res = new HashMap<>();
		
		if(hasTimeOverlap(fromDate, toDate,startDate,endDate)) {
			//1) calc overlay:
			LocalDateTime spanStartDate = fromDate, spanEndDate = toDate;
			//TODO: find better solution:
			/* cases:
			 * 1) left overlay
			 * 2) right overlay
			 * 3) total overlay
			 * 4) middle overlay
			 */
			// 1)
			if(fromDate.isBefore(startDate)&&toDate.isBefore(endDate)) {
				spanStartDate = startDate;
			}
			// 2)
			if(fromDate.isAfter(endDate)&&toDate.isAfter(endDate)) {
				spanEndDate = endDate;
			}
			// 3)
			if(fromDate.isBefore(startDate)&&toDate.isAfter(endDate)) {
				spanStartDate = startDate;
				spanEndDate = endDate;
			}
			// 4) default
			
			//2) set dates
			List<LocalDate> bfDates = new ArrayList<>(); 
			TemporalAmount itStep = Duration.ofDays(1);
			LocalDateTime it = spanStartDate.withHour(0).withMinute(0);
			while(it.isBefore(spanEndDate)) {
				if(weekDays.contains(it.getDayOfWeek())) {
					bfDates.add(it.toLocalDate());
				}
				it = it.plus(itStep);
			}
			
			//TODO: optimize loops!
			
			//3) collect colliding bfDates
			int bfAmount = 0;
			for(LocalDate bfDate:bfDates) {
				LocalDateTime bfDateTime = LocalDateTime.of(bfDate, time);
				if(bfDateTime.isAfter(spanStartDate)&&bfDateTime.isBefore(spanEndDate)) {
					bfAmount++;
				}
			}
			
			
			//4) build Map and return
			Quantity bfQuantity = Quantity.of(bfAmount);
			for(ProductIdentifier bfProductId: orderLinesProductIds) {
				res.put(bfProductId,bfQuantity);
			}
		}
		
		return res;
	}
	
	//impl.:
	Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate){
		return getItems(fromDate,
						toDate,
						getStartDate(),
						getEndDate(),
						weekDays,time,
						getOrderLines().stream().
							map(oL->oL.getProductIdentifier()).
							collect(Collectors.toList()));
		
	}
	
	//impl.:
	LocalDateTime getEndDate() {
		return endDate;
	}
	
	
	LocalTime getTime() {
		return time;
	}
	List<DayOfWeek> getWeekDays() {
		return weekDays;
	}
	
}
