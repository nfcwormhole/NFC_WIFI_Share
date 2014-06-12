package com.hoho.android.usbserial.examples;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleSerialService extends Service {

    
    private final String TAG = SingleSerialService.class.getSimpleName();
    private final int RUNONUITHREAD_MSG = 101;
    private static UsbSerialDriver sDriver = null;
   
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler()
    {
        public void handleMessage(android.os.Message msg) 
        {
            if(msg.what == RUNONUITHREAD_MSG)
            {
                byte[] data = (byte[])msg.obj;
                updateReceivedData(data);
            }
        };
    };
    
    public static void setUsbSerialDriver(UsbSerialDriver target)
    {
        if(sDriver != null)
        {
            try {
                sDriver.close();
            } catch (IOException e) {
                // Ignore.
                e.printStackTrace();
            }
        }
        sDriver = target;
    }
    
    byte[] bs = {
            /* NDEF Tag Application */

            (byte) 0xD2, 0x76, 0x00, 0x00, (byte) 0x85, 0x01, 0x01,

            (byte) 0xE1, 0x03, /* Capability Container ID */

            /* CC file start */
            0x00, 0x0F, /* CCLEN 15bytes fix */
            0x20, /* Mapping version 2.0 */
            0x00, 0x3B, /* MLe (59 bytes); Maximum R-APDU data size */
            0x00, 0x34, /* MLc (52 bytes); Maximum C-APDU data size */
            0x04, /* Tag, File Control TLV (4 = NDEF file) */
            0x06, /* Length, File Control TLV (6 = 6 bytes of data for this tag) */
            (byte) 0xE1, 0x04, /* Type4 Tag File Identifier */
            0x0B, (byte) 0xDF, /*
                                * Max NDEF size (3037 bytes of RF430CL330
                                * useable memory)
                                */
            0x00, /*
                   * NDEF file read access condition, read access without any
                   * security
                   */
            0x00, /*
                   * NDEF file write access condition; write access without any
                   * security
                   */
            /* CC file end */

            (byte) 0xE1, 0x04, /* NDEF File ID */

            0x00, 0x0F, /* NDEF Length 15bytes */

            /* NDEF start */
            (byte) 0xD1, /* NDEF Header MB=1, ME=1, CF=0, SR=1, IL=0, TNF=1 */

            0x01, /* Type Length 1 byte */

            0x0B, /* Payload length 11bytes */

            0x55, /* Type U (URI) */
            /* Payload start */

            0x01, /* URI Record Type : http://www. */
            /* Appliction Data : douban.com */
            0x64, 0x6F, 0x75, 0x62, 0x61, 0x6E, 0x2E, 0x63,
            0x6F, 0x6D,
    };
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() 
    {
        Log.d(TAG, "oncreate ...");
        super.onCreate();
       
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
        Log.d(TAG, "onStartCommand ... sDriver = " +sDriver );
        
        //sam add
        /*if(1 == intent.getIntExtra("mode", 0))
        {
            if(sDriver!= null)
            {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1,
                        UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                return super.onStartCommand(intent, flags, startId);
            }

            onDeviceStateChange(sDriver);
            mSerialIoManager.writeAsync(bs);
            }
        }*/
        //sam end
         if(sDriver != null)
        {

            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1,
                        UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                return super.onStartCommand(intent, flags, startId);
            }

            onDeviceStateChange(sDriver);
     
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    private void onDeviceStateChange(UsbSerialDriver serialDriver) {
        stopIoManager();
        startIoManager(serialDriver);
    }
    
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }
    
    private void startIoManager(UsbSerialDriver serialDriver) {
        if (serialDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(serialDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }
    
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
//            SerialService.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    updateReceivedData(data);
//                }
//            });
            Log.d(TAG, "onNewData.");
           Message message = handler.obtainMessage();
           message.what = RUNONUITHREAD_MSG;
           message.obj = data;
           handler.sendMessage(message);
        }
    };
    
    byte[] buffs = new byte[16384];
    int buffIndex = 0;
    boolean isStartFF = false;
    boolean isEndFF = false;
    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        
        Log.d(TAG, message);
        
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
    
        try
        {
        /** 转发intent **/
        startActivity(intent);
        }catch (Exception e) {
           e.printStackTrace();
        }
        
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
}
