package prototype.xd.scheduler.utilities;

import android.os.Parcel;

import com.google.android.material.datepicker.CalendarConstraints;

public class RangeDateValidator implements CalendarConstraints.DateValidator {
    
    private Long leftBoundMsUTC = -1L;
    
    private Long rightBoundMsUTC = -1L;
    
    /**
     * Part of {@link android.os.Parcelable} requirements. Do not use.
     */
    public static final Creator<RangeDateValidator> CREATOR =
            new Creator<RangeDateValidator>() {
                @Override
                public RangeDateValidator createFromParcel(Parcel source) {
                    return new RangeDateValidator();
                }
                
                @Override
                public RangeDateValidator[] newArray(int size) {
                    return new RangeDateValidator[size];
                }
            };
    
    public RangeDateValidator() {
        //
    }
    
    public void setRightBoundMsUTC(Long rightBoundMsUTC) {
        this.rightBoundMsUTC = rightBoundMsUTC;
    }
    
    public void setLeftBoundMsUTC(Long leftBoundMsUTC) {
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
    public void writeToParcel(Parcel dest, int flags) {
        // nothing to save
    }
}
