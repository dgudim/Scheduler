package prototype.xd.scheduler;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroCustomLayoutFragment;
import com.github.appintro.AppIntroPageTransformerType;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

import prototype.xd.scheduler.fragments.IntroStartingFragment;
import prototype.xd.scheduler.fragments.PermissionRequestFragment;
import prototype.xd.scheduler.utilities.ColorUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;

public class IntroActivity extends AppIntro {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Static.init(this);
        
        ColorUtilities.harmonizeColorsForActivity(this,
                R.color.gray_harmonized,
                R.color.green_harmonized,
                R.color.yellow_harmonized,
                R.color.dark_green_harmonized,
                R.color.dark_yellow_harmonized);
        
        setTransformer(AppIntroPageTransformerType.Depth.INSTANCE);
        addSlide(new IntroStartingFragment());
        addSlide(new PermissionRequestFragment());
        if (isXiaomiPhone()) {
            addSlide(AppIntroCustomLayoutFragment.newInstance(R.layout.intro_xiaomi_fragment));
        }
        
        View rootView = findViewById(android.R.id.content).getRootView();
        int surfaceColor = MaterialColors.getColor(rootView, R.attr.colorSurface);
        int surfaceColorVariant = MaterialColors.getColor(rootView, R.attr.colorSurfaceVariant);
        int primaryColor = MaterialColors.getColor(rootView, R.attr.colorPrimary);
        int secondaryColor = MaterialColors.getColor(rootView, R.attr.colorSecondary);

        setNavBarColor(surfaceColor);
        showStatusBar(true);
        setStatusBarColor(surfaceColor);

        setSystemBackButtonLocked(true);
        setSkipButtonEnabled(false);

        setNextArrowColor(primaryColor);
        setSeparatorColor(secondaryColor);
        setBarColor(surfaceColor);

        setDoneTextAppearance(R.style.MediumHeading);
        setColorDoneText(primaryColor);
        setDoneText(R.string.finish);
        setIndicatorColor(primaryColor, surfaceColorVariant);
    }
    
    private static boolean isXiaomiPhone() {
        return Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("xiaomi") ||
                Build.MODEL.toLowerCase(Locale.ROOT).contains("xiaomi");
    }
    
    @Override
    protected void onDonePressed(@Nullable Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // go back to the main activity
        Utilities.switchActivity(this, MainActivity.class);
    }
}
