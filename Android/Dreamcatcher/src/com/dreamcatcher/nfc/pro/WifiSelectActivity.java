/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

public class WifiSelectActivity extends Activity {

    private WifiManager wifiManager;

    private boolean wifiOpening = false;

    List<ScanResult> list;

    EditText wifiSsidName;

    Spinner wifiSafeSpinner;

    EditText wifiPassword;

    LinearLayout panelWifiPwd;

    private int wifiSafe = 0; // 0=no pwd 1=WPA 2=WEP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_wifi);
        init();
    }

    private void init() {

        wifiSsidName = (EditText)findViewById(R.id.wifi_ssid_edit);
        ImageView selectSsid = (ImageView)findViewById(R.id.wifi_select_ssid_im);
        selectSsid.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (wifiOpening) {
                    Toast.makeText(WifiSelectActivity.this, R.string.noti_wifi_open_wait,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                scanWifiResults();

            }
        });
        wifiSafeSpinner = (Spinner)findViewById(R.id.wifi_select_safe);

        ArrayAdapter wifiSafeAdapter = ArrayAdapter.createFromResource(this,
                R.array.wifi_select_safe_array, android.R.layout.simple_spinner_item);
        // new
        // ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,R.array.wifi_select_safe_array);

        wifiSafeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wifiSafeSpinner.setAdapter(wifiSafeAdapter);
        wifiSafeSpinner.setSelection(1);
        wifiSafeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long arg3) {
                Log.d("sam test", "select index = " + index);
                wifiSafe = index;
                if (index == 0)
                    panelWifiPwd.setVisibility(View.GONE);
                else
                    panelWifiPwd.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });
        panelWifiPwd = (LinearLayout)findViewById(R.id.panel_wifi_pwd);
        wifiPassword = (EditText)findViewById(R.id.wifi_password);
        Button writeTag = (Button)findViewById(R.id.bnt_write_wifi_tag);
        writeTag.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WifiSelectActivity.this, WriteTag.class);
                intent.putExtra(WriteTag.WRITE_TYPE, WriteTag.NFC_WRITE_WIFI_TYPE);
                intent.putExtra(WriteTag.WIFI_SSID, wifiSsidName.getText().toString());
                intent.putExtra(WriteTag.WIFI_SAFE, wifiSafe);
                intent.putExtra(WriteTag.WIFI_PASSWORD, wifiPassword.getText().toString());
                startActivity(intent);
            }
        });

        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        openWifi();

    }

    private void scanWifiResults() {
        list = wifiManager.getScanResults();

        if (list != null && list.size() > 0) {
            Log.d("sam test", "scanResults = " + list.size());
            final String[] scanResults = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                scanResults[i] = list.get(i).SSID.replace("\"", "");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(WifiSelectActivity.this);
            builder.setTitle(R.string.wifi_title);
            builder.setItems(scanResults, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String ssid = scanResults[item];
                    Log.d("sam test", "select ssid = " + ssid);
                    wifiSsidName.setText(ssid);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    Runnable wifiOpenSuccess = new Runnable() {

        @Override
        public void run() {
            Toast.makeText(WifiSelectActivity.this, R.string.noti_wifi_opened, Toast.LENGTH_LONG)
                    .show();
        }
    };

    private void openWifi() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.noti_wifi_close_wait, Toast.LENGTH_LONG).show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    wifiOpening = true;
                    wifiManager.setWifiEnabled(true);
                    while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                        try {
                            Thread.currentThread();
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                        }
                    }
                    runOnUiThread(wifiOpenSuccess);
                    wifiOpening = false;
                }
            }).start();
        }

    }
}
