package nl.yoerinijs.nb.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.yoerinijs.nb.R;
import nl.yoerinijs.nb.files.misc.LocationCentral;
import nl.yoerinijs.nb.files.onedrive.OneDriverUploader;
import nl.yoerinijs.nb.files.text.TextfileReader;
import nl.yoerinijs.nb.files.text.TextfileRemover;
import nl.yoerinijs.nb.files.text.TextfileWriter;
import nl.yoerinijs.nb.helpers.ImageAdapter;
import nl.yoerinijs.nb.storage.KeyValueDB;
import nl.yoerinijs.nb.validators.NoteBodyValidator;
import nl.yoerinijs.nb.validators.NoteTitleValidator;

/**
 * A class for creating and editing notes
 */
public class EditNoteActivity extends AppCompatActivity {

    private static final String PACKAGE_NAME = LocationCentral.PACKAGE;

    private static final String NOTES_ACTIVITY = LocationCentral.NOTES;

    private final Context m_context = this;

    private EditText m_noteTitle;

    private EditText m_noteBody;

    private GridView imagesView;

    private View m_focusView;

    private String m_location;

    private String m_password;

    private TextfileReader m_textFileReader;

    public ImageAdapter adapter;

    private Uri photoUri2;

    public static final String IMAGE_ADAPTER = "IMAGE_ADAPTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        m_focusView = null;

        FloatingActionButton m_saveButton = (FloatingActionButton) findViewById(R.id.saveButton);
        FloatingActionButton m_backButton = (FloatingActionButton) findViewById(R.id.backButton);
        FloatingActionButton m_deleteButton = (FloatingActionButton) findViewById(R.id.deleteButton);
        FloatingActionButton m_shareButton = (FloatingActionButton) findViewById(R.id.shareButton);
        FloatingActionButton m_cameraButton = (FloatingActionButton) findViewById(R.id.cameraButton);
        imagesView = (GridView) findViewById(R.id.imagesView);

        adapter = new ImageAdapter(m_context);
        imagesView.setAdapter(adapter);

        //Linking to the upload button
        FloatingActionButton m_uploadButton = (FloatingActionButton) findViewById(R.id.uploadButton);

        m_noteTitle = (EditText) findViewById(R.id.noteTitle);
        m_noteBody = (EditText) findViewById(R.id.noteText);
        m_password = getIntent().getStringExtra(LoginActivity.KEY_PASSWORD);
        m_location = getFilesDir().getAbsolutePath();
        m_textFileReader = new TextfileReader();

        final String note = getIntent().getStringExtra(NotesActivity.KEY_NOTE);
        final String noteFileName = getIntent().getStringExtra(NotesActivity.KEY_NOTE_TITLE);
       //Hide delete, share and upload when creating a new note
        if (null == note && null == noteFileName) {
            m_deleteButton.setVisibility(View.GONE);
            m_shareButton.setVisibility(View.GONE);
            m_uploadButton.setVisibility(View.GONE);
        } else {
            try {
                adapter.images = KeyValueDB.getURIs(m_context, noteFileName);
                adapter.notifyDataSetChanged();
            } catch (NoSuchAlgorithmException e) {
                //Error...no images will be displayed
            }
            m_noteTitle.setText(noteFileName);
            m_noteBody.setText(note);
        }

        imagesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                Intent intent = new Intent(view.getContext(), FullScreenImageActivity.class);
                intent.setData(adapter.images.get(position));
                intent.putExtra("ImageIndex", position);
                startActivityForResult(intent, 2020);
            }
        });

        m_saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String noteTitle = m_noteTitle.getText().toString();
                final String noteBody = m_noteBody.getText().toString();
                if(!onSave(noteTitle, noteBody)) {
                    if(m_textFileReader.fileExists(m_location, noteTitle, m_password, m_context)) {
                        new AlertDialog.Builder(m_context)
                                .setTitle(getString(R.string.dialog_title_note_exists))
                                .setMessage(getString(R.string.dialog_question_overwrite_note))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        writeNote(noteTitle, noteFileName, noteBody);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {}
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        writeNote(noteTitle, noteFileName, noteBody);
                    }
                }
            }
        });

        m_cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(m_context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(m_context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity)m_context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA }, 1111);
                } else {
                    openCameraAlertDialog();
                }
            }
        });

        m_backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNotesActivity();
            }
        });

        m_shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, m_noteBody.getText().toString());
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

        m_deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    new AlertDialog.Builder(m_context)
                            .setTitle(getString(R.string.dialog_title_delete_note))
                            .setMessage(getString(R.string.dialog_question_delete_note))
                            .setPositiveButton(getString(R.string.dialog_question_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), TextfileRemover.deleteFile(m_location, noteFileName) ?
                                            getString(R.string.success_deleted) : getString(R.string.error_cannot_delete) + ". ",
                                            Toast.LENGTH_SHORT).show();
                                    startNotesActivity();
                                }
                            })
                            .setNegativeButton(getString(R.string.dialog_question_deny), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {    }
                            })
                            .show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_cannot_delete) + ". ", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Onclick listener for the upload button
        m_uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Hey u want some upload? ", Toast.LENGTH_SHORT).show();
                //uploadNoteOD();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        for (int result: grantResults) {
            if (result != 0) {
                return;
            }
        }
        openCameraAlertDialog();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1314) {
                Uri selectedImage = data.getData();
                adapter.images.add(selectedImage);
            } else if (requestCode == 1313) {
                adapter.images.add(photoUri2);
            } else {
                adapter.images.remove(data.getIntExtra("ImageIndex", 0));
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void openCameraAlertDialog() {
        new AlertDialog.Builder(m_context)
                .setTitle(getString(R.string.dialog_question_camera))
                .setMessage(getString(R.string.dialog_question_overwrite_note))
                .setPositiveButton(R.string.dialog_answer_camera, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        openCamera();
                    }
                })
                .setNegativeButton(R.string.dialog_answer_camera_roll, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        openCameraRoll();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void openCameraRoll() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select A Picture"), 1314);
    }

    /// Open device camera
    private void openCamera() {
        // Create camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoUri;

        try {
            photoUri = FileProvider.getUriForFile(m_context, m_context.getApplicationContext().getPackageName() + ".my.package.name.provider", createImageFile());
        } catch (IOException ex) {
            // Error occurred while creating the File
            return;
        }
        photoUri2 = photoUri;
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Launch camera intent
        startActivityForResult(cameraIntent,1313);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEGss_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = new File(storageDir, imageFileName);
        return image;
    }

    /**
     * A method that validates the note input
     * @param noteTitle
     * @param noteBody
     * @return
     */
    private boolean onSave(@Nullable String noteTitle, @Nullable String noteBody) {
        boolean error = false;
        if(!NoteTitleValidator.isNoteTitleValid(noteTitle)) {
            m_noteTitle.setError(getString(R.string.error_invalid_note_title));
            m_focusView = m_noteTitle;
            error = true;
        }
        if(!NoteBodyValidator.isNoteBodyValid(noteBody)) {
            m_noteBody.setError(getString(R.string.error_invalid_note_body));
            m_focusView = m_noteBody;
            error = true;
        }
        if(error) {
            m_focusView.requestFocus();
        }
        return error;
    }

    /**
     * Write note. Should delete a note when the current note title is not the same as the old note title (i.e. title is changed).
     * @param currentNoteTile
     * @param oldNoteTitle
     * @param noteBody
     */
    private void writeNote(@NonNull String currentNoteTile, @Nullable final String oldNoteTitle, @NonNull String noteBody) {
        try {
            TextfileWriter textfileWriter = new TextfileWriter();
            textfileWriter.writeFile(m_context, currentNoteTile, noteBody, m_password, adapter.images);
            if(null != oldNoteTitle) {
                if(!currentNoteTile.equalsIgnoreCase(oldNoteTitle)) {
                    new AlertDialog.Builder(m_context)
                            .setTitle(getString(R.string.dialog_title_old_note))
                            .setMessage(getString(R.string.dialog_question_delete_old_note))
                            .setPositiveButton(getString(R.string.dialog_answer_delete_old_note), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    TextfileRemover.deleteFile(m_location, oldNoteTitle);
                                    postWriting();
                                }
                            })
                            .setNegativeButton(getString(R.string.dialog_answer_keep_old_note), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    postWriting();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    postWriting();
                }
            } else {
                postWriting();
            }
        } catch (Exception e) {
            return;
            // Something went wrong. Skip.
        }
    }

    /**
     * This method holds everything related to post writing.
     */
    private void postWriting() {
        Toast.makeText(getApplicationContext(), getString(R.string.success_saved) + ".", Toast.LENGTH_SHORT).show();
        startNotesActivity();
    }

    /**
     * A simple method to start the common notes activity
     */
    private void startNotesActivity() {
        Intent intent = new Intent();
        intent.setClassName(m_context, PACKAGE_NAME + "." + NOTES_ACTIVITY);
        intent.putExtra(LoginActivity.KEY_PASSWORD, m_password);
        startActivity(intent);
        finish();
    }

    /**
    * Opens the OneDrive application for the user to upload the current note to their OneDrive
    */
    private void uploadNoteOD() {
        String filename = m_noteTitle.getText().toString();
        int filesize = filename.length() + 50;
        Intent intent = new Intent(this, OneDriverUploader.class);
        Bundle b = new Bundle();
        b.putString("name", filename); b.putInt("size", filesize);
        intent.putExtras(b);
        startActivity(intent); //finish();
    }

}
