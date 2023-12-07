package prototype.xd.scheduler.views;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;
import static prototype.xd.scheduler.utilities.DateManager.getEndOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getStartOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.systemLocale;
import static prototype.xd.scheduler.utilities.ImageUtilities.dimColorToBg;
import static prototype.xd.scheduler.utilities.Utilities.areDatesEqual;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarDayLayoutBinding;
import prototype.xd.scheduler.databinding.CalendarHeaderBinding;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.TodoEntryManager;

public class CalendarView {
    
    public static final String NAME = CalendarView.class.getSimpleName();
    
    static final class CalendarDayViewContainer extends ViewContainer {
        
        private static final int MAX_INDICATORS = 4;
        
        @NonNull
        private final CalendarDayLayoutBinding binding;
        private final Context context;
        private LocalDate date;
        
        private CalendarDayViewContainer(@NonNull CalendarDayLayoutBinding bnd, @NonNull CalendarView container) {
            super(bnd.getRoot());
            binding = bnd;
            context = bnd.getRoot().getContext();
            bnd.root.setOnClickListener(v -> container.selectDate(date));
        }
        
        private void setEventIndicator(@ColorInt int color, @IntRange(from = 0, to = MAX_INDICATORS) int index,
                                       boolean visiblePosition, boolean inCalendar) {
            // first index is day text itself
            View eventIndicator = binding.root.getChildAt(index + 1);
            eventIndicator.setVisibility(visiblePosition ? View.VISIBLE : View.INVISIBLE);
            
            if (!visiblePosition) {
                return;
            }
            
            if (!inCalendar) {
                // make the indicator less visible
                color = dimColorToBg(color, context);
            }
            eventIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        
        // for month dates
        private void setEventIndicatorInCalendar(@ColorInt int color, @IntRange(from = 0, to = MAX_INDICATORS) int index, boolean visiblePosition) {
            setEventIndicator(color, index, visiblePosition, true);
        }
        
        // for in and out dates
        private void setEventIndicatorOffCalendar(@ColorInt int color, @IntRange(from = 0, to = MAX_INDICATORS) int index, boolean visiblePosition) {
            setEventIndicator(color, index, visiblePosition, false);
        }
        
        public void bindTo(@NonNull CalendarDay elementDay, @Nullable LocalDate currentlySelectedDate, @NonNull TodoEntryManager todoEntryManager) {
            date = elementDay.getDate();
            binding.calendarDayText.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            
            DayPosition dayPosition = elementDay.component2();
            
            int textColor;
            
            if (dayPosition == DayPosition.MonthDate) {
                if (areDatesEqual(date, DateManager.getCurrentDate())) {
                    textColor = MaterialColors.getColor(context, R.attr.colorPrimary, Color.WHITE);
                } else {
                    textColor = MaterialColors.getColor(context, R.attr.colorOnSurface, Color.WHITE);
                }
                todoEntryManager.processEventIndicators(date.toEpochDay(), MAX_INDICATORS, this::setEventIndicatorInCalendar);
            } else {
                textColor = context.getColor(R.color.gray_harmonized);
                todoEntryManager.processEventIndicators(date.toEpochDay(), MAX_INDICATORS, this::setEventIndicatorOffCalendar);
            }
            
            binding.calendarDayText.setTextColor(textColor);
            
            // highlight current date
            if (areDatesEqual(date, currentlySelectedDate) && dayPosition == DayPosition.MonthDate) {
                binding.root.setBackgroundResource(R.drawable.round_bg_calendar_selection);
            } else {
                binding.root.setBackgroundResource(0);
            }
        }
        
    }
    
    static final class CalendarHeaderContainer extends ViewContainer {
        
        @NonNull
        private final CalendarHeaderBinding binding;
        
        private CalendarHeaderContainer(@NonNull CalendarHeaderBinding bnd) {
            super(bnd.getRoot());
            binding = bnd;
        }
        
