package mil.nga.dice.about;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import mil.nga.dice.R;

public class DisclaimerDialogFragment extends DialogFragment {

    private CheckBox showDisclaimerCheckBox;
    private View view;

    public interface OnDisclaimerDialogDismissedListener {
        public void onDisclaimerDialogAgree(DisclaimerDialogFragment disclaimerDialog);
        public void onDisclaimerDialogDisagree(DisclaimerDialogFragment disclaimerDialog);
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
        view.setMinimumWidth((int) (500 * getResources().getDisplayMetrics().density));
        view.setMinimumHeight((int) (400 * getResources().getDisplayMetrics().density));

        String disclaimer = loadDisclaimer();
        TextView disclaimerTextView = ((TextView)view.findViewById(R.id.disclaimer_dialog_textview));
        disclaimerTextView.setText(Html.fromHtml(disclaimer));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(R.string.disclaimer_title_text);
        builder.setPositiveButton(getString(R.string.disclaimer_agree_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mOnDisclaimerDialogDismissedListener != null) {
                    mOnDisclaimerDialogDismissedListener.onDisclaimerDialogAgree(DisclaimerDialogFragment.this);
                }
                mOnDisclaimerDialogDismissedListener = null;
            }
        });
        builder.setNegativeButton(getString(R.string.disclaimer_exit_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (mOnDisclaimerDialogDismissedListener != null) {
                    mOnDisclaimerDialogDismissedListener.onDisclaimerDialogDisagree(DisclaimerDialogFragment.this);
                }
                mOnDisclaimerDialogDismissedListener = null;
            }
        });

        showDisclaimerCheckBox = (CheckBox)view.findViewById(R.id.show_disclaimer_checkbox);

        return builder.create();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnDisclaimerDialogDismissedListener = (OnDisclaimerDialogDismissedListener) activity;
        }
        catch (ClassCastException caught) {
            throw new Error(activity.toString() + " must implement OnDisclaimerDialogDismissedListener");
        }
    }


    public boolean isShowDisclaimerChecked() {
        return showDisclaimerCheckBox.isChecked();
    }

    private String loadDisclaimer() {
        try {
            TransformerFactory xf = TransformerFactory.newInstance();
            StringBuilder xslt = new StringBuilder()
                    .append("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">")
                    .append(  "<xsl:template match=\"/\">")
                    .append(    "<xsl:apply-templates select=\"//div[@id='disclaimer']\"/>")
                    .append(  "</xsl:template>")
                    .append(  "<xsl:template match=\"div[@id='disclaimer']\">")
                    .append(    "<xsl:copy-of select=\"*[local-name() != 'h1']\"/>")
                    .append(  "</xsl:template>")
                    .append("</xsl:stylesheet>");
            Transformer extractDisclaimer = xf.newTransformer(new StreamSource(new ByteArrayInputStream(xslt.toString().getBytes("UTF-8"))));
            extractDisclaimer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            extractDisclaimer.setOutputProperty(OutputKeys.STANDALONE, "no");
            extractDisclaimer.setOutputProperty(OutputKeys.METHOD, "html");
            InputStream in = getActivity().getAssets().open("legal/legal.html");
            ByteArrayOutputStream resultBytes = new ByteArrayOutputStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document sourceDoc = dbf.newDocumentBuilder().parse(in);
            extractDisclaimer.transform(new DOMSource(sourceDoc), new StreamResult(resultBytes));
            String disclaimer = resultBytes.toString();
            return disclaimer;
        }
        catch (Exception e) {
            throw new Error("error loading legal disclaimer", e);
        }
    }
}
