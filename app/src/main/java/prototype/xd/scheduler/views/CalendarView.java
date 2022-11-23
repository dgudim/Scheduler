package prototype.xd.scheduler.views;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;
import static prototype.xd.scheduler.utilities.Utilities.datesEqual;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarDayLayoutBinding;
import prototype.xd.scheduler.databinding.CalendarHeaderBinding;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.TodoListEntryManager;

public class CalendarView {
    
    static class CalendarDayViewContainer extends ViewContainer {
        
        private static final int MAX_INDICATORS = 4;
        
        private final CalendarDayLayoutBinding binding;
        private LocalDate date;
        
        public CalendarDayViewContainer(@NonNull CalendarDayLayoutBinding bnd, CalendarView container) {
            super(bnd.getRoot());
            binding = bnd;
            bnd.calendarDayCard.setOnClickListener(v -> container.selectDate(date));
        }
        
        private void setEventIndicators(List<ColorStateList> eventIndicatorColors) {
            for (int i = 0; i < MAX_INDICATORS; i++) {
                // first index is day text itself
                View eventIndicator = binding.eventIndicatorContainer.getChildAt(i + 1);
                if (eventIndicatorColors.size() <= i) {
                    eventIndicator.setVisibility(View.INVISIBLE);
                    continue;
                }
                eventIndicator.setVisibility(View.VISIBLE);
                eventIndicator.setBackgroundTintList(eventIndicatorColors.get(i));
            }
        }
        
