package prototype.xd.scheduler.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import prototype.xd.scheduler.R;

@SuppressWarnings("ConstantConditions")
public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource, int messageStringResource,
                                                   int cancelButtonResource, int confirmButtonResource,
                                                   View.OnClickListener confirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.confirmation_dialogue);
    
        MaterialButton cancel_button = dialog.findViewById(R.id.cancel_button);
        MaterialButton confirm_button = dialog.findViewById(R.id.confirm_button);
    
        cancel_button.setText(cancelButtonResource);
        confirm_button.setText(confirmButtonResource);
        
        cancel_button.setOnClickListener(v -> dialog.dismiss());
        confirm_button.setOnClickListener(v -> {
            confirmationListener.onClick(v);
            dialog.dismiss();
        });
    }
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                               int cancelButtonResource, int confirmButtonResource,
                                               OnClickListenerWithEditText confirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        addFocusChangeListener(editText);
    
        MaterialButton cancel_button = dialog.findViewById(R.id.cancel_button);
        MaterialButton confirm_button = dialog.findViewById(R.id.confirm_button);
    
        cancel_button.setText(cancelButtonResource);
        confirm_button.setText(confirmButtonResource);
        
        cancel_button.setOnClickListener(v -> dialog.dismiss());
        confirm_button.setOnClickListener(v -> {
            if (checkEmptyInput(editText)) {
                if (confirmationListener.onClick(v, editText.getText().toString(), 0)) {
                    dialog.dismiss();
                }
            }
        });
    }
    
    public static void displayEditTextSpinnerDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                                      int cancelButtonResource, int confirmButtonResource, int secondaryButtonResource,
                                                      String defaultEditTextValue,
                                                      ArrayList<?> items, int defaultIndex,
                                                      OnClickListenerWithEditText confirmationListener,
                                                      OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_spinner_dialogue);
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        editText.setText(defaultEditTextValue);
        addFocusChangeListener(editText);
        Spinner spinner = dialog.findViewById(R.id.groupSpinner);
        
        final ArrayAdapter<?> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        
        final int[] selectedIndex = {defaultIndex};
        spinner.setSelection(defaultIndex);
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedIndex[0] = position;
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        MaterialButton cancel_button = dialog.findViewById(R.id.cancel_button);
        MaterialButton confirm_button = dialog.findViewById(R.id.confirm_button);
        MaterialButton secondary_button = dialog.findViewById(R.id.secondary_action_button);
    
        cancel_button.setText(cancelButtonResource);
        confirm_button.setText(confirmButtonResource);
        
        cancel_button.setOnClickListener(v -> dialog.dismiss());
        confirm_button.setOnClickListener(v -> {
            if (checkEmptyInput(editText)) {
                if (confirmationListener.onClick(v, editText.getText().toString(), selectedIndex[0])) {
                    dialog.dismiss();
                }
            }
        });
        if (secondaryConfirmationListener != null) {
            secondary_button.setText(secondaryButtonResource);
            secondary_button.setOnClickListener(v -> {
                if (checkEmptyInput(editText)) {
                    if (secondaryConfirmationListener.onClick(v, editText.getText().toString(), selectedIndex[0])) {
                        dialog.dismiss();
                    }
                }
            });
        } else {
            secondary_button.setVisibility(View.GONE);
        }
    }
    
    private static boolean checkEmptyInput(@NonNull EditText editText) {
        if (editText.getText() == null || editText.getText().toString().trim().equals("")) {
            editText.setError(editText.getContext().getString(R.string.input_cant_be_empty));
            return false;
        }
        return true;
    }
    
    private static void addFocusChangeListener(@NonNull EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 200));
        editText.requestFocus();
    }
    
    private static Dialog buildTemplate(Context context, int titleStringResource, int messageStringResource, int layout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(layout);
        return builder.show();
    }
    
    public abstract static class OnClickListenerWithEditText {
        public abstract boolean onClick(View view, String text, int selectedIndex);
    }
    
}