        public void bindTo(@NonNull YearMonth yearMonth, @NonNull List<DayOfWeek> daysOfWeek) {
            if (binding.weekdayTitlesContainer.getTag() == null) {
                binding.weekdayTitlesContainer.setTag(yearMonth);
                for (int i = 0; i < daysOfWeek.size(); i++) {
                    ((TextView) binding.weekdayTitlesContainer.getChildAt(i)).setText(daysOfWeek.get(i).getDisplayName(TextStyle.SHORT, systemLocale));
                }
            }
            binding.monthTitle.setText(String.format(systemLocale, "%s %d",
                    yearMonth.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, systemLocale),
                    yearMonth.getYear()));
        }
    }
    
    public static final int DAYS_ON_ONE_PANEL = 7 * 6;
    private static final int CACHED_PANELS = 2;
    private static final int POTENTIALLY_VISIBLE_DAYS = DAYS_ON_ONE_PANEL * CACHED_PANELS;
    private static final int MAX_MONTHS = 100;
    
    private List<DayOfWeek> daysOfWeek;
    
    @Nullable
    LocalDate selectedDate;
    @Nullable
    YearMonth selectedMonth;
    
    // initial capacity is fine
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final Set<YearMonth> loadedMonths = new HashSet<>();
    
    private long firstSelectedMonthDayUTC;
    private long lastSelectedMonthDayUTC;
    
    private long firstVisibleDayUTC;
    private long lastVisibleDayUTC;
    
    private long minVisibleDayUTC;
    private long maxVisibleDayUTC;
    
    @NonNull
    final com.kizitonwose.calendar.view.CalendarView rootCalendarView;
    @Nullable
    BiConsumer<LocalDate, Context> dateChangeListener;
    @Nullable
    Consumer<YearMonth> newMonthBindListener;
    
    public CalendarView(@NonNull com.kizitonwose.calendar.view.CalendarView rootCalendarView, @NonNull TodoEntryManager todoEntryManager) {
        this.rootCalendarView = rootCalendarView;
        
        rootCalendarView.setDayBinder(new MonthDayBinder<CalendarDayViewContainer>() {
            @NonNull
            @Override
            public CalendarDayViewContainer create(@NonNull View view) {
                return new CalendarDayViewContainer(CalendarDayLayoutBinding.bind(view), CalendarView.this);
            }
            
            @Override
            public void bind(@NonNull CalendarDayViewContainer container, @NonNull CalendarDay calendarDay) {
                container.bindTo(calendarDay, selectedDate, todoEntryManager);
            }
        });
        
        rootCalendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<CalendarHeaderContainer>() {
            @NonNull
            @Override
            public CalendarHeaderContainer create(@NonNull View view) {
                return new CalendarHeaderContainer(CalendarHeaderBinding.bind(view));
            }
            
            @Override
            public void bind(@NonNull CalendarHeaderContainer container, @NonNull CalendarMonth calendarMonth) {
                YearMonth calendarYearMonth = calendarMonth.getYearMonth();
                
                container.bindTo(calendarYearMonth, daysOfWeek);
                
                // new month was loaded
                if (!loadedMonths.contains(calendarYearMonth) && newMonthBindListener != null) {
                    loadedMonths.add(calendarYearMonth);
                    Logger.debug(NAME, "New month loaded: " + calendarYearMonth);
                    newMonthBindListener.accept(calendarYearMonth);
                }
            }
        });
        
        rootCalendarView.setMonthScrollListener(calendarMonth -> {
            // update currently visible day range and selected month
            setSelectedMonth(calendarMonth.getYearMonth(), true);
            return null;
        });
        
