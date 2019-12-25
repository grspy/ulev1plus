package com.grspy.ulev1plus;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private DBHelper dbHelper = null;

    private NfcAdapter adapter = null;
    private PendingIntent pendingIntent = null;
    private Tag tag = null;

    private String inputDialogResult;

    private EditText tagContentsView;
    private TextView legendTextView;

    final String TAG = MainActivity.class.getCanonicalName();

    @Override
    public void onCreate(final Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        legendTextView = findViewById(R.id.legendTextView);
        tagContentsView = findViewById(R.id.tag_contents_view);

        Button read_button = findViewById(R.id.btn_read);
        Button write_button = findViewById(R.id.btn_write);
        Button save_button = findViewById(R.id.btn_save);
        Button load_button = findViewById(R.id.btn_load);

        tagContentsView.setMovementMethod(new ScrollingMovementMethod());

        read_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Read button clicked
                readMode();
            }
        });

        write_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Write button clicked
                writeMode();
            }
        });

        load_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Load button clicked
                loadDump();
            }
        });

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save button clicked
                saveDump();
            }
        });

        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter == null)
            return;

        if (!adapter.isEnabled()) {
            Utils.showNfcSettingsDialog(this);
            return;
        }

        if (pendingIntent == null) {
            pendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }

        adapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null)
            adapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Discovered tag
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String tagId = Utils.bytesToHex(tag.getId());
        if (verifyMFULEV1()) {
            Toast.makeText(getApplicationContext(), String.format(getString(R.string.ulev1_tag_found), tagId), Toast.LENGTH_LONG).show();
        } else {
            tag = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Load file handler
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedFile = data.getData(); //The uri with the location of the file

            String mimeType = null;
            Cursor returnCursor = null;

            if (selectedFile != null) {
                mimeType = getContentResolver().getType(selectedFile);
                returnCursor = getContentResolver().query(selectedFile, null, null, null, null);
            }

            //int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = 0;
            if (returnCursor != null) {
                sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
            }

            //String filename = returnCursor.getString(nameIndex);
            String sizeStr = null;
            if (returnCursor != null) {
                sizeStr = Long.toString(returnCursor.getLong(sizeIndex));
            }

            if (returnCursor != null) {
                returnCursor.close();
            }

            int size = 0;
            if (sizeStr != null) {
                size = Integer.parseInt(sizeStr);
            }

            if (mimeType != null) {

                String loadedData = null;

                if (mimeType.equals(getString(R.string.text_plain))) {
                    // just read the contents as text
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(selectedFile)));
                        StringBuilder sb = new StringBuilder();
                        String line;

                        while (br.ready() && (line = br.readLine()) != null) {
                            sb.append(line);
                        }

                        loadedData = Utils.pageBreak(sb.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Treat as byte file
                    byte[] bytes = new byte[size];

                    try {
                        BufferedInputStream buf = new BufferedInputStream(getContentResolver().openInputStream(selectedFile));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                        // If importing from pm3 dump we need to get only the block data
                        String bytesToHex = Utils.bytesToHex(bytes);
                        if (bytesToHex != null) {
                            if (hasPM3ULHeader(bytes, size) && bytesToHex.length() > 56 * 2) {
                                loadedData = Utils.pageBreak(bytesToHex.substring(56 * 2));
                            } else {
                                loadedData = Utils.pageBreak(bytesToHex);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if ((loadedData != null) && (size >= 20 * 4)) {
                    // Color the bytes like in readMode
                    SpannableString spannableLoadedData = new SpannableString(loadedData);
                    colorTagBytes((size > 25 * 4 ? 2 : 1), spannableLoadedData); // Type 1 shouldn't exceed 25 pages (x 4 bytes)
                    tagContentsView.setText(spannableLoadedData);

                    showColorLegend();
                }
            }
        }
    }

    // Code borrowed from the ChameleonMini GUI software to identify if the PM3 dump has the new UL header
    private boolean hasPM3ULHeader(byte[] bytes, int size) {
        if (size % 4 != 0 || size <= 56)
            return false;

        // tbo should be ZERO
        if (bytes[8] != 0x00 || bytes[9] != 0x00)
            return false;

        // tbo1 should be ZERO
        if (bytes[10] != 0x00)
            return false;

        // pages count must be equals to pages in header
        int maxPage = (size - 56) / 4 - 1;
        return maxPage == bytes[11];
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.list_uid) {
            Intent listUidIntent = new Intent(this, UidListActivity.class);
            this.startActivity(listUidIntent);
            return true;
        }

        if (item.getItemId() == R.id.about) {
            showAboutBox();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutBox() {
        String curVersion = "";
        // Get version
        try {
            curVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SpannableString aboutText = new SpannableString(String.format(getString(R.string.about_box_text), curVersion));

        View about;
        TextView aboutTextView;
        try {
            LayoutInflater inflater = getLayoutInflater();
            about = inflater.inflate(R.layout.about_box, (ViewGroup) findViewById(R.id.aboutView));
            aboutTextView = about.findViewById(R.id.aboutTextView);
        } catch (InflateException e) {
            about = aboutTextView = new TextView(this);
        }

        aboutTextView.setText(aboutText);
        // Linkify the links
        Linkify.addLinks(aboutTextView, Linkify.ALL);
        // Show the dialog
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_box_title))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.ok), null)
                .setView(about)
                .show();
    }

    public String getPassFromUserDialog(Context context) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }
        };

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(getString(R.string.enter_tag_pass));
        if (tag != null)
            alert.setMessage(String.format(getString(R.string.uid), Utils.bytesToHex(tag.getId())));

        final EditText input = new EditText(context);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        alert.setView(input);

        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                inputDialogResult = input.getText().toString();
                handler.sendMessage(handler.obtainMessage());
            }
        });
        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                inputDialogResult = null;
                handler.sendMessage(handler.obtainMessage());
            }
        });
        alert.show();

        try {
            Looper.loop();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return inputDialogResult;
    }

    // Reads the contents of the tag and displays them into the TextView
    private void readMode() {
        if (tag != null) {
            MifareUltralight mFUL = MifareUltralight.get(tag);
            try {
                mFUL.connect();
                // send a GET_VERSION command to ensure we are still talking with an ULEV1 tag
                byte[] resp = mFUL.transceive(new byte[]{0x60});

                if (resp[4] == (byte) 0x01) {
                    // Read the pages
                    byte[] mFUPages = {};

                    // TODO: Read the configuration pages to see if all pages are accessible, else ask/check for password or abort
                    int mfuType = 0;

                    if (resp[6] == (byte) 0x0b) {
                        // MF0UL11
                        // user memory is 48 bytes (12 pages)
                        mfuType = 1;

                        /*
                        mFUPages = new byte[128];
                        for (int i = 0; i < 17; i += 4) {
                            System.arraycopy(mFUL.readPages(i), 0, mFUPages, i * 4, 16);
                        }
                        */

                        // Doing a fast read (3Ah) instead
                        mFUPages = mFUL.transceive(new byte[]{0x3a, 0x00, 0x13});
                    } else if (resp[6] == (byte) 0x0e) {
                        // MF0UL21
                        // user memory is 128 bytes (32 pages)
                        mfuType = 2;

                        /*
                        mFUPages = new byte[164];
                        for (int i = 0; i < 38; i += 4) {
                            System.arraycopy(mFUL.readPages(i), 0, mFUPages, i * 4, 16);
                        }
                         */

                        // Doing a fast read (3Ah) instead
                        mFUPages = mFUL.transceive(new byte[]{0x3a, 0x00, 0x28});
                    }

                    String hexContentsStr = Utils.pageBreak(Utils.bytesToHex(mFUPages));
                    SpannableString spannableStr = new SpannableString(hexContentsStr);

                    colorTagBytes(mfuType, spannableStr);

                    tagContentsView.setText(spannableStr);

                    showColorLegend();

                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.not_ulev1_tag_error), Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.tag_connect_error), Toast.LENGTH_LONG).show();
            } finally {
                if (mFUL != null) {
                    try {
                        mFUL.close();
                    } catch (Exception ex) {
                        //Log.e(TAG, ex.toString());
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.read_tag_error), Toast.LENGTH_LONG).show();
        }
    }

    private void showColorLegend() {
        SpannableString spannableLegendStr = new SpannableString(getString(R.string.color_legend));
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.uid_color))), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // UID
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.bcc_color))), 6, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // BCC
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.lockbytes_color))), 12, 21, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Lock bytes
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.otp_color))), 24, 27, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // OTP
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.cfg_color))), 30, 33, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // CFG
        spannableLegendStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.pwdpack_color))), 36, 44, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // PWD
        legendTextView.setText(spannableLegendStr);
    }

    private void colorTagBytes(int mfuType, SpannableString spannableStr) {
        // TODO: Fix this a bit (like declaring CONSTANTS for all these magic numbers)
        // Page index: PAGE_NR * (8 CHARACTERS + 1 NEW_LINE) + [CHARACTER_INDEX_IN_PAGE]
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.uid_color))), 0 * (8 + 1), 0 * (8 + 1) + 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // UID1
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.bcc_color))), 0 * (8 + 1) + 6, 0 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // BCC1
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.uid_color))), 1 * (8 + 1), 1 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // UID2
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.bcc_color))), 2 * (8 + 1), 2 * (8 + 1) + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // BCC2
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.lockbytes_color))), 2 * (8 + 1) + 4, 2 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Lock bytes
        spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.otp_color))), 3 * (8 + 1), 3 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // OTP

        if (mfuType == 1) {
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.cfg_color))), 16 * (8 + 1), 37 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // CFG0
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.cfg_color))), 17 * (8 + 1), 38 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // CFG1
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.pwdpack_color))), 18 * (8 + 1), 39 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // PWD
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.pwdpack_color))), 19 * (8 + 1), 40 * (8 + 1) + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // PACK
        } else if (mfuType == 2) {
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.lockbytes_color))), 36 * (8 + 1), 36 * (8 + 1) + 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Lock bytes
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.cfg_color))), 37 * (8 + 1), 37 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // CFG0
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.cfg_color))), 38 * (8 + 1), 38 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // CFG1
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.pwdpack_color))), 39 * (8 + 1), 39 * (8 + 1) + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // PWD
            spannableStr.setSpan(new ForegroundColorSpan(Color.parseColor(getString(R.string.pwdpack_color))), 40 * (8 + 1), 40 * (8 + 1) + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // PACK
        }
    }

    // Writes the contents of the TextView to the tag
    // Asks for password from user if the database doesn't contain it
    private void writeMode() {

        if (tag != null) {
            MifareUltralight mFUL = MifareUltralight.get(tag);
            String uid = Utils.bytesToHex(tag.getId());

            if (tagContentsView.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), getString(R.string.no_data_error), Toast.LENGTH_LONG).show();
            } else {
                byte[] pass = null;
                byte[] pack = null;

                // 1. Check if there is a password for this UID stored
                ULTag ultag = dbHelper.GetTagByUID(uid);
                if (ultag != null && ultag.getUid().equals(uid) && !ultag.getPwd().isEmpty()) {
                    // we have a pass
                    pass = Utils.hexStringToByteArray(ultag.getPwd());

                    // check if we have also a pack
                    if (!ultag.getPack().isEmpty()) {
                        pack = Utils.hexStringToByteArray(ultag.getPack());
                    }
                }

                if (pass == null) {
                    // Ask user for PWD
                    String passStr = getPassFromUserDialog(this);

                    if (passStr != null && !passStr.isEmpty()) {
                        pass = Utils.hexStringToByteArray(passStr);
                    }
                }

                if (pass != null && pass.length == 4) {
                    // 2. Authenticate using the password and write the data
                    try {
                        mFUL.connect();

                        // send a GET_VERSION command to ensure we are still talking with an ULEV1 tag
                        byte[] versionResp = mFUL.transceive(new byte[]{0x60});

                        if (versionResp[4] == (byte) 0x01) {
                            // it's an ULEV1 tag

                            // Authenticate
                            byte[] authResp = mFUL.transceive(new byte[]{
                                    (byte) 0x1B, // PWD_AUTH
                                    pass[0], pass[1], pass[2], pass[3]
                            });

                            if ((authResp != null) && (authResp.length >= 2)) {
                                // success

                                byte[] packReceived = Arrays.copyOf(authResp, 2);

                                if (pack != null && pack.length == 2) {
                                    // TODO: Why do we compare the pack? Should we remove it entirely?
                                    if (Arrays.equals(packReceived, pack)) {
                                        //Log.d(TAG, "PACK verified");
                                    } else {
                                        //Log.d(TAG, "Stored PACK (0x" + Utils.bytesToHex(pack) + ") different than the received one (0x" + Utils.bytesToHex(packReceived) + ")");
                                    }
                                }

                                // Save password and pack into DB
                                if (ultag == null) {
                                    // create a new ULTag
                                    ultag = new ULTag(uid);
                                    dbHelper.insertTag(ultag);
                                }
                                ultag.setPwd(Utils.bytesToHex(pass));
                                ultag.setPack(Utils.bytesToHex(packReceived));

                                // Check if it already exists
                                ULTag dbUlTag = dbHelper.GetTagByUID(uid);
                                if (dbUlTag != null) {
                                    if (!dbUlTag.isEqualWith(ultag)) {
                                        dbHelper.UpdateTag(uid, Utils.bytesToHex(pass), Utils.bytesToHex(packReceived));
                                    }
                                }

                                byte[] readBytes = {};
                                byte[] tagBytes = {};

                                // Do the actual write by converting the hex data of the EditText to bytes
                                if (versionResp[6] == (byte) 0x0b) {
                                    // MF0UL11
                                    // user memory is 48 bytes (User pages 4-15)

                                    // Bytes from the EditText
                                    tagBytes = Utils.hexStringToByteArray(tagContentsView.getText().toString().replace("\n", "").substring(4 * 2 * 4, 16 * 2 * 4));

                                    if (tagBytes != null && tagBytes.length <= 48) {
                                        int j = 0;
                                        for (int i = 4; i < 16; i++) {
                                            byte[] bytesToWrite = Arrays.copyOfRange(tagBytes, j, j + 4);
                                            mFUL.writePage(i, bytesToWrite);
                                            j += 4;
                                        }

                                        // Verify written data with fast read (3Ah)
                                        readBytes = mFUL.transceive(new byte[]{0x3a, 0x04, 0x0f});
                                    }

                                } else if (versionResp[6] == (byte) 0x0e) {
                                    // MF0UL21
                                    // user memory is 128 bytes (User pages 4-35)

                                    // Bytes from the EditText
                                    tagBytes = Utils.hexStringToByteArray(tagContentsView.getText().toString().replace("\n", "").substring(4 * 2 * 4, 36 * 2 * 4));

                                    if (tagBytes != null && tagBytes.length <= 128) {
                                        int j = 0;
                                        for (int i = 4; i < 36; i++) {
                                            //Log.d(TAG, "Page " + i + " <- tagBytes[" + j + "] - tagBytes[" + (j + 3) + "]");
                                            byte[] bytesToWrite = Arrays.copyOfRange(tagBytes, j, j + 4);
                                            mFUL.writePage(i, bytesToWrite);
                                            j += 4;
                                        }

                                        // Verify written data with fast read (3Ah)
                                        readBytes = mFUL.transceive(new byte[]{0x3a, 0x04, 0x23});
                                    }
                                }

                                if (Arrays.equals(tagBytes, readBytes)) {
                                    // All good
                                    Toast.makeText(getApplicationContext(), getString(R.string.data_write_success), Toast.LENGTH_LONG).show();
                                } else {
                                    // Something wasn't written correctly
                                    Toast.makeText(getApplicationContext(), getString(R.string.data_verification_failed), Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.tag_connect_error_wrong_pass), Toast.LENGTH_LONG).show();
                    } finally {
                        if (mFUL != null) {
                            try {
                                mFUL.close();
                            } catch (Exception ex) {
                                //Log.e(TAG, ex.toString());
                            }
                        }
                    }

                }
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.read_tag_error), Toast.LENGTH_LONG).show();
        }
    }

    // Loads a dump file into the TextView
    private void loadDump() {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_dump)), 123);
    }

    // Saves the TextView contents to a file
    private void saveDump() {
        String tagContents = tagContentsView.getText().toString();

        if (!tagContents.isEmpty() && tagContents.length() >= 180) { // Minimum: 20 pages * (2 * 4 chars + 1 new line)

            // Try to get filename from the first eight bytes (the uid)
            String uid = (tagContents.substring(0, 6) + tagContents.substring(9, 17)).trim();
            uid = uid.replaceAll("[^a-zA-Z0-9]", "");

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.datetime_format));
            String filename = dateFormat.format(date);
            if (uid.length() > 0) {
                filename += "_" + uid;
            }

            //String txtFilename = filename + ".txt";
            String binFilename = filename + ".bin";

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
/*
                // Text file
                File txtFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), txtFilename);
                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(txtFile);
                    stream.write(tagContents.getBytes());
                    Toast.makeText(getApplicationContext(), "Data dumped successfully to: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + txtFilename, Toast.LENGTH_LONG).show();
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
 */

                // Bin file
                File binFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binFilename);
                FileOutputStream stream2 = null;
                try {
                    stream2 = new FileOutputStream(binFile);
                    stream2.write(Utils.hexStringToByteArray(tagContents.replace("\n", "")));
                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.data_dump_success), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), binFilename), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (stream2 != null) {
                            stream2.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_data_error_read_tag), Toast.LENGTH_LONG).show();
        }
    }

    // Verifies that the identified tag is an ULEV1 one
    private boolean verifyMFULEV1() {
        // Check if UL tag
        boolean isUL = false;
        boolean isULEV1 = false;

        // First check if MFUL
        for (String tech : tag.getTechList()) {
            if (tech.equals("android.nfc.tech.MifareUltralight")) {
                isUL = true;
            }
        }

        if (isUL) {
            MifareUltralight mFUL = MifareUltralight.get(tag);
            try {
                mFUL.connect();
                // send a GET_VERSION command
                byte[] resp = mFUL.transceive(new byte[]{0x60});
                if (resp[4] == (byte) 0x01) {
                    // Mifare Ultralight EV1 tag found
                    isULEV1 = true;
                }

            } catch (Exception e) {
                // Unable to connect with the Mifare Ultralight tag
            } finally {
                if (mFUL != null) {
                    try {
                        mFUL.close();
                    } catch (Exception ex) {
                        //Log.e(TAG, ex.toString());
                    }
                }
            }
        }

        return isULEV1;
    }

}