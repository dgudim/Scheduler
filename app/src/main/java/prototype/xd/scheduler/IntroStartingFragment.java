package prototype.xd.scheduler;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;

import prototype.xd.scheduler.views.CheckBox;

public class IntroStartingFragment extends Fragment implements SlidePolicy {
    
    private CheckBox understoodCheckbox;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_starting_fragment, container, false);
        view.findViewById(R.id.github_button).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dgudim/Scheduler/issues"))));
        understoodCheckbox = view.findViewById(R.id.understood_checkbox);
        return view;
    }
    
    @Override
    public boolean isPolicyRespected() {
        return understoodCheckbox.isChecked();
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(getActivity(), getString(R.string.please_read_the_notes),
                Toast.LENGTH_LONG).show();
    }
}