        init(FIRST_DAY_OF_WEEK.get());
    }
    
    private void setSelectedMonth(@NonNull YearMonth month, boolean extend) {
        selectedMonth = month;
        Logger.debug(NAME, "New month selected: " + selectedMonth + (extend ? " (extension)" : ""));
        
        firstSelectedMonthDayUTC = getStartOfMonthDayUTC(selectedMonth);
        lastSelectedMonthDayUTC = getEndOfMonthDayUTC(selectedMonth);
        
        CalendarDay firstVisibleCalendarDay = rootCalendarView.findFirstVisibleDay();
        CalendarDay lastVisibleCalendarDay = rootCalendarView.findLastVisibleDay();
        
        firstVisibleDayUTC = firstVisibleCalendarDay != null ? firstVisibleCalendarDay.getDate().toEpochDay() : firstSelectedMonthDayUTC;
        lastVisibleDayUTC = lastVisibleCalendarDay != null ? lastVisibleCalendarDay.getDate().toEpochDay() : lastSelectedMonthDayUTC;
        
        if (extend) {
            minVisibleDayUTC = min(minVisibleDayUTC, firstVisibleDayUTC);
            maxVisibleDayUTC = max(maxVisibleDayUTC, lastVisibleDayUTC);
        } else {
            minVisibleDayUTC = firstVisibleDayUTC;
            maxVisibleDayUTC = lastVisibleDayUTC;
        }
    }
    
    private void init(@NonNull DayOfWeek firstDayOfWeek) {
        Logger.debug(NAME, "Calendar init!");
        
        YearMonth currentMonth = YearMonth.now();
        loadedMonths.add(currentMonth);
        
        daysOfWeek = daysOfWeek(firstDayOfWeek);
        
        rootCalendarView.setup(currentMonth.minusMonths(MAX_MONTHS), currentMonth.plusMonths(MAX_MONTHS), daysOfWeek.get(0));
        selectDate(DateManager.getCurrentDate());
        rootCalendarView.scrollToMonth(currentMonth);
        
        setSelectedMonth(currentMonth, false);
    }
    
    public long getFirstLoadedDayUTC() {
        return min(firstVisibleDayUTC - POTENTIALLY_VISIBLE_DAYS, minVisibleDayUTC);
    }
    
    public long getLastLoadedDayUTC() {
        return min(lastVisibleDayUTC + POTENTIALLY_VISIBLE_DAYS, maxVisibleDayUTC);
    }
    
    public void selectDate(@NonNull LocalDate targetDate) {
        
        rootCalendarView.scrollToDate(targetDate);
        
        // we selected another date
        if (!areDatesEqual(targetDate, selectedDate)) {
            Logger.debug(NAME, "Date selected: " + targetDate);
            if (selectedDate != null) {
                rootCalendarView.notifyDateChanged(selectedDate);
            }
            rootCalendarView.notifyDateChanged(targetDate);
            selectedDate = targetDate;
            if (dateChangeListener != null) {
                dateChangeListener.accept(targetDate, rootCalendarView.getContext());
            }
        }
    }
    
    public void setNewMonthBindListener(@Nullable Consumer<YearMonth> newMonthBindListener) {
        this.newMonthBindListener = newMonthBindListener;
    }
    
    public void setOnDateChangeListener(@NonNull BiConsumer<LocalDate, Context> dateChangeListener) {
        this.dateChangeListener = dateChangeListener;
    }
    
    public void notifyDayChanged(long targetDayUTC) {
        if (targetDayUTC < getFirstLoadedDayUTC() || targetDayUTC > getLastLoadedDayUTC()) {
            // day is not loaded, skip it
            return;
        }
        DayPosition dayPosition = DayPosition.MonthDate;
        if (targetDayUTC < firstSelectedMonthDayUTC) {
            dayPosition = DayPosition.InDate;
        } else if (targetDayUTC > lastSelectedMonthDayUTC) {
            dayPosition = DayPosition.OutDate;
        }
        LocalDate date = LocalDate.ofEpochDay(targetDayUTC);
        if (dayPosition == DayPosition.MonthDate) {
            rootCalendarView.notifyDateChanged(date, DayPosition.InDate);
            rootCalendarView.notifyDateChanged(date, DayPosition.OutDate);
        } else {
            rootCalendarView.notifyDateChanged(date, dayPosition);
        }
        rootCalendarView.notifyDateChanged(date, DayPosition.MonthDate);
    }
    
    public void notifyDaysChanged(@NonNull Set<Long> days) {
        days.forEach(this::notifyDayChanged);
    }
    
    public void notifyCurrentMonthChanged() {
        if (selectedMonth != null) {
            // internally will rebind all visible dates
            rootCalendarView.notifyMonthChanged(selectedMonth);
        }
    }
    
    public void notifyCalendarChanged() {
        DayOfWeek firstDayOfWeek = FIRST_DAY_OF_WEEK.get();
        if (firstDayOfWeek == daysOfWeek.get(0)) {
            rootCalendarView.notifyCalendarChanged();
        } else {
            init(firstDayOfWeek);
        }
    }
}

