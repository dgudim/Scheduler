package prototype.xd.scheduler.views;

import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.dateStringMonthNamesUTCFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.daysToMs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.misc.RangeDateValidator;

public class DateSelectButton extends MaterialButton {
    
    private Long selectedDayUTC;
    private Long selectedMsUTC;
    
    private MaterialDatePicker.Builder<Long> datePickerBuilder;
    
    private DateSelectButton pairButton;
    private RangeDateValidator dateValidator;
    
    public enum Role {
        START_DAY, END_DAY, NEUTRAL
    }
    
    private Role role = Role.NEUTRAL;
    
    public DateSelectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (isInEditMode()) {
            setText(R.string.select_date);
        } else {
            dateValidator = new RangeDateValidator();
            datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(context.getString(R.string.select_date))
                    .setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(dateValidator).build());
        }
    }
    
    public void setup(@NonNull FragmentManager fragmentManager, long initialDay) {
        if(initialDay == -1) {
            initialDay = currentlySelectedDayUTC;
        }
        long initialMsUTC = daysToMs(initialDay);
        selectedDayUTC = initialDay;
        selectedMsUTC = initialMsUTC;
    
        datePickerBuilder.setSelection(initialMsUTC);
    
        MaterialDatePicker<Long> datePicker = datePickerBuilder.build();
        datePicker.addOnPositiveButtonClickListener(msUTCSelection -> {
            selectedMsUTC = msUTCSelection;
            selectedDayUTC = msToDays(msUTCSelection);
            updateText();
        });
        
        updateText();
        setOnClickListener(v -> {
    
            switch (role) {
                case START_DAY:
                    dateValidator.setRightBoundMsUTC(pairButton.selectedMsUTC);
                    break;
                case END_DAY:
                    dateValidator.setLeftBoundMsUTC(pairButton.selectedMsUTC);
                    break;
                case NEUTRAL:
                default:
            }
            
            datePicker.show(fragmentManager, "date_picker");
        });
    }
    
    public void setRole(@NonNull Role role, @NonNull DateSelectButton pairButton) {
        this.role = role;
        this.pairButton = pairButton;
    }
    
    @NonNull
    public String getSelectedDayUTCStr() {
        return String.valueOf(selectedDayUTC);
    }
    
    private void updateText() {
        setText(dateStringMonthNamesUTCFromMsUTC(selectedMsUTC));
    }
}
