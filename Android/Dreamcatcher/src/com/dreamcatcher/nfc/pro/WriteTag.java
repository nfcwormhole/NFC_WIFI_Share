/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class WriteTag extends Activity {
    public static final String WRITE_TYPE = "WRITE_TYPE";

    public static final int NFC_WRITE_WIFI_TYPE = 0;

    public static final int NFC_WRITE_URL_TYPE = 1;

    // wifi share
    public static final String WIFI_SSID = "WIFI_SSID";

    public static final String WIFI_SAFE = "WIFI_SAFE";

    public static final String WIFI_PASSWORD = "WIFI_PASSWORD";

    // URL share
    public static final String WEB_URL_ADDRESS = "WEB_URL_ADDRESS";

    NfcAdapter nfcAdapter;

    private boolean WriteMode = false;

    int nfcWriteType = -1; // 0 =wifi,1=url

    String wifissid;

    int wifiSafe;

    String wifiPassword;

    String webUrlAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_tag);
        initData();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void initData() {
        Intent dataIntent = getIntent();
        nfcWriteType = dataIntent.getIntExtra(WRITE_TYPE, -1);
        if (nfcWriteType == -1)
            finish();

        wifissid = dataIntent.getStringExtra(WIFI_SSID);
        wifiSafe = dataIntent.getIntExtra(WIFI_SAFE, 0);
        wifiPassword = dataIntent.getStringExtra(WIFI_PASSWORD);
        webUrlAddress = dataIntent.getStringExtra(WEB_URL_ADDRESS);

        Log.d("sam test", "write tag data [nfcWriteType = " + nfcWriteType + ",webUrlAddress = "
                + webUrlAddress + ", ssid = " + wifissid + ",safe = " + wifiSafe + ",pwd = "
                + wifiPassword + "]");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d("sam test", "WriteTag onNewIntent action = " + intent.getAction());
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (writeTag(createNdefMessage(), detectedTag)) {
                Toast.makeText(getApplicationContext(), R.string.noti_wifi_write_success,
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), R.string.noti_wifi_write_fail,
                        Toast.LENGTH_LONG).show();
            }

        }
    }

    public NdefMessage createNdefMessage() {

        if (nfcWriteType == NFC_WRITE_WIFI_TYPE) {
            NdefRecord[] records = new NdefRecord[2];
            // URI
            records[0] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI,
                    "0".getBytes(), createURI(Util.APP_URL));
            // TXT
            records[1] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                    "1".getBytes(), createText()); // createTxt("msd".getBytes())

            NdefMessage message = new NdefMessage(records);
            return message;
        }

        else if (nfcWriteType == NFC_WRITE_URL_TYPE) {
            NdefRecord[] records = new NdefRecord[1];

            records[0] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI,
                    "0".getBytes(), createURI(webUrlAddress));

            NdefMessage message = new NdefMessage(records);
            return message;
        }

        return null;
    }

    private byte[] createURI(String url) {
        byte[] uris = url.getBytes();
        byte[] uristmp = new byte[uris.length + 1];
        uristmp[0] = 0x00;
        System.arraycopy(uris, 0, uristmp, 1, uris.length);
        return uristmp;
    }

    private byte[] createText() {
        byte[] header = {
                0x02, 0x0a, 0x1f, 0x1d, 0x04, 0x30
        };
        byte split = 0x00;
        byte[] texts = null;
        try {
            byte[] ssid = wifissid.getBytes(NfcParseUtil.ECODE);
            byte[] password = wifiPassword.getBytes(NfcParseUtil.ECODE);
            byte[] safe = String.valueOf(wifiSafe).getBytes(NfcParseUtil.ECODE);

            int len = header.length + ssid.length + safe.length + password.length + 3;
            Log.d("sam test", "len = " + len);
            texts = new byte[len];

            System.arraycopy(header, 0, texts, 0, header.length);
            int startIndex = header.length;
            texts[startIndex] = split;

            startIndex++;
            System.arraycopy(ssid, 0, texts, startIndex, ssid.length);
            startIndex = startIndex + ssid.length;
            texts[startIndex] = split;

            startIndex++;
            System.arraycopy(safe, 0, texts, startIndex, safe.length);
            startIndex = startIndex + safe.length;
            texts[startIndex] = split;

            startIndex++;
            System.arraycopy(password, 0, texts, startIndex, password.length);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return texts;
    }

    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("sam test", "nfc WriteMode = " + WriteMode);
        enableTagWriteMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ((isFinishing()) && (nfcAdapter != null)) {
            WriteMode = false;
            Log.d("sam test", "nfc disableForegroundDispatch");
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    private void enableTagWriteMode() {
        if (!WriteMode && nfcAdapter != null && nfcAdapter.isEnabled()) {
            WriteMode = true;
            Log.d("sam test", "nfc enableForegroundDispatch");
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] mWriteTagFilters = new IntentFilter[] {
                tagDetected
            };
            // Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
            PendingIntent mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                    getClass()).addFlags(603979776), 0);
            nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
        }
    }
}
