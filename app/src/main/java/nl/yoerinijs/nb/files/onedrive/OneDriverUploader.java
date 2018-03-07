package nl.yoerinijs.nb.files.onedrive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;
import com.microsoft.onedrivesdk.saver.SaverException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import nl.yoerinijs.nb.R;


/**
 * Created by Cooper on 2018-02-12.
 */

public class OneDriverUploader extends Activity {

    // May want to *hide* this
    private static final String ONEDRIVE_APP_ID = "eb45ee4d-a290-4206-8958-f2f142b7dc19";
    private String filename;
    private int filesize;
    /**
     * The OneDrive saver instance used by this activity
     */
    private ISaver mSaver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onedrive_saver);

        // Get passed file name and file size
        Bundle b = getIntent().getExtras();
        if (b != null) {
            filesize = b.getInt("size");
            filename = b.getString("name");
        }

        // Create the saver instance
        mSaver = Saver.createSaver(ONEDRIVE_APP_ID);

        // Add the start saving listener
        findViewById(R.id.startSaverButton).setOnClickListener(mStartPickingListener);
    }

    /**
     * The onClickListener that will start the OneDrive Picker
     */
    private final View.OnClickListener mStartPickingListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Activity activity = (Activity) v.getContext();
            activity.findViewById(R.id.result_table).setVisibility(View.INVISIBLE);

         // Create a file
            final File f = createExternalSdCardFile(filename, filesize);

            // Start the saver
            mSaver.startSaving(activity, filename, Uri.parse("file://" + f.getAbsolutePath()));
        }
    };


    // Control comes here after the saver is finished
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Check that we were able to save the file on OneDrive
        final TextView overallResult = (TextView) findViewById(R.id.overall_result);
        final TextView errorResult = (TextView) findViewById(R.id.error_type_result);
        final TextView debugErrorResult = (TextView) findViewById(R.id.debug_error_result);

        try {
            mSaver.handleSave(requestCode, resultCode, data);
            overallResult.setText(getString(R.string.overall_result_success));
            errorResult.setText(getString(R.string.error_message_none));
            debugErrorResult.setText(getString(R.string.error_message_none));
        } catch (final SaverException e) {
            overallResult.setText(getString(R.string.overall_result_failure));
            errorResult.setText(e.getErrorType().toString());
            debugErrorResult.setText(e.getDebugErrorInfo());
        }
        findViewById(R.id.result_table).setVisibility(View.VISIBLE);
    }

    /**
     * Creates an file on the SDCard
     * @param filename The name of the file to create
     * @param size The size in KB to make the file
     * @return The {@link File} object that was created
     */
    private File createExternalSdCardFile(final String filename, final int size) {
        final int bufferSize = size;
        final int alphabetRange = 'z' - 'a';
        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory(), filename);
            final FileOutputStream fos = new FileOutputStream(file);

            // Create a 1 kb size buffer to use in writing the temp file
            byte[] buffer = new byte[bufferSize];
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte)('a' + i % alphabetRange);
            }

            // Write out the file, 1 kb at a time
            for (int i = 0; i < size; i++) {
                fos.write(buffer, 0, buffer.length);
            }

            fos.close();
        } catch (final IOException e) {
            Toast.makeText(this, "Error when creating the file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return file;
    }



}
