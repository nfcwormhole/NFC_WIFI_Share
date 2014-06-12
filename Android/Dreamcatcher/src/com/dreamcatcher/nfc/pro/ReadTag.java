/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class ReadTag extends Activity {

    private static final int OK = 100;

    @Override
    protected void onResume() {

        Log.d("sam test", "getIntent().getAction() =" + getIntent().getAction());
        super.onResume();
        // ACTION_NDEF_DISCOVERED
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
        finish();
    }

    void processIntent(Intent intent) {
        NdefMessage[] msgs = null;
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage)rawMsgs[i];
            }
        }

        byte[] records1 = msgs[0].getRecords()[1].getPayload();
        final NfcWifiAction nfcWifiAction = NfcParseUtil.parseData(records1);
        Log.d("sam test", "nfcWifiAction: " + nfcWifiAction);
        if (nfcWifiAction != null) {

            new AsyncTask<Void, Void, Integer>() {

                @Override
                protected Integer doInBackground(Void... params) {
                    final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    WifiConnect connect = new WifiConnect(wifiManager);
                    connect.Connect(nfcWifiAction.getSsid(), nfcWifiAction.getPassword(),
                            nfcWifiAction.getCipherType());
                    return OK;
                }

                @Override
                protected void onPostExecute(Integer result) {
                    if (result == OK) {
                        Toast.makeText(getApplicationContext(), R.string.noti_setting_success,
                                Toast.LENGTH_LONG).show();

                        Notification mNotification = new Notification(R.drawable.ic_launcher,
                                getString(R.string.noti_setting_title), System.currentTimeMillis());
                        mNotification.defaults |= Notification.DEFAULT_VIBRATE;
                        Intent intent = new Intent(ReadTag.this, ReadTag.class);
                        PendingIntent pendIntent = PendingIntent.getActivity(ReadTag.this, 0,
                                intent, 0);
                        mNotification.setLatestEventInfo(ReadTag.this,
                                getString(R.string.noti_setting_title),
                                getString(R.string.noti_wifi_setting_title), pendIntent);

                        NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                        // mId allows you to update the notification later on.
                        mNotificationManager.notify(Util.WIFI_SETTING_NOTIFICATION, mNotification);

                    }
                }

            }.execute((Void)null);

        } else {
            Toast.makeText(getApplicationContext(), R.string.noti_parse_wifi_fail,
                    Toast.LENGTH_LONG).show();
        }
    }
}
