package mampf.order;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.FetchMode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.payment.PaymentMethod;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import mampf.catalog.Item;
import mampf.order.OrderController.BreakfastMappedItems;

@Entity
public class MBOrder extends MampfOrder {

    @ElementCollection(fetch = FetchType.EAGER)
    // @LazyCollection(LazyCollectionOption.FALSE)
    private Set<DayOfWeek> weekDays;
    private LocalTime time;

    @SuppressWarnings("unused")
    public MBOrder() {
    }

    public MBOrder(UserAccount account, PaymentMethod paymentMethod, LocalDateTime startDate, LocalDateTime endDate,
            BreakfastMappedItems bfItem) {
        super(account, paymentMethod, Item.Domain.MOBILE_BREAKFAST, startDate, endDate, bfItem.getAdress());
        this.time = bfItem.getBreakfastTime();
        this.weekDays = bfItem.getWeekDays().stream().collect(Collectors.toSet());
    }

    public static long getAmount(LocalDateTime fromDate, LocalDateTime toDate, LocalDateTime startDate,
            LocalDateTime endDate, Collection<DayOfWeek> weekDays, LocalTime time) {

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
            LocalDateTime bfDateTime = LocalDateTime.of(bfDate, time);
            if (bfDateTime.isAfter(spanStartDate) && bfDateTime.isBefore(spanEndDate)) {
                bfAmount++;
            }
        }
        return bfAmount;
    }

    // impl.:
    Map<ProductIdentifier,Quantity> getItems(LocalDateTime fromDate, LocalDateTime toDate){
    
        Map<ProductIdentifier,Quantity> res = new HashMap<>();
        Quantity mBquantiy = Quantity.of(getAmount(fromDate, toDate, getStartDate(), getEndDate(), weekDays, time));
        getOrderLines().forEach(oL->res.put(oL.getProductIdentifier(), mBquantiy));
        return res;
    }

    // impl.:
    public String getDescription() {
        return "Bestellung für Mobile Breakfast: je" + weekDays.toString() + " gegen " + time.toString() + " Uhr";
    }

    LocalTime getTime() {
        return time;
    }

    Set<DayOfWeek> getWeekDays() {
        return weekDays;
    }

}
