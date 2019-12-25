package com.grspy.ulev1plus;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class UidListActivity extends AppCompatActivity {

    private DBHelper dbHelper = null;

    private ListView tagDataListView = null;

    private View popupInputDialogView = null;

    private EditText uidEditText = null;
    private EditText passwordEditText = null;
    private EditText packEditText = null;

    private Button saveTagDataButton = null;
    private Button cancelTagDataButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uid_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setSubtitle(R.string.uid_list);

        tagDataListView = findViewById(R.id.listview_tag_data);
        dbHelper = new DBHelper(UidListActivity.this);

        RefreshTagList();
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.uidlistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.add_new:
                showTagDataAlertDialog(null);
                return true;
            case R.id.import_list:
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_import_file)), 321);
                return true;
            case R.id.export_list:
                ArrayList<ULTag> ulTags = dbHelper.GetULTags();
                String output = "UID,PASSWORD,PACK\n";
                for (ULTag tag : ulTags) {
                    output += tag.getUid() + "," + tag.getPwd() + "," + tag.getPack() + "\n";
                }

                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.datetime_format));
                String filename = String.format(getString(R.string.uid_list_export_file), dateFormat.format(date));
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(file);
                    stream.write(output.getBytes());
                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.list_export_success), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Load file handler
        if (requestCode == 321 && resultCode == RESULT_OK) {
            Uri selectedFile = data.getData(); //The uri with the location of the file

            String mimeType = null;
            Cursor returnCursor =
                    null;
            if (selectedFile != null) {
                mimeType = getContentResolver().getType(selectedFile);
                returnCursor = getContentResolver().query(selectedFile, null, null, null, null);
            }

            //int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            if (returnCursor != null) {
                //returnCursor.moveToFirst();
                returnCursor.close();
            }

            //String filename = returnCursor.getString(nameIndex);
            //String sizeStr = Long.toString(returnCursor.getLong(sizeIndex));
            //int size = Integer.parseInt(sizeStr);

            if (mimeType != null && (mimeType.equals(getString(R.string.text_plain)) || mimeType.equals(getString(R.string.text_csv)))) {
                // just read the contents as text
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(selectedFile)));
                    String line;

                    while (br.ready() && (line = br.readLine()) != null) {
                        if (line.contains(getString(R.string.uid_pass_pack)))
                            continue;
                        String[] lineParts = line.split(",");
                        String uid = lineParts[0].trim();
                        String pass = lineParts[1].trim();
                        String pack = lineParts[2].trim();

                        if (!uid.isEmpty()) {
                            ULTag tag = new ULTag(uid);
                            if (!pass.isEmpty())
                                tag.setPwd(pass);
                            if (!pack.isEmpty())
                                tag.setPack(pack);

                            // Check if exists or add into the DB
                            ULTag dbUlTag = dbHelper.GetTagByUID(uid);
                            if (dbUlTag != null) {
                                if (!dbUlTag.isEqualWith(tag) && !pass.isEmpty()) {
                                    dbHelper.UpdateTag(uid, pass, pack);
                                }
                            } else {
                                dbHelper.insertTag(tag);
                            }
                        }
                    }
                    RefreshTagList();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerForContextMenu(tagDataListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listview_tag_data) {
            String[] actions = getResources().getStringArray(R.array.context_menu);
            for (int i = 0; i < actions.length; i++) {
                menu.add(Menu.NONE, i, i, actions[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.context_menu);
        String menuItemName = menuItems[menuItemIndex];

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        RelativeLayout relativeLayout = (RelativeLayout) info.targetView;

        String uid = ((TextView) relativeLayout.getChildAt(0)).getText().toString();

        switch (menuItemName) {
            case "Edit":
                // Show the same dialog with the input fields filled
                showTagDataAlertDialog(relativeLayout);
                break;

            case "Delete":
                // Delete from DB
                if (dbHelper == null)
                    dbHelper = new DBHelper(UidListActivity.this);
                dbHelper.DeleteTag(uid);
                RefreshTagList();
                break;
        }
        return true;
    }

    private void RefreshTagList() {
        ArrayList<HashMap<String, String>> tagList = dbHelper.GetTags();
        ListAdapter listAdapter = new SimpleAdapter(UidListActivity.this, tagList, R.layout.list_row, new String[]{"uid", "pass", "pack"}, new int[]{R.id.uid, R.id.password, R.id.pack});
        tagDataListView.setAdapter(listAdapter);
    }

    /* Initialize popup dialog view and ui controls in the popup dialog. */
    private void initPopupViewControls() {
        // Get layout inflater object.
        LayoutInflater layoutInflater = LayoutInflater.from(UidListActivity.this);

        // Inflate the popup dialog from a layout xml file.
        popupInputDialogView = layoutInflater.inflate(R.layout.popup_input_dialog, null);

        // Get tag input EditText and button ui controls in the popup dialog.
        uidEditText = popupInputDialogView.findViewById(R.id.uid);
        passwordEditText = popupInputDialogView.findViewById(R.id.password);
        packEditText = popupInputDialogView.findViewById(R.id.pack);
        saveTagDataButton = popupInputDialogView.findViewById(R.id.button_save_tag_data);
        cancelTagDataButton = popupInputDialogView.findViewById(R.id.button_cancel_tag_data);
    }

    /* Get current tag data from ListView and set them in the popup dialog EditText controls. */
    private void initEditTextTagDataInPopupDialog(RelativeLayout relativeLayout) {

        if (relativeLayout != null && relativeLayout.getChildCount() == 3) {
            String uid = ((TextView) relativeLayout.getChildAt(0)).getText().toString();
            String password = ((TextView) relativeLayout.getChildAt(1)).getText().toString();
            String pack = ((TextView) relativeLayout.getChildAt(2)).getText().toString();

            if (uidEditText != null) {
                uidEditText.setText(uid);
            }

            if (passwordEditText != null) {
                passwordEditText.setText(password);
            }

            if (packEditText != null) {
                packEditText.setText(pack);
            }
        }
    }

    private void showTagDataAlertDialog(RelativeLayout relativeLayout) {
        // Create a AlertDialog Builder.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UidListActivity.this);
        // Set title, icon, can not cancel properties.
        alertDialogBuilder.setTitle(getString(R.string.insert_uid_pass_pack));
        alertDialogBuilder.setCancelable(true);

        // Init popup dialog view and it's ui controls.
        initPopupViewControls();

        if (relativeLayout != null) {
            // Display values from the main activity list view in tag input EditText.
            initEditTextTagDataInPopupDialog(relativeLayout);
        }

        // Set the inflated layout view object to the AlertDialog builder.
        alertDialogBuilder.setView(popupInputDialogView);

        // Create AlertDialog and show.
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        // When user click the save tag data button in the popup dialog.
        saveTagDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get tag data from popup dialog EditText.
                String tagUid = uidEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String pack = packEditText.getText().toString();

                // Add to DB if it doesn't exist
                if (dbHelper == null)
                    dbHelper = new DBHelper(UidListActivity.this);

                ULTag ulTag = dbHelper.GetTagByUID(tagUid);
                if (ulTag != null) {
                    // just update
                    dbHelper.UpdateTag(tagUid, password, pack);
                } else {
                    dbHelper.insertTag(tagUid, password, pack);
                }

                RefreshTagList();

                alertDialog.cancel();
            }
        });

        cancelTagDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });
    }

}
