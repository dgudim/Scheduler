package prototype.xd.scheduler.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import prototype.xd.scheduler.R;

public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource, int messageStringResource,
                                                   View.OnClickListener confirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.confirmation_dialogue);
        
        dialog.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
        
        dialog.findViewById(R.id.confirm_button).setOnClickListener(v -> {
            confirmationListener.onClick(v);
            dialog.dismiss();
        });
    }
    
    public static void displayConfirmationDialogue(Context context, int titleStringResource,
                                                   View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, -1, confirmationListener);
    }
    
    public static void displayEditTextDialogue(Context context, int titleStringResource, int messageStringResource, int hintResource,
                                               OnClickListenerWithEditText confirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 200));
        editText.requestFocus();
        dialog.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.confirm_button).setOnClickListener(v -> {
            if (confirmationListener.onClick(v, editText)) {
                dialog.dismiss();
            }
        });
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
        public abstract boolean onClick(View view, TextInputEditText editText);
    }
    
}
