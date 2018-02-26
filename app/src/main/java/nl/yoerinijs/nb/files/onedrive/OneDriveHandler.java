package nl.yoerinijs.nb.files.onedrive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.IPickerResult;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;

import java.io.File;

/**
 * Created by Cooper on 2018-02-12.
 */

public class OneDriveHandler {

    private final Context context;


    public OneDriveHandler( Context context ) {
        this.context = context;
    }


    // OneDrive additions


    private IPicker mPicker;
    private ISaver mSaver;

    // Might want to hide this
    private String ONEDRIVE_APP_ID = "eb45ee4d-a290-4206-8958-f2f142b7dc19";

    // The onClickListener that will start the OneDrive picker

    // TODO: BIND THIS TO A BUTTON SO THAT THE USER CAN ACTUALLY LOOK AT THEIR ONEDRIVE FILES
    private final View.OnClickListener startPickingListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            mPicker = Picker.createPicker(ONEDRIVE_APP_ID);
            mPicker.startPicking((Activity)view.getContext(), LinkType.WebViewLink);
        }
    };

    // Handles results of the OneDrive 'popup'
    // Is called whenever a file is selected or user cancels
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Get the results from the picker
        IPickerResult result = mPicker.getPickerResult(requestCode, resultCode, data);
        // Handle the case if nothing was picked
        if (result != null) {
            // Do something with the picked file
            Log.d("main", "Link to file '" + result.getName() + ": " + result.getLink());
            return;
        }

        // Handle non-OneDrive picker request
        onActivityResult(requestCode, resultCode, data);
    }

    // The onClickListener that will start the OneDrive picker

    // TODO: BIND THIS TO A BUTTON SO THAT THE USER CAN ACTUALLY UPLOAD TO ONEDRIVE
    private final View.OnClickListener startSavingListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            // create example file to save to OneDrive
            final String filename = "file.txt";
            final File f = new File(context.getFilesDir(), filename);

            // create and launch the saver
            mSaver = Saver.createSaver(ONEDRIVE_APP_ID);
            mSaver.startSaving((Activity)view.getContext(), filename, Uri.fromFile(f));
        }
    };


    // End OneDrive additions


}
