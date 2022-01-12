package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BackgroundChooser.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.File;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ApplySharedPref"})
public class SettingsFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.resetBgButton).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удалить все сохраненные фоны?");

            builder.setPositiveButton("Да", (dialog, which) -> {
                new File(rootDir, defaultBackgroundName).delete();
                for (int i = 0; i < 7; i++) {
                    new File(rootDir, availableDays[i] + ".png").delete();
                }
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());

            builder.show();
        });

        ImageView todayBgColor = view.findViewById(R.id.defaultColor);
        ImageView todayBevelColor = view.findViewById(R.id.defaultBevelColor);
        ImageView yesterdayBgColor = view.findViewById(R.id.defaultYesterdayColor);
        ImageView yesterdayBevelColor = view.findViewById(R.id.defaultYesterdayBevelColor);

        ImageView globalBgColor = view.findViewById(R.id.globalColor);
        ImageView globalBevelColor = view.findViewById(R.id.globalBevelColor);

        todayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("todayBgColor", 0xFFFFFFFF)));
        yesterdayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("yesterdayBgColor", 0xFFFFCCCC)));
        todayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("todayBevelColor", 0xFF888888)));
        yesterdayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("yesterdayBevelColor", 0xFFFF8888)));

        globalBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("globalBgColor", 0xFFCCFFCC)));
        globalBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("globalBevelColor", 0xFF88FF88)));

        todayBgColor.setOnClickListener(v -> invokeColorDialogue("todayBgColor", (ImageView) v, 0xFFFFFFFF, getContext()));

        yesterdayBgColor.setOnClickListener(v -> invokeColorDialogue("yesterdayBgColor", (ImageView) v, 0xFFFFCCCC, getContext()));

        todayBevelColor.setOnClickListener(v -> invokeColorDialogue("todayBevelColor", (ImageView) v, 0xFF888888, getContext()));

        yesterdayBevelColor.setOnClickListener(v -> invokeColorDialogue("yesterdayBevelColor", (ImageView) v, 0xFFFF8888, getContext()));

        globalBgColor.setOnClickListener(v -> invokeColorDialogue("globalBgColor", (ImageView) v, 0xFFCCFFCC, getContext()));

        globalBevelColor.setOnClickListener(v -> invokeColorDialogue("globalBevelColor", (ImageView) v, 0xFF88FF88, getContext()));
        
        addSeekBarChangeListener(
                view.findViewById(R.id.textSizeDescripton),
                view.findViewById(R.id.fontSizeBar),
                "fontSize", 21, R.string.settings_font_size, this);

        addSeekBarChangeListener(
                view.findViewById(R.id.defaultBevelThickness),
                view.findViewById(R.id.defaultBevelThicknessBar),
                "defaultBevelThickness", 5, R.string.settings_default_bevel_thickness, this);

        addSeekBarChangeListener(
                view.findViewById(R.id.yesterdayBevelThickness),
                view.findViewById(R.id.yesterdaytBevelThicknessBar),
                "yesterdayBevelThickness", 5, R.string.settings_yesterday_bevel_thickness, this);

        addSeekBarChangeListener(
                view.findViewById(R.id.globalBevelThickness),
                view.findViewById(R.id.globalBevelThicknessBar),
                "globalBevelThickness", 5, R.string.settings_global_bevel_thickness, this);

        addSeekBarChangeListener(
                view.findViewById(R.id.adaptive_color_balance_description),
                view.findViewById(R.id.adaptive_color_balance_bar),
                "adaptiveColorBalance", 500, R.string.settings_adaptive_color_balance, this);

        addSwitchChangeListener(view.findViewById(R.id.yesterdayItemsLockSwitch), "yesterdayItemsLock", false, null);
        addSwitchChangeListener(view.findViewById(R.id.yesterdayItemsListSwitch), "yesterdayItemsList", false, null);
        addSwitchChangeListener(view.findViewById(R.id.completedTasksSwitch), "completedTasks", false, null);
        addSwitchChangeListener(view.findViewById(R.id.yesterdayTasksSwitch), "yesterdayTasks", true, null);
        addSwitchChangeListener(view.findViewById(R.id.yesterdayTasksLockSwitch), "yesterdayTasksLock", true, null);
        addSwitchChangeListener(view.findViewById(R.id.globalItemsSwitch), "globalTasksLock", true, null);
        addSwitchChangeListener(view.findViewById(R.id.itemWidthSwitch), "forceMaxRWidthOnLock", false, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptiveColorSwitch), "adaptiveColorEnabled", false, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptiveBackgroundSwitch), "adaptiveBackgroundEnabled", true, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptiveColorUnderlaySwitch), "adaptiveBackgroundUnderlayEnabled", false, null);
        
        view.findViewById(R.id.resetSettingsButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Сбросить настройки?");

            builder.setPositiveButton("Да", (dialog, which) -> {
                preferences.edit().clear().commit();
                SettingsFragment.this.onViewCreated(view, savedInstanceState);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());

            builder.show();
        });

    }
}