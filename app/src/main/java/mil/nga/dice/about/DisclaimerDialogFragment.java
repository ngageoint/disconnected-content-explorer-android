package mil.nga.dice.about;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionActivity;

public class DisclaimerDialogFragment extends DialogFragment{

    private CheckBox checkBox;
    private View view;

    public interface OnDisclaimerDialogDismissedListener {
        public void onDisclaimerDialogDismissed(boolean exitApplication);
    }


    public static DisclaimerDialogFragment newInstance() {
        DisclaimerDialogFragment dialogFragment = new DisclaimerDialogFragment();
        return dialogFragment;
    }


    private OnDisclaimerDialogDismissedListener mOnDisclaimerDialogDismissedListener;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setStyle(STYLE_NORMAL, 0);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        view =  getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog, null);
        ((TextView)view.findViewById(R.id.disclaimer_dialog_textview)).setText(Html.fromHtml(getActivity().getString(R.string.disclaimer_text)));
        view.setMinimumWidth((int)(500 * getResources().getDisplayMetrics().density));
        view.setMinimumHeight((int)(400 * getResources().getDisplayMetrics().density));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(R.string.disclaimer_title_text);
        builder.setPositiveButton(getString(R.string.disclaimer_agree_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mOnDisclaimerDialogDismissedListener != null) {
                    mOnDisclaimerDialogDismissedListener.onDisclaimerDialogDismissed(false);
                }
                mOnDisclaimerDialogDismissedListener = null;
            }
        });

        builder.setNegativeButton(getString(R.string.disclaimer_exit_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mOnDisclaimerDialogDismissedListener != null) {
                    mOnDisclaimerDialogDismissedListener.onDisclaimerDialogDismissed(true);
                }
                mOnDisclaimerDialogDismissedListener = null;
            }
        });
        addCheckboxListener();
        return builder.create();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnDisclaimerDialogDismissedListener = (OnDisclaimerDialogDismissedListener)activity;
        }
        catch (ClassCastException caught) {
            throw new ClassCastException(activity.toString() + " must implement OnDisclaimerDialogDismissedListener");
        }
    }


    public void addCheckboxListener() {
        checkBox = (CheckBox)view.findViewById(R.id.show_disclaimer_checkbox);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(ReportCollectionActivity.HIDE_DISCLAIMER_KEY, checkBox.isChecked());
                editor.commit();
            }
        });
    }
}