        public void bind(CalendarDay elementDay, CalendarView calendarView, TodoListEntryManager todoListEntryManager) {
            Context context = calendarView.rootCalendarView.getContext();
            date = elementDay.getDate();
            binding.calendarDayText.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            
            DayPosition dayPosition = elementDay.component2();
            
            if (dayPosition == DayPosition.MonthDate) {
                if (datesEqual(date, DateManager.currentDate)) {
                    binding.calendarDayText.setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary, Color.WHITE));
                } else {
                    binding.calendarDayText.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.WHITE));
                }
            } else {
                binding.calendarDayText.setTextColor(context.getColor(R.color.gray_harmonized));
            }
            
            setEventIndicators(todoListEntryManager.getEventIndicators(
                    date.toEpochDay(),
                    dayPosition != DayPosition.MonthDate,
                    context));
            
            if (datesEqual(date, calendarView.selectedDate) && dayPosition == DayPosition.MonthDate) {
                binding.calendarDayCard.setStrokeColor(MaterialColors.getColor(context, R.attr.colorAccent, Color.WHITE));
                binding.calendarDayCard.setCardBackgroundColor(MaterialColors.getColor(context, R.attr.colorSurfaceVariant, Color.WHITE));
            } else {
                binding.calendarDayCard.setStrokeColor(Color.TRANSPARENT);
                binding.calendarDayCard.setCardBackgroundColor(Color.TRANSPARENT);
            }
        }
        
    }
    
    static class CalendarHeaderContainer extends ViewContainer {
        
        private final CalendarHeaderBinding binding;
        
        public CalendarHeaderContainer(@NonNull CalendarHeaderBinding bnd) {
            super(bnd.getRoot());
            binding = bnd;
        }
        
        public void bind(CalendarMonth calendarMonth, List<DayOfWeek> daysOfWeek) {
            if (binding.weekdayTitlesContainer.getTag() == null) {
                binding.weekdayTitlesContainer.setTag(calendarMonth.getYearMonth());
                for (int i = 0; i < daysOfWeek.size(); i++) {
                    ((TextView) binding.weekdayTitlesContainer.getChildAt(i)).setText(daysOfWeek.get(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()));
                }
            }
            binding.monthTitle.setText(String.format(Locale.getDefault(), "%s %d",
                    calendarMonth.getYearMonth().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    calendarMonth.getYearMonth().getYear()));
        }
    }
    
    @Nullable
    LocalDate selectedDate;
    @Nullable
    YearMonth selectedMonth;
    
    long firstMonthDay;
    long lastMonthDay;
    
    long firstVisibleDay;
    long lastVisibleDay;
    
    com.kizitonwose.calendar.view.CalendarView rootCalendarView;
    @Nullable
    DateChangeListener dateChangeListener;
    @Nullable
    MonthChangeListener monthPreChangeListener;
    
    public CalendarView(com.kizitonwose.calendar.view.CalendarView rootCalendarView, TodoListEntryManager todoListEntryManager) {
        this.rootCalendarView = rootCalendarView;
        
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        List<DayOfWeek> daysOfWeek = daysOfWeek(DayOfWeek.MONDAY);
        
        rootCalendarView.setDayBinder(new MonthDayBinder<CalendarDayViewContainer>() {
            @NonNull
            @Override
            public CalendarDayViewContainer create(@NonNull View view) {
                return new CalendarDayViewContainer(CalendarDayLayoutBinding.bind(view), CalendarView.this);
            }
            
            @Override
            public void bind(@NonNull CalendarDayViewContainer container, CalendarDay calendarDay) {
                container.bind(calendarDay, CalendarView.this, todoListEntryManager);
            }
        });
        
        rootCalendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<CalendarHeaderContainer>() {
            @NonNull
            @Override
            public CalendarHeaderContainer create(@NonNull View view) {
                return new CalendarHeaderContainer(CalendarHeaderBinding.bind(view));
            }
            
            @Override
            public void bind(@NonNull CalendarHeaderContainer container, CalendarMonth calendarMonth) {
                container.bind(calendarMonth, daysOfWeek);
                YearMonth prevMonth = selectedMonth;
                selectedMonth = calendarMonth.getYearMonth();
                
                firstMonthDay = selectedMonth.atDay(1).toEpochDay();
                lastMonthDay = selectedMonth.atEndOfMonth().toEpochDay();
                
                CalendarDay firstVisibleCalendarDay = rootCalendarView.findFirstVisibleDay();
                CalendarDay lastVisibleCalendarDay = rootCalendarView.findLastVisibleDay();

                firstVisibleDay = firstVisibleCalendarDay != null ? firstVisibleCalendarDay.getDate().toEpochDay() : firstMonthDay;
                lastVisibleDay = lastVisibleCalendarDay != null ? lastVisibleCalendarDay.getDate().toEpochDay() : lastMonthDay;
                
                if (monthPreChangeListener != null) {
                    monthPreChangeListener.onMonthChanged(
                            prevMonth,
                            firstMonthDay,
                            lastMonthDay,
                            rootCalendarView.getContext());
                }
            }
        });
        
        rootCalendarView.setup(startMonth, endMonth, daysOfWeek.get(0));
        selectDate(DateManager.currentDate);
        rootCalendarView.scrollToMonth(currentMonth);
    }
    
    public void selectDate(LocalDate targetDate) {
        LocalDate prevSelection = selectedDate;
        
        rootCalendarView.scrollToDate(targetDate);
        
        // we selected another date
        if (!datesEqual(targetDate, prevSelection)) {
            if (prevSelection != null) {
                rootCalendarView.notifyDateChanged(prevSelection);
            }
            rootCalendarView.notifyDateChanged(targetDate);
            selectedDate = targetDate;
            if (dateChangeListener != null) {
                dateChangeListener.onDateChanged(selectedDate, rootCalendarView.getContext());
            }
        }
    }
    
    public void selectDay(long day) {
        selectDate(LocalDate.ofEpochDay(day));
    }
    
    public void setOnDateChangeListener(DateChangeListener dateChangeListener) {
        this.dateChangeListener = dateChangeListener;
    }
    
    public long getFirstVisibleDay() {
        return firstVisibleDay;
    }
    
    public long getLastVisibleDay() {
        return lastVisibleDay;
    }
    
    public void notifyDayChanged(long day) {
        DayPosition dayPosition = DayPosition.MonthDate;
        if (day < firstMonthDay) {
            dayPosition = DayPosition.InDate;
        } else if (day > lastMonthDay) {
            dayPosition = DayPosition.OutDate;
        }
        rootCalendarView.notifyDateChanged(LocalDate.ofEpochDay(day), dayPosition);
    }
    
    public void notifyDaysChanged(Set<Long> days) {
        days.forEach(this::notifyDayChanged);
    }
    
    public void notifyVisibleDaysChanged() {
        if (selectedMonth != null) {
            // internally will rebind all visible dates
            rootCalendarView.notifyMonthChanged(selectedMonth);
        }
    }
    
    public void setOnMonthPreChangeListener(MonthChangeListener monthPreChangeListener) {
        this.monthPreChangeListener = monthPreChangeListener;
    }
    
    @FunctionalInterface
    public interface DateChangeListener {
        void onDateChanged(LocalDate selectedDate, Context context);
    }
    
    @FunctionalInterface
    public interface MonthChangeListener {
        void onMonthChanged(@Nullable YearMonth prevMonth, long firstVisibleDay, long lastVisibleDay, Context context);
    }
}

