package mil.nga.dice.report;

import mil.nga.dice.R;
import mil.nga.dice.R.layout;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NoteFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
		return inflater.inflate(R.layout.activity_note, container, false);
	}	
}
