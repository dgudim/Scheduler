package prototype.xd.scheduler.views;

import static prototype.xd.scheduler.utilities.DateManager.dateStringMonthNamesFromMsUTC;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import prototype.xd.scheduler.R;

public class DateSelectButton extends MaterialButton {
    
    private Long selectedDateMsUTC;
    private MaterialDatePicker.Builder<Long> datePickerBuilder;
    
    public DateSelectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(context.getString(R.string.select_date));
        } else {
            setText(R.string.select_date);
        }
    }
    
    public void setup(@NonNull FragmentManager fragmentManager, Long initialDate) {
        selectedDateMsUTC = initialDate;
        datePickerBuilder.setSelection(initialDate);
        MaterialDatePicker<Long> datePicker = datePickerBuilder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMsUTC = selection;
            updateText();
        });
        updateText();
        setOnClickListener(v -> datePicker.show(fragmentManager, "date_picker"));
    }
    
    public String getSelectedDateMsUTCStr() {
        return String.valueOf(selectedDateMsUTC);
    }
    
    private void updateText() {
        setText(dateStringMonthNamesFromMsUTC(selectedDateMsUTC));
    }
}
