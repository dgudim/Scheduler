package prototype.xd.scheduler.utilities;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.material.datepicker.CalendarConstraints;

/**
 * Simple range date validator, will constraint selectable date between leftBoundMsUTC and rightBoundMsUTC
 */
public class RangeDateValidator implements CalendarConstraints.DateValidator {
    
    // minimum selectable date in ms UTC
    private Long leftBoundMsUTC = -1L;
    
    // maximum selectable date in ms UTC
    private Long rightBoundMsUTC = -1L;
    
    /**
     * Part of {@link android.os.Parcelable} requirements. Do not use.
     */
    public static final Creator<RangeDateValidator> CREATOR =
            new Creator<>() {
                @NonNull
                @Override
                public RangeDateValidator createFromParcel(Parcel source) {
                    return new RangeDateValidator();
                }
                
                @Override
                public RangeDateValidator[] newArray(int size) {
                    return new RangeDateValidator[size];
                }
            };
    
    public void setRightBoundMsUTC(@NonNull Long rightBoundMsUTC) {
        this.rightBoundMsUTC = rightBoundMsUTC;
    }
    
    public void setLeftBoundMsUTC(@NonNull Long leftBoundMsUTC) {
        this.leftBoundMsUTC = leftBoundMsUTC;
    }
    
    @Override
    public boolean isValid(long dateMsUTC) {
        return (dateMsUTC <= rightBoundMsUTC || rightBoundMsUTC == -1)
                && (dateMsUTC >= leftBoundMsUTC || leftBoundMsUTC == -1);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // nothing to save
    }
}
