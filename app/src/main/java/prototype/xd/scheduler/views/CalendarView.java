package prototype.xd.scheduler.views;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;

import android.content.Context;
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
import java.util.List;
import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.DateManager;

public class CalendarView {
    
    static class CalendarDayViewContainer extends ViewContainer {
        
        TextView textView;
        public MaterialCardView cardView;
        public LocalDate date;
        
        public CalendarDayViewContainer(@NonNull View view, CalendarView container) {
            super(view);
            textView = view.findViewById(R.id.calendarDayText);
            cardView = view.findViewById(R.id.calendarDayCard);
            cardView.setOnClickListener(v -> container.selectDate(date));
        }
        
        public void bind(CalendarDay elementDay, CalendarView calendarView) {
            Context context = calendarView.rootCalendarView.getContext();
            date = elementDay.getDate();
            textView.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            if (elementDay.component2() == DayPosition.MonthDate) {
                if (date.isEqual(DateManager.currentDate)) {
                    textView.setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary, Color.WHITE));
                } else {
                    textView.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.WHITE));
                }
            } else {
                textView.setTextColor(context.getColor(R.color.gray_harmonized));
            }
            
            if (date.isEqual(calendarView.selectedDate)) {
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
    DateChangedListener dateChangedListener;
    
    public CalendarView(com.kizitonwose.calendar.view.CalendarView rootCalendarView) {
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
                container.bind(calendarDay, CalendarView.this);
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
            }
        });
        
        rootCalendarView.setup(startMonth, endMonth, daysOfWeek.get(0));
        rootCalendarView.scrollToMonth(currentMonth);
    }
    
    public void selectDate(LocalDate targetDate) {
        LocalDate prevSelection = selectedDate;
        
        // we selected another date
        if (prevSelection == null || !targetDate.isEqual(prevSelection)) {
            rootCalendarView.scrollToDate(targetDate);
            if (prevSelection != null) {
                rootCalendarView.notifyDateChanged(prevSelection);
            }
            rootCalendarView.notifyDateChanged(targetDate);
            selectedDate = targetDate;
            if (dateChangedListener != null) {
                dateChangedListener.onDateChanged(selectedDate, rootCalendarView.getContext());
            }
        }
    }
    
    public void selectDay(long day) {
        selectDate(LocalDate.ofEpochDay(day));
    }
    
    public void setOnDateChangeListener(DateChangedListener dateChangedListener) {
        this.dateChangedListener = dateChangedListener;
    }
    
    @FunctionalInterface
    public interface DateChangedListener {
        void onDateChanged(LocalDate selectedDate, Context context);
    }
}

