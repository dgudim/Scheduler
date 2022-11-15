package prototype.xd.scheduler.utilities;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.views.Spinner;

public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @StringRes int secondaryButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener,
                                                   @Nullable View.OnClickListener secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.three_buttons);
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                v -> {
                    confirmationListener.onClick(v);
                    dialog.dismiss();
                }, secondaryConfirmationListener == null ? null : v -> {
                    secondaryConfirmationListener.onClick(v);
                    dialog.dismiss();
                });
    }
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, messageStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, -1,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    @SuppressWarnings("ConstantConditions")
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintStringResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        setupEditText(editText, defaultEditTextValue);
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonResource,
                v -> {
                    if (checkEmptyInput(editText)
                            && confirmationListener.onClick(v, editText.getText().toString().trim(), 0)) {
                        dialog.dismiss();
                    }
                }, secondaryConfirmationListener == null ? null : v -> {
                    if (checkEmptyInput(editText)
                            && secondaryConfirmationListener.onClick(v, editText.getText().toString().trim(), 0)) {
                        dialog.dismiss();
                    }
                });
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                "",
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                defaultEditTextValue,
                confirmationListener, secondaryConfirmationListener);
    }
    
    @SuppressWarnings("ConstantConditions")
    public static void displayEditTextSpinnerDialogue(@NonNull Context context,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int hintStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @StringRes int secondaryButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull List<?> items,
                                                      int defaultIndex,
                                                      @NonNull OnClickListenerWithEditText confirmationListener,
                                                      @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_spinner_dialog);
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintStringResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        setupEditText(editText, defaultEditTextValue);
        Spinner spinner = dialog.findViewById(R.id.groupSpinner);
        
        final ArrayAdapter<?> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        
        final int[] selectedIndex = {defaultIndex};
        spinner.setSelectionSilent(defaultIndex);
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedIndex[0] = position;
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
            }
        });
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                v -> {
                    if (checkEmptyInput(editText)
                            && confirmationListener.onClick(v, editText.getText().toString().trim(), selectedIndex[0])) {
                        dialog.dismiss();
                    }
                }, secondaryConfirmationListener == null ? null : v -> {
                    if (checkEmptyInput(editText)
                            && secondaryConfirmationListener.onClick(v, editText.getText().toString().trim(), selectedIndex[0])) {
                        dialog.dismiss();
                    }
                });
    }
    
    private static void setupEditText(@NonNull EditText editText,
                                      @NonNull String defaultEditTextValue) {
        editText.setText(defaultEditTextValue);
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 200));
        editText.requestFocus();
    }
    
    private static void setupButtons(Dialog dialog,
                                     @StringRes int cancelButtonStringResource,
                                     @StringRes int confirmButtonStringResource,
                                     @StringRes int secondaryButtonStringResource,
                                     @NonNull View.OnClickListener confirmationListener,
                                     @Nullable View.OnClickListener secondaryConfirmationListener) {
        
        MaterialButton confirmButton = dialog.findViewById(R.id.confirm_button);
        MaterialButton secondaryButton = dialog.findViewById(R.id.secondary_action_button);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        
        cancelButton.setText(cancelButtonStringResource);
        confirmButton.setText(confirmButtonStringResource);
        if (secondaryButtonStringResource != -1) {
            secondaryButton.setText(secondaryButtonStringResource);
        } else {
            secondaryButton.setVisibility(View.GONE);
            dialog.findViewById(R.id.button_flex_container).setLayoutParams(
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(confirmationListener);
        if (secondaryConfirmationListener != null) {
            secondaryButton.setOnClickListener(secondaryConfirmationListener);
        }
    }
    
    private static boolean checkEmptyInput(@NonNull EditText editText) {
        if (editText.getText() == null || editText.getText().toString().trim().equals("")) {
            editText.setError(editText.getContext().getString(R.string.input_cant_be_empty));
            return false;
        }
        return true;
    }
    
    private static Dialog buildTemplate(@NonNull Context context,
                                        @StringRes int titleStringResource,
                                        @StringRes int messageStringResource,
                                        @LayoutRes int layoutRes) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(layoutRes);
        return builder.show();
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithEditText {
        boolean onClick(View view, String text, int selectedIndex);
    }
    
}
