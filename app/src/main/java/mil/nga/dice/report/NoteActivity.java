package mil.nga.dice.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import mil.nga.dice.R;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class NoteActivity extends ActionBarActivity {
	
	Report mReport;
	
	File root = Environment.getExternalStorageDirectory();
	File notesDirectory = new File(root.getPath() + "/DICE/notes");
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_note);
		
		Bundle bundle = getIntent().getExtras();
		mReport = bundle.getParcelable("report");
		
		String notePath = notesDirectory.getPath() + "/" + mReport.getTitle() + ".txt";
		String noteText = readNote(notePath);
		TextView textView = (TextView)findViewById(R.id.noteTextArea);
		textView.setText(noteText);
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
		
		File note= new File(notesDirectory.getPath() + "/" + mReport.getTitle() + ".txt");
		
		if (!notesDirectory.exists()) {
			notesDirectory.mkdir();
		}
		
		try {
			note.createNewFile();
			FileOutputStream fOut = new FileOutputStream(note);
			fOut.write(textView.getText().toString().getBytes());
			fOut.flush();
			fOut.close();
			Toast.makeText(getBaseContext(), "Note saved", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getBaseContext(), "Problem saving note", Toast.LENGTH_SHORT).show();
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
	
	
	private String readNote(String path) {
		String noteText = "";
		
		File note = new File(path);
		if (note.exists()) {
			try {
				FileInputStream inputStream = new FileInputStream(note);
				
				if (inputStream != null) {
					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
					String readString = "";
					StringBuilder stringBuilder = new StringBuilder();
					
					while ((readString = bufferedReader.readLine()) != null) {
						stringBuilder.append(readString).append("\n");
					}
					
					inputStream.close();
					noteText = stringBuilder.toString();
				}
				
			} catch (Exception e) {
				Toast.makeText(getBaseContext(), "Problem loading note", Toast.LENGTH_SHORT).show();	
			}
		}
		
		return noteText;
	}
}
