package prototype.xd.scheduler.utilities;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.views.Spinner;

public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource, int messageStringResource,
                                                   int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                                   View.OnClickListener confirmationListener,
                                                   View.OnClickListener secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.three_buttons);
        
        setupButtons(dialog,
                cancelButtonResource, confirmButtonResource, secondaryButtonResource,
                v -> {
                    confirmationListener.onClick(v);
                    dialog.dismiss();
                }, secondaryConfirmationListener == null ? null : v -> {
                    secondaryConfirmationListener.onClick(v);
                    dialog.dismiss();
                });
    }
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource, int messageStringResource,
                                                   int cancelButtonResource, int confirmButtonResource,
                                                   View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, messageStringResource,
                cancelButtonResource, confirmButtonResource, -1,
                confirmationListener, null);
    }
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource,
                                                   int cancelButtonResource, int confirmButtonResource,
                                                   View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, -1,
                cancelButtonResource, confirmButtonResource, -1,
                confirmationListener, null);
    }
    
    @SuppressWarnings("ConstantConditions")
    public static void displayEditTextDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                               String defaultEditTextValue,
                                               OnClickListenerWithEditText confirmationListener,
                                               OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        setupEditText(editText, defaultEditTextValue);
        
        setupButtons(dialog,
                cancelButtonResource, confirmButtonResource, secondaryButtonResource,
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
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource,
                                               String defaultEditTextValue,
                                               OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintResource,
                cancelButtonResource, confirmButtonResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource,
                                               OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintResource,
                cancelButtonResource, confirmButtonResource, -1,
                "",
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource,
                                               String defaultEditTextValue,
                                               OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintResource,
                cancelButtonResource, confirmButtonResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                               String defaultEditTextValue,
                                               OnClickListenerWithEditText confirmationListener,
                                               OnClickListenerWithEditText secondaryConfirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintResource,
                cancelButtonResource, confirmButtonResource, secondaryButtonResource,
                defaultEditTextValue,
                confirmationListener, secondaryConfirmationListener);
    }
    
    @SuppressWarnings("ConstantConditions")
    public static void displayEditTextSpinnerDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                                      int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                                      String defaultEditTextValue,
                                                      List<?> items, int defaultIndex,
                                                      OnClickListenerWithEditText confirmationListener,
                                                      OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_spinner_dialogue);
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintResource);
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
                cancelButtonResource, confirmButtonResource, secondaryButtonResource,
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
    
    private static void setupEditText(EditText editText, String defaultEditTextValue) {
        editText.setText(defaultEditTextValue);
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 200));
        editText.requestFocus();
    }
    
    private static void setupButtons(Dialog dialog,
                                     int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                     View.OnClickListener confirmationListener,
                                     View.OnClickListener secondaryConfirmationListener) {
        
        MaterialButton confirmButton = dialog.findViewById(R.id.confirm_button);
        MaterialButton secondaryButton = dialog.findViewById(R.id.secondary_action_button);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        
        cancelButton.setText(cancelButtonResource);
        confirmButton.setText(confirmButtonResource);
        if (secondaryButtonResource != -1) {
            secondaryButton.setText(secondaryButtonResource);
        } else {
            secondaryButton.setVisibility(View.GONE);
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
    
    private static Dialog buildTemplate(Context context, int titleStringResource, int messageStringResource, int layout) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(layout);
        return builder.show();
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithEditText {
        boolean onClick(View view, String text, int selectedIndex);
    }
    
}
