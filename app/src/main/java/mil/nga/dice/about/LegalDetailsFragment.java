package mil.nga.dice.about;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mil.nga.dice.R;


public class LegalDetailsFragment  extends android.support.v4.app.Fragment  {

    @Override
    public void onCreate (Bundle savedInstance) {
        super.onCreate(savedInstance);


    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_legal, container, false);
        ((TextView)v.findViewById(R.id.attribution)).setText(Html.fromHtml(getActivity().getString(R.string.attributions)));
        ((TextView)v.findViewById(R.id.disclaimer)).setText(Html.fromHtml(getActivity().getString(R.string.disclaimer_text)));
        ((TextView)v.findViewById(R.id.privacy_policy)).setText(Html.fromHtml(getActivity().getString(R.string.privacy_policy)));
        return v;
    }

}
