package mampf.order;

import mampf.catalog.Item;
import mampf.order.OrderController.BreakfastMappedItems;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import java.sql.Time;
import java.time.*;
import java.time.temporal.TemporalAmount;
import java.util.*;
/**
 *  a {@link MampfOrder} class, which represents a mobile breakfast order.
 */
@Entity
public class MBOrder extends MampfOrder {

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<DayOfWeek> weekDays;


	/**
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	@Deprecated
    public MBOrder() {
    }

    public MBOrder(UserAccount account, PaymentMethod paymentMethod, LocalDateTime startDate, LocalDateTime endDate,
            BreakfastMappedItems bfItem) {
        super(account, paymentMethod, Item.Domain.MOBILE_BREAKFAST, startDate, endDate, bfItem.getAddress());
        this.weekDays = new HashSet<>(bfItem.getWeekDays());
    }
    /**
     * calculates the amount of breakfast dates for the given breakfast days.</br>
     * one breakfast Date is a {@link DayOfWeek} of weekDays.</br>
     * the amount of breakfast dates corresponds to the amount of overlapping breakfast
	 * Dates with the needed-timespan.</br>
     *
     * @param fromDate needed-timespan start
     * @param toDate needed-timespan end
     * @param startDate duration-timespan start, the start Date of the mobile breakfast order
     * @param endDate duration-timespan end, the end Date of the mobile breakfast order
     * @param weekDays a {@link List} of {@link DayOfWeek} represents weekdays
	 *                 where the user wants to have a breakfast meal
     * @return long
     */
    public static long getAmount(LocalDateTime fromDate, LocalDateTime toDate, LocalDateTime startDate,
            LocalDateTime endDate, Collection<DayOfWeek> weekDays) {

        long bfAmount = 0;
        if (!hasTimeOverlap(fromDate, toDate, startDate, endDate)) {
            return bfAmount;
        }
        // 1) calc overlay:
        LocalDateTime spanStartDate = fromDate;
        LocalDateTime spanEndDate = toDate;
        /*
         * cases: 1) left overlay 2) right overlay 3) total overlay 4) middle overlay
         */
        if (fromDate.isBefore(startDate) && toDate.isBefore(endDate)) {
            spanStartDate = startDate;
        }
        if (fromDate.isAfter(endDate) && toDate.isAfter(endDate)) {
            spanEndDate = endDate;
        }
        if (fromDate.isBefore(startDate) && toDate.isAfter(endDate)) {
            spanStartDate = startDate;
            spanEndDate = endDate;
        }

        // 2) set dates
        List<LocalDate> bfDates = new ArrayList<>();
        TemporalAmount itStep = Duration.ofDays(1);
        LocalDateTime it = spanStartDate.withHour(0).withMinute(0);
        while (it.isBefore(spanEndDate)) {
            if (weekDays.contains(it.getDayOfWeek())) {
                bfDates.add(it.toLocalDate());
            }
            it = it.plus(itStep);
        }
        
        // 3) collect colliding bfDates
        for (LocalDate bfDate : bfDates) {
            LocalDateTime bfDateTime = LocalDateTime.of(bfDate, LocalTime.of(7, 0));
            if (bfDateTime.isAfter(spanStartDate) && bfDateTime.isBefore(spanEndDate)) {
                bfAmount++;
            }
        }
        return bfAmount;
    }

    /**
     * needed Items will be calculated with the amount of needed breakfastDates.
     */
    public Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate){
    
        Map<ProductIdentifier,Quantity> res = new HashMap<>();
        Quantity mbQuantity = Quantity.of(getAmount(fromDate, toDate, getStartDate(), getEndDate(), weekDays));
        getOrderLines().forEach(oL->res.put(oL.getProductIdentifier(), mbQuantity));
        return res;
    }

    // impl.:
    public String getDescription() {
        return "Bestellung f??r Mobile Breakfast: je" + weekDays.toString();
    }

    public Set<DayOfWeek> getWeekDays() {
        return weekDays;
    }

}
