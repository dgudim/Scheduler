package prototype.xd.scheduler.views.settings;

import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.views.Switch;

public class PopupSettingsView {
    
    protected final View fontColor_view;
    protected final View bgColor_view;
    protected final View padColor_view;
    protected final View add_group;
    protected final Spinner group_spinner;
    
    protected final TextView fontColor_view_state;
    protected final TextView bgColor_view_state;
    protected final TextView padColor_view_state;
    protected final TextView adaptiveColor_switch_state;
    protected final TextView priority_state;
    protected final TextView padSize_state;
    protected final TextView show_on_lock_state;
    protected final TextView adaptiveColor_bar_state;
    protected final TextView showDaysUpcoming_bar_state;
    protected final TextView showDaysExpired_bar_state;
    
    protected final TextView bevel_thickness_description;
    protected final SeekBar bevel_thickness_bar;
    protected final TextView priority_description;
    protected final SeekBar priority_bar;
    protected final TextView adaptive_color_balance_description;
    protected final SeekBar adaptive_color_balance_bar;
    protected final TextView show_days_beforehand_description;
    protected final SeekBar show_days_beforehand_bar;
    protected final TextView show_days_after_description;
    protected final SeekBar show_days_after_bar;
    protected final Switch show_on_lock_switch;
    protected final Switch adaptive_color_switch;
    
    protected final View settings_reset_button;
    
    PopupSettingsView(View settingsView){
    
        fontColor_view = settingsView.findViewById(R.id.textColor);
        bgColor_view = settingsView.findViewById(R.id.backgroundColor);
        padColor_view = settingsView.findViewById(R.id.bevelColor);
        add_group = settingsView.findViewById(R.id.addGroup);
        group_spinner = settingsView.findViewById(R.id.groupSpinner);
    
        fontColor_view_state = settingsView.findViewById(R.id.font_color_state);
        bgColor_view_state = settingsView.findViewById(R.id.background_color_state);
        padColor_view_state = settingsView.findViewById(R.id.bevel_color_state);
        priority_state = settingsView.findViewById(R.id.priority_state);
        padSize_state = settingsView.findViewById(R.id.bevel_size_state);
        show_on_lock_state = settingsView.findViewById(R.id.show_on_lock_state);
        adaptiveColor_switch_state = settingsView.findViewById(R.id.adaptive_color_state);
        adaptiveColor_bar_state = settingsView.findViewById(R.id.adaptive_color_balance_state);
        showDaysUpcoming_bar_state = settingsView.findViewById(R.id.days_beforehand_state);
        showDaysExpired_bar_state = settingsView.findViewById(R.id.days_after_state);
    
        bevel_thickness_description = settingsView.findViewById(R.id.bevel_thickness_description);
        bevel_thickness_bar = settingsView.findViewById(R.id.bevel_thickness_bar);
        priority_description = settingsView.findViewById(R.id.priority_description);
        priority_bar = settingsView.findViewById(R.id.priority_bar);
        adaptive_color_balance_description = settingsView.findViewById(R.id.adaptive_color_balance_description);
        adaptive_color_balance_bar = settingsView.findViewById(R.id.adaptive_color_balance_bar);
        show_days_beforehand_description = settingsView.findViewById(R.id.show_days_beforehand_description);
        show_days_beforehand_bar = settingsView.findViewById(R.id.show_days_beforehand_bar);
        show_days_after_description = settingsView.findViewById(R.id.show_days_after_description);
        show_days_after_bar = settingsView.findViewById(R.id.show_days_after_bar);
        show_on_lock_switch = settingsView.findViewById(R.id.show_on_lock_switch);
        adaptive_color_switch = settingsView.findViewById(R.id.adaptive_color_switch);
        
        settings_reset_button = settingsView.findViewById(R.id.settings_reset_button);
    }
    
}
