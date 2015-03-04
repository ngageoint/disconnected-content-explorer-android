package mil.nga.dice;

import android.content.Context;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import mil.nga.dice.report.Report;

/**

 */
public class JavaScriptAPI {
    Context mContext;
    Report mReport;
    File root = Environment.getExternalStorageDirectory();
    File exportDirectory = new File(root.getPath() + "/DICE/export");


    public JavaScriptAPI(Context c, Report r) {
        mContext = c;
        mReport = r;
    }


    @JavascriptInterface
    public void saveToFile(String data) {

        File export = new File(exportDirectory.getPath() + "/" + mReport.getTitle() + ".txt");

        if (!exportDirectory.exists()) {
            exportDirectory.mkdir();
        }

        try {
            export.createNewFile();
            FileOutputStream fOut = new FileOutputStream(export);
            fOut.write(data.getBytes());
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: send an error object back through the webview so the user can handle it
        }
    }


    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }


    @JavascriptInterface
    public void getLocation() {

    }
}
