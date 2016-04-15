package mil.nga.dice.report;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import mil.nga.dice.R;

public class NoteActivity extends AppCompatActivity {
	
	private Report mReport;
	private File notesDirectory = ReportManager.getInstance().getNotesDir();

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_note);
		
		Bundle bundle = getIntent().getExtras();
		mReport = bundle.getParcelable("report");
		
		String noteText = readNote();
		TextView textView = (TextView)findViewById(R.id.noteTextArea);
		textView.setText(noteText);
        setTitle(mReport.getTitle() + " note");
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				saveNote();
				finish();
				return true;
			case R.id.save_note:
				saveNote();
				return true;
			case R.id.cancel_note:
				cancelNote();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	private void saveNote() {
		// TODO Auto-generated method stub
		TextView textView = (TextView)findViewById(R.id.noteTextArea);
		
		File note = ReportManager.getInstance().noteFileForReport(mReport);
		
		try {
			note.createNewFile();
			FileOutputStream fOut = new FileOutputStream(note);
			fOut.write(textView.getText().toString().getBytes());
			fOut.flush();
			fOut.close();
			Toast.makeText(getBaseContext(), R.string.note_saved, Toast.LENGTH_SHORT).show();
		}
        catch (IOException e) {
			Toast.makeText(getBaseContext(), R.string.note_save_error, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		finish();
	}


	private void cancelNote() {
		finish();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_note, menu);
	    return true;
	}
	
	
	private String readNote() {
		File note = ReportManager.getInstance().noteFileForReport(mReport);
		if (!note.exists()) {
            return "";
        }

        try {
            BufferedReader noteReader = new BufferedReader(new InputStreamReader(new FileInputStream(note)));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = noteReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            noteReader.close();
            return stringBuilder.toString();
        }
        catch (Exception e) {
            Toast.makeText(getBaseContext(), R.string.note_load_error, Toast.LENGTH_SHORT).show();
        }

		return "";
	}

}
