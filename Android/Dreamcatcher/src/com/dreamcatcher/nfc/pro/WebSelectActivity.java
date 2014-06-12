/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class WebSelectActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent dataIntent = getIntent();
        String shareText = dataIntent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d("sam test", "shareText =" + shareText);

        if (shareText != null) {
            Intent intent = new Intent(this, WriteTag.class);
            intent.putExtra(WriteTag.WRITE_TYPE, WriteTag.NFC_WRITE_URL_TYPE);
            intent.putExtra(WriteTag.WEB_URL_ADDRESS, shareText);
            startActivity(intent);
            finish();
        }
    }
}
