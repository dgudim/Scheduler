package prototype.xd.scheduler.views;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.Utilities.datesEqual;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class CalendarView {
    
    static class CalendarDayViewContainer extends ViewContainer {
        
        TextView textView;
        
        final int maxIndicators = 4;
        View[] eventIndicators;
        
        public MaterialCardView cardView;
        public LocalDate date;
        
        public CalendarDayViewContainer(@NonNull View view, CalendarView container) {
            super(view);
            textView = view.findViewById(R.id.calendarDayText);
            cardView = view.findViewById(R.id.calendarDayCard);
            
            eventIndicators = new View[maxIndicators];
            eventIndicators[0] = view.findViewById(R.id.event_indicator1);
            eventIndicators[1] = view.findViewById(R.id.event_indicator2);
            eventIndicators[2] = view.findViewById(R.id.event_indicator3);
            eventIndicators[3] = view.findViewById(R.id.event_indicator4);
            
            cardView.setOnClickListener(v -> container.selectDate(date));
        }
        
        private void setEventIndicators(ArrayList<ColorStateList> eventIndicatorColors) {
            for (int i = 0; i < maxIndicators; i++){
                if(eventIndicatorColors.size() <= i) {
                    eventIndicators[i].setVisibility(View.INVISIBLE);
                    continue;
                }
                eventIndicators[i].setVisibility(View.VISIBLE);
                eventIndicators[i].setBackgroundTintList(eventIndicatorColors.get(i));
            }
        }
        
        public void bind(CalendarDay elementDay, CalendarView calendarView, TodoListEntryStorage todoListEntryStorage) {
            Context context = calendarView.rootCalendarView.getContext();
            date = elementDay.getDate();
            textView.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            
            DayPosition dayPosition = elementDay.component2();
            
            if (dayPosition == DayPosition.MonthDate) {
                if (datesEqual(date, DateManager.currentDate)) {
                    textView.setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary, Color.WHITE));
                } else {
                    textView.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.WHITE));
                }
            } else {
                textView.setTextColor(context.getColor(R.color.gray_harmonized));
            }
            
            setEventIndicators(todoListEntryStorage.getEventIndicators(
                    date.toEpochDay(),
                    dayPosition != DayPosition.MonthDate,
                    context));
            
            if (datesEqual(date, calendarView.selectedDate) && dayPosition == DayPosition.MonthDate) {
                cardView.setStrokeColor(MaterialColors.getColor(context, R.attr.colorAccent, Color.WHITE));
                cardView.setCardBackgroundColor(MaterialColors.getColor(context, R.attr.colorSurfaceVariant, Color.WHITE));
            } else {
                cardView.setStrokeColor(Color.TRANSPARENT);
                cardView.setCardBackgroundColor(Color.TRANSPARENT);
            }
        }
        
    }
    
    static class CalendarMonthViewContainer extends ViewContainer {
        
        ViewGroup weekday_titles;
        TextView month_title;
        
        public CalendarMonthViewContainer(@NonNull View view) {
            super(view);
            weekday_titles = view.findViewById(R.id.weekday_titles_container);
            month_title = view.findViewById(R.id.month_title);
        }
        
        public void bind(CalendarMonth calendarMonth, List<DayOfWeek> daysOfWeek) {
            if (weekday_titles.getTag() == null) {
                weekday_titles.setTag(calendarMonth.getYearMonth());
                for (int i = 0; i < daysOfWeek.size(); i++) {
                    ((TextView) weekday_titles.getChildAt(i)).setText(daysOfWeek.get(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()));
                }
            }
            month_title.setText(String.format(Locale.getDefault(), "%s %d",
                    calendarMonth.getYearMonth().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    calendarMonth.getYearMonth().getYear()));
        }
    }
    
    LocalDate selectedDate;
    com.kizitonwose.calendar.view.CalendarView rootCalendarView;
    DateChangeListener dateChangeListener;
    MonthChangeListener monthPreChangeListener;
    
    public CalendarView(com.kizitonwose.calendar.view.CalendarView rootCalendarView, TodoListEntryStorage todoListEntryStorage) {
        this.rootCalendarView = rootCalendarView;
        
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        List<DayOfWeek> daysOfWeek = daysOfWeek(DayOfWeek.MONDAY);
        
        rootCalendarView.setDayBinder(new MonthDayBinder<CalendarDayViewContainer>() {
            @NonNull
            @Override
            public CalendarDayViewContainer create(@NonNull View view) {
                return new CalendarDayViewContainer(view, CalendarView.this);
            }
            
            @Override
            public void bind(@NonNull CalendarDayViewContainer container, CalendarDay calendarDay) {
                container.bind(calendarDay, CalendarView.this, todoListEntryStorage);
            }
        });
        
        rootCalendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<CalendarMonthViewContainer>() {
            @NonNull
            @Override
            public CalendarMonthViewContainer create(@NonNull View view) {
                return new CalendarMonthViewContainer(view);
            }
            
            @Override
            public void bind(@NonNull CalendarMonthViewContainer container, CalendarMonth calendarMonth) {
                container.bind(calendarMonth, daysOfWeek);
                if (monthPreChangeListener != null) {
                    YearMonth yearMonth = calendarMonth.getYearMonth();
                    monthPreChangeListener.onMonthChanged(calendarMonth,
                            yearMonth.atDay(1).toEpochDay(),
                            yearMonth.atEndOfMonth().toEpochDay(),
                            rootCalendarView.getContext());
                }
            }
        });
        
        rootCalendarView.setup(startMonth, endMonth, daysOfWeek.get(0));
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
    
    public void notifyCurrentDayChanged() {
        rootCalendarView.notifyDateChanged(LocalDate.ofEpochDay(currentlySelectedDay));
    }
    
    public void setOnMonthPostChangeListener(MonthChangeListener monthPostChangeListener) {
        rootCalendarView.setMonthScrollListener(calendarMonth -> {
            monthPostChangeListener.onMonthChanged(calendarMonth,
                    Objects.requireNonNull(rootCalendarView.findFirstVisibleDay()).getDate().toEpochDay(),
                    Objects.requireNonNull(rootCalendarView.findLastVisibleDay()).getDate().toEpochDay(),
                    rootCalendarView.getContext());
            return null;
        });
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
        void onMonthChanged(CalendarMonth calendarMonth, long first_visible_day, long last_visible_day, Context context);
    }
}

