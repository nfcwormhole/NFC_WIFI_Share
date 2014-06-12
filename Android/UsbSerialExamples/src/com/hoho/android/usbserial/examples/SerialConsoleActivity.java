/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialDriver} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialDriver)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialDriver sDriver = null;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            } catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sDriver.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    byte[] buffs = new byte[16384];
    int buffIndex = 0;
    boolean isStartFF = false;
    boolean isEndFF = false;
    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
        
        synchronized (buffs) 
        {
            int  i = 0;
            while(i < data.length)
            {
               switch(data[i])
               {
                   case (byte)0xFF:
                       if(isStartFF)
                       {
                         Log.d(TAG, "read end FF = 0x" + Integer.toHexString(data[i]));
                         isEndFF = true;
                         isStartFF = false;
                       }
                       else
                       {
                         Log.d(TAG, "read begin FF = 0x" + Integer.toHexString(data[i]));
                         isStartFF = true;
                       }
                   break;
                      
               }
              
               if(isStartFF)
               {
                    if(i < data.length && data[i] != (byte)0xFF)
                    {
                        buffs[buffIndex++] =  data[i];  
                        Log.d("sam test", "buffer data " + Integer.toHexString(data[i]));
                    }
                    
               }
               i++;
            }
    
            
            if(isEndFF)
            {
                isEndFF = false;
                //buffIndex = 0;
          
                System.out.println("buffIndex = " +buffIndex);
                parseData(buffs,buffIndex);
                buffIndex = 0;
            }
        }
    }
    

    private void parseData(byte[] buffer,int buffIndex)
    {
        int ret = buffIndex;
       
        int i;      
//        while (ret >= 0) {
//            
//            Log.d(TAG, "receive length = 0x" + Integer.toHexString(ret));
            i = 0;
            while (i < ret) {
                int len = ret - i;//减去已经识别的
                Log.d(TAG, "left len = 0x" + Integer.toHexString(len));
                //for (int k=0; k<len; k++)
                  //Log.d(TAG, "RF430buffer[" + Integer.toHexString(k) + "]" + "=" + Integer.toHexString(buffer[k]));
                switch (buffer[i]) 
                {
                case (byte)0xE1:
                    Log.d(TAG, "Type 2 Tag!!");
                    if (len >= (buffer[i + 1]+2)) 
                    {
                        for (int k=0; k < (buffer[i + 1]+2); k++)
                            Log.d(TAG, "0x03 buffer[" + k + "]" + "=" + Integer.toHexString(buffer[k]));
                        //sendIntend(buffer,len+2);
                    }
                    i += (buffer[i + 1]+2);
                    break;
                case (byte)0xD2:
                    Log.d(TAG, "Type 4 Tag!!");
                
                    //byte buffer[1024];
                    int NDEFMessageLength = 0;
                    int type4tagLength = 0;
                    
                    /* get the NDEF Message Length */
                    //nfc.Read_Continuous(0, buffer, (uint16_t)28);
                    NDEFMessageLength = buffer[26] << 8 | buffer[27];
                    //Serial.print("NDEFMessageLength = 0x");Serial.println(NDEFMessageLength, HEX);
                    Log.d(TAG, "NDEFMessageLength = 0x" + Integer.toHexString(NDEFMessageLength));    
                    /* entire Tag length */
                    type4tagLength = NDEFMessageLength + 28;
                    //Serial.print("Tag4Length = 0x");Serial.println(Tag4Length, HEX);
                    Log.d(TAG, "type4tagLength = 0x" + Integer.toHexString(type4tagLength));    

                    if (len >= type4tagLength) 
                    {
                        for (int k=0; k < type4tagLength; k++)
                            Log.d(TAG, "type4tag[0x" + Integer.toHexString(k) + "]" + "=" + Integer.toHexString(buffer[k]));
                            sendIntend(buffer,type4tagLength);
                    }
                    i += type4tagLength;
                    break;                  
                default:
                    Log.d(TAG, "unknown msg: " + Integer.toHexString(buffer[i]));
                    i = len;
                    break;
                }
            }
//        }
//        Log.d(TAG, "sam thread end");
    }

    
    private void sendIntend(byte[] tagMessage, int tagMessageLength) {
        byte ndefRecordCommonSet = 0;
        short tnf = 0x01;
        byte ndefTypeLength = 0;
        int ndefPayloadLength = 0;
        int ndefIdLength = 0;
        //byte ndefType = 0;
        
        byte recordsLength = 0;
        
        
        NdefRecord[] recordbuff = new NdefRecord[256];
        
        int tagMessageIndex = 0;
        int tagPayloadIndex = 0;
        byte bit3 = 0x1<<3;
        byte bit6 = 0x1<<6;
        
        /** 分解NFC Tag的数据，解析payload **/
        switch (tagMessage[0])
        {   
            case (byte)0xD2:
            {               
               /**  Type4Tag 解析 **/
               tagMessageIndex += 0x1A;
               //ndefPayloadLength = tagMessage[0x1A] << 8 | tagMessage[0x1B];
               tagMessageIndex += 0x02;
               
               do{
                   tagPayloadIndex = 0;
                   //COM
                   ndefRecordCommonSet = tagMessage[tagMessageIndex++];
                   tnf = (short)(ndefRecordCommonSet & 0x07);
                   //TYPE_LENGTH
                   ndefTypeLength = tagMessage[tagMessageIndex++];
                   //Payload_Length
                   ndefPayloadLength = tagMessage[tagMessageIndex++];
                   //Id_length when IL=1
                   if ((ndefRecordCommonSet & bit3) == bit3)
                       ndefIdLength = tagMessage[tagMessageIndex++];  
                   //Type数组
                   byte[] ndefType = new byte[ndefTypeLength];
                   for (int i=0; i < ndefTypeLength; i++)                      
                       ndefType[i] = tagMessage[tagMessageIndex++];  
                   //Id数组 when ndefIdLength != 0
                   byte[] ndefId = new byte[ndefIdLength];
                   if (ndefIdLength != 0)
                       for (int i=0; i < ndefIdLength; i++)
                           ndefId[i] = tagMessage[tagMessageIndex++];                                         
                                     
                   /* 获取payload */
                   byte[] ndefPayload = new byte[ndefPayloadLength];
                   for(int i=0; i < (ndefPayloadLength); i++) 
                      ndefPayload[tagPayloadIndex++] = tagMessage[tagMessageIndex++];
                                              
                   /* 组装 record */
                   recordbuff[recordsLength] = new NdefRecord(tnf, ndefType, ndefId, ndefPayload );
                   
                   recordsLength++;//records下标加1
               }while((ndefRecordCommonSet & bit6) != bit6);         

               
               break;
            }
            case (byte)0xE1:
            {               
                /** Type2Tag解析 **/
                tagMessageIndex++;
                tagMessageIndex++;
                
                do{
                    tagPayloadIndex = 0;
                    //COM
                    ndefRecordCommonSet = tagMessage[tagMessageIndex++];
                    tnf = (short)(ndefRecordCommonSet & 0x07);
                    //TYPE_LENGTH
                    ndefTypeLength = tagMessage[tagMessageIndex++];
                    //Payload_Length
                    ndefPayloadLength = tagMessage[tagMessageIndex++];
                    //Id_length when IL=1
                    if ((ndefRecordCommonSet & bit3) == bit3)
                        ndefIdLength = tagMessage[tagMessageIndex++];  
                    //Type数组
                    byte[] ndefType = new byte[ndefTypeLength];
                    for (int i=0; i < ndefTypeLength; i++)                      
                        ndefType[i] = tagMessage[tagMessageIndex++];  
                    //Id数组 when ndefIdLength != 0
                    byte[] ndefId = new byte[ndefIdLength];
                    if (ndefIdLength != 0)
                        for (int i=0; i < ndefIdLength; i++)
                            ndefId[i] = tagMessage[tagMessageIndex++];                                         
                                      
                    /* 获取payload */
                    byte[] ndefPayload = new byte[ndefPayloadLength];
                    for(int i=0; i < (ndefPayloadLength); i++) 
                       ndefPayload[tagPayloadIndex++] = tagMessage[tagMessageIndex++];
                                               
                    /* 组装 record */
                    recordbuff[recordsLength] = new NdefRecord(tnf, ndefType, ndefId, ndefPayload );
                    
                    recordsLength++;//records下标加1
                }while((ndefRecordCommonSet & bit6) != bit6);   
                
                break;
            }
            default:
                break;
        }
        
        /** 组成record数组 **/
        NdefRecord[] records = new NdefRecord[recordsLength];
        for (int i=0; i < recordsLength; i++)
            records[i] = recordbuff[i];

        /** 组装NDEFMessage **/
        NdefMessage message= new NdefMessage(records);
        //Log.d(TAG, "message" + message);
        
        /** 组装tag **/
        Bundle extras = new Bundle();
        extras.putParcelable(Ndef.EXTRA_NDEF_MSG, message);
        extras.putInt(Ndef.EXTRA_NDEF_MAXLENGTH, 0x4D);
        extras.putInt(Ndef.EXTRA_NDEF_CARDSTATE, Ndef.NDEF_MODE_READ_WRITE);
        extras.putInt(Ndef.EXTRA_NDEF_TYPE, Ndef.TYPE_2);           
        byte[] tag_UID = {0x04, (byte)0xA5, 0x2B, (byte)0x9A, 0x43, 0x2F, (byte)0x80};
        int[] techlist = {TagTechnology.MIFARE_ULTRALIGHT, TagTechnology.NFC_A, TagTechnology.NDEF};
        
        Tag tag = Tag.createMockTag(
                tag_UID,                    //new byte[] { 0x00 },
                techlist,                   //new int[] { TagTechnology.NDEF },
                new Bundle[] { extras });
        
                 
        /** 组装intent **/
        Intent intent= new Intent(); 
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[]{message});
        intent.putExtra(NfcAdapter.EXTRA_TAG, tag);                          
        intent.putExtra(NfcAdapter.EXTRA_ID, tag.getId());
        
        
        //设置整个Message的类型，for intent-filter
        final Uri ndefUri;
        final String ndefMimeType;
        ndefUri = message.getRecords()[0].toUri();
        ndefMimeType = message.getRecords()[0].toMimeType();  
        
        //设置intent的action类型，ndefUri or ndefMimeType
        intent.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        if (ndefUri != null) {
            intent.setData(ndefUri);                
        } else if (ndefMimeType != null) {
            intent.setType(ndefMimeType);
        }
    
        /** 转发intent **/
        startActivity(intent);
        
//      handler.post(new Runnable(){  
//          public void run(){  
//              Toast.makeText(getApplicationContext(), "show show show!", Toast.LENGTH_SHORT).show();  
//          }  
//      });  
    
        /** 打印Intent的内容 **/                     
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        Log.d(TAG, "dispatch tag: " + tag.toString() + " message: " + msg);
    }
    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver) {
        sDriver = driver;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

}
