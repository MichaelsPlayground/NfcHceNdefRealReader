package de.androidcrypto.nfchcendefreader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView readResult;
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        contextSave = getApplicationContext();
        readResult = findViewById(R.id.tvMainReadResult);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        runOnUiThread(() -> {
            readResult.setText("");
        });

        IsoDep isoDep = null;
        writeToUiAppend(readResult, "Tag found");
        String[] techList = tag.getTechList();
        for (int i = 0; i < techList.length; i++) {
            writeToUiAppend(readResult, "TechList: " + techList[i]);
        }
        String tagId = Utils.bytesToHex(tag.getId());
        writeToUiAppend(readResult, "TagId: " + tagId);

        try {
            isoDep = IsoDep.get(tag);

            if (isoDep != null) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is IsoDep compatible",
                            Toast.LENGTH_SHORT).show();
                });

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                isoDep.connect();
                dumpExportString = "";
                runOnUiThread(() -> {
                    //readResult.setText("");
                });


                writeToUiAppend(readResult, "IsoDep reading");
                String nfcaContent = "IsoDep reading" + "\n";

                // now we run the select command with AID
                String nfcHecNdefAid = "F0394148148100";
                byte[] aid = Utils.hexStringToByteArray(nfcHecNdefAid);

                byte[] command = selectApdu(aid);
                byte[] responseSelect = isoDep.transceive(command);
                writeToUiAppend(readResult, "selectApdu with AID: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "selectApdu response: " + Utils.bytesToHex(responseSelect));

                if (responseSelect == null) {
                    writeToUiAppend(readResult, "selectApdu with AID fails (null)");
                } else {
                    writeToUiAppend(readResult, "responseSelect length: " + responseSelect.length + " data: " + Utils.bytesToHex(responseSelect));
                    System.out.println("responseSelect: " + Utils.bytesToHex(responseSelect));
                }

                if (!Utils.isSucceed(responseSelect)) {
                    writeToUiAppend(readResult, "responseSelect is not 90 00 - aborted");
                    System.out.println("responseSelect is not 90 00 - aborted ");
                    return;
                }

                // sending cc select = get the capability container
                String selectCapabilityContainer = "00a4000c02e103";
                command = Utils.hexStringToByteArray(selectCapabilityContainer);
                byte[] responseSelectCc = isoDep.transceive(command);
                writeToUiAppend(readResult, "select CC: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "select CC response: " + Utils.bytesToHex(responseSelectCc));
                writeToUiAppend(readResult, "responseSelect length: " + responseSelectCc.length + " data: " + Utils.bytesToHex(responseSelectCc));
                System.out.println("responseSelectCc: " + Utils.bytesToHex(responseSelectCc));

                if (!Utils.isSucceed(responseSelectCc)) {
                    writeToUiAppend(readResult, "responseSelectCc is not 90 00 - aborted");
                    System.out.println("responseSelectCc is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary from CC...
                String sendBinareFromCc = "00b000000f";
                command = Utils.hexStringToByteArray(sendBinareFromCc);
                byte[] responseSendBinaryFromCc = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryFromCc: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryFromCc response: " + Utils.bytesToHex(responseSendBinaryFromCc));
                writeToUiAppend(readResult, "sendBinaryFromCc response length: " + responseSendBinaryFromCc.length + " data: " + Utils.bytesToHex(responseSendBinaryFromCc));
                System.out.println("sendBinaryFromCc response: " + Utils.bytesToHex(responseSendBinaryFromCc));

                if (!Utils.isSucceed(responseSendBinaryFromCc)) {
                    writeToUiAppend(readResult, "responseSendBinaryFromCc is not 90 00 - aborted");
                    System.out.println("responseSendBinaryFromCc is not 90 00 - aborted ");
                    return;
                }

                // Capability Container header:
                byte[] capabilityContainerHeader = Arrays.copyOfRange(responseSendBinaryFromCc, 0, responseSendBinaryFromCc.length - 2);
                writeToUiAppend(readResult, "capabilityContainerHeader length: " + capabilityContainerHeader.length + " data: " + Utils.bytesToHex(capabilityContainerHeader));
                System.out.println("capabilityContainerHeader: " + Utils.bytesToHex(capabilityContainerHeader));
                System.out.println("capabilityContainerHeader: " + new String(capabilityContainerHeader));

                // Sending NDEF Select...
                String sendNdefSelect = "00a4000c02e104";
                command = Utils.hexStringToByteArray(sendNdefSelect);
                byte[] responseSendNdefSelect = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendNdefSelect: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendNdefSelect response: " + Utils.bytesToHex(responseSendNdefSelect));
                writeToUiAppend(readResult, "sendNdefSelect response length: " + responseSendNdefSelect.length + " data: " + Utils.bytesToHex(responseSendNdefSelect));
                System.out.println("sendNdefSelect response: " + Utils.bytesToHex(responseSendNdefSelect));

                if (!Utils.isSucceed(responseSendNdefSelect)) {
                    writeToUiAppend(readResult, "responseSendNdefSelect is not 90 00 - aborted");
                    System.out.println("responseSendNdefSelect is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary NLEN...
                String sendReadBinaryNlen = "00b0000002";
                command = Utils.hexStringToByteArray(sendReadBinaryNlen);
                byte[] responseSendBinaryNlen = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryNlen: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryNlen response: " + Utils.bytesToHex(responseSendBinaryNlen));
                writeToUiAppend(readResult, "sendBinaryNlen response length: " + responseSendBinaryNlen.length + " data: " + Utils.bytesToHex(responseSendBinaryNlen));
                System.out.println("sendBinaryNlen response: " + Utils.bytesToHex(responseSendBinaryNlen));

                if (!Utils.isSucceed(responseSendBinaryNlen)) {
                    writeToUiAppend(readResult, "responseSendBinaryNlen is not 90 00 - aborted");
                    System.out.println("responseSendBinaryNlen is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary, get NDEF data...
                String sendReadBinaryNdefData = "00b000000f";
                command = Utils.hexStringToByteArray(sendReadBinaryNdefData);
                byte[] responseSendBinaryNdefData = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryNdefData: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryNdefData response: " + Utils.bytesToHex(responseSendBinaryNdefData));
                writeToUiAppend(readResult, "sendBinaryNdefData response length: " + responseSendBinaryNdefData.length + " data: " + Utils.bytesToHex(responseSendBinaryNdefData));
                writeToUiAppend(readResult, "sendBinaryNdefData response: " + new String(responseSendBinaryNdefData));
                System.out.println("sendBinaryNdefData response: " + Utils.bytesToHex(responseSendBinaryNdefData));
                System.out.println("sendBinaryNdefData response: " + new String(responseSendBinaryNdefData));

                if (!Utils.isSucceed(responseSendBinaryNdefData)) {
                    writeToUiAppend(readResult, "responseSendBinaryNdefData is not 90 00 - aborted");
                    System.out.println("responseSendBinaryNdefData is not 90 00 - aborted ");
                    return;
                }

                byte[] ndefMessage = Arrays.copyOfRange(responseSendBinaryNdefData, 0, responseSendBinaryNdefData.length - 2);
                writeToUiAppend(readResult, "ndefMessage length: " + ndefMessage.length + " data: " + Utils.bytesToHex(ndefMessage));
                writeToUiAppend(readResult, "ndefMessage: " + new String(ndefMessage));
                System.out.println("ndefMessage: " + new String(ndefMessage));

                // strip off the first 2 bytes
                byte[] ndefMessageStrip = Arrays.copyOfRange(ndefMessage, 9, ndefMessage.length);

                //String ndefMessageParsed = Utils.parseTextrecordPayload(ndefMessageStrip);
                String ndefMessageParsed = new String(ndefMessageStrip);
                writeToUiAppend(readResult, "ndefMessage parsed: " + ndefMessageParsed);
                System.out.println("ndefMessage parsed: " + ndefMessageParsed);

                // try to get a NdefMessage from the byte array
                byte[] ndefMessageByteArray = Arrays.copyOfRange(ndefMessage, 2, ndefMessage.length);
                try {
                    NdefMessage ndefMessageFromTag = new NdefMessage(ndefMessageByteArray);
                    NdefRecord[] ndefRecords = ndefMessageFromTag.getRecords();
                    NdefRecord ndefRecord;
                    int ndefRecordsCount = ndefRecords.length;
                    if (ndefRecordsCount > 0) {
                        for (int i = 0; i < ndefRecordsCount; i++) {
                            short ndefTnf = ndefRecords[i].getTnf();
                            byte[] ndefType = ndefRecords[i].getType();
                            byte[] ndefPayload = ndefRecords[i].getPayload();
                            // here we are trying to parse the content
                            // Well known type - Text
                            if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(ndefType, NdefRecord.RTD_TEXT)) {
                                writeToUiAppend(readResult, "rec: " + i +
                                        " Well known Text payload\n" + new String(ndefPayload) + " \n");
                                writeToUiAppend(readResult, Utils.parseTextrecordPayload(ndefPayload));
                            }
                            // Well known type - Uri
                            if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(ndefType, NdefRecord.RTD_URI)) {
                                writeToUiAppend(readResult, "rec: " + i +
                                        " Well known Uri payload\n" + new String(ndefPayload) + " \n");
                                writeToUiAppend(readResult, Utils.parseUrirecordPayload(ndefPayload) + " \n");
                            }
                        }
                        dumpExportString = readResult.getText().toString();
                    }
                //dumpExportString = readResult.getText().toString();

                } catch (FormatException e) {
                    e.printStackTrace();
                }
            } else {
                writeToUiAppend(readResult, "IsoDep == null");
            }
        } catch (IOException e) {
            writeToUiAppend(readResult, "ERROR IOException: " + e);
            e.printStackTrace();
        }
    }

    // https://stackoverflow.com/a/51338700/8166854
    private byte[] selectApdu(byte[] aid) {
        byte[] commandApdu = new byte[6 + aid.length];
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xA4;  // INS
        commandApdu[2] = (byte) 0x04;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) (aid.length & 0x0FF);       // Lc
        System.arraycopy(aid, 0, commandApdu, 5, aid.length);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = textView.getText().toString() + "\n" + message;
            textView.setText(newString);
            dumpExportString = newString;
        });
    }

    private void writeToUiAppendReverse(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = message + "\n" + textView.getText().toString();
            textView.setText(newString);
        });
    }

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    private void exportDumpMail() {
        if (dumpExportString.isEmpty()) {
            writeToUiToast("Scan a tag first before sending emails :-)");
            return;
        }
        String subject = "Dump NFC-Tag " + tagTypeString + " UID: " + tagIdString;
        String body = dumpExportString;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void exportDumpFile() {
        if (dumpExportString.isEmpty()) {
            writeToUiToast("Scan a tag first before writing files :-)");
            return;
        }
        verifyPermissionsWriteString();
    }

    // section external storage permission check
    private void verifyPermissionsWriteString() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            writeStringToExternalSharedStorage();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void writeStringToExternalSharedStorage() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //boolean pickerInitialUri = false;
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // get filename from edittext
        String filename = tagTypeString + "_" + tagIdString + ".txt";
        // sanity check
        if (filename.equals("")) {
            writeToUiToast("scan a tag before writing the content to a file :-)");
            return;
        }
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        fileSaverActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileSaverActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                // get file content from edittext
                                String fileContent = dumpExportString;
                                writeTextToUri(uri, fileContent);
                                String message = "file written to external shared storage: " + uri.toString();
                                writeToUiToast("file written to external shared storage: " + uri.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                                writeToUiToast("ERROR: " + e.toString());
                                return;
                            }
                        }
                    }
                }
            });

    private void writeTextToUri(Uri uri, String data) throws IOException {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(contextSave.getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            System.out.println("Exception File write failed: " + e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mExportMail = menu.findItem(R.id.action_export_mail);
        mExportMail.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                //startActivity(i);
                exportDumpMail();
                return false;
            }
        });

        MenuItem mExportFile = menu.findItem(R.id.action_export_file);
        mExportFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                //startActivity(i);
                exportDumpFile();
                return false;
            }
        });

        MenuItem mClearDump = menu.findItem(R.id.action_clear_dump);
        mClearDump.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                dumpExportString = "";
                readResult.setText("read result");
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}