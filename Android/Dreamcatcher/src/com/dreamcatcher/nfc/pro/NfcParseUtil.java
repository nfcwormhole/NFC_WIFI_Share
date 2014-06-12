/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class NfcParseUtil {

    public static final String ECODE = "utf-8";

    public static NfcWifiAction parseData(byte[] records1) {
        byte[] tempFlag = {
                0x0a, 0x1f, 0x1d
        };

        try {
            String strRecords1 = new String(records1, ECODE);
            System.out.println("s = " + strRecords1);
            String[] datas = strRecords1.split(new String(tempFlag, ECODE)); // 0x0a
                                                                             // 0x1f
                                                                             // 0x1d
            System.out.println("len =" + datas.length);

            if (datas.length == 2) {
                return parseDataAction(datas[1]);
                // parse 0x0a 0x1f 0x1d second data
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;

    }

    private static NfcWifiAction parseDataAction(String data) throws UnsupportedEncodingException {
        NfcWifiAction nfcWifiAction = null;
        if (data != null) {
            byte[] splitdata = data.getBytes(ECODE);

            int startIndex = 0;
            // 0x0a 0x1f 0x1d 04 :wifi
            if (splitdata[startIndex] == 0x04) {
                startIndex++;
                nfcWifiAction = new NfcWifiAction();
                // 0x30:wifi
                if (splitdata[startIndex] == 0x30) {

                    int srcPos = startIndex + 2;
                    int descLen = splitdata.length - srcPos;
                    byte[] tmpDest = new byte[descLen];
                    System.arraycopy(splitdata, srcPos, tmpDest, 0, descLen);

                    // 00
                    String strContent = new String(tmpDest, ECODE);
                    String[] contentDatas = strContent.split(new String(new byte[] {
                        0x00
                    }, ECODE));
                    // index = 0 ssid
                    // index = 1 chphertype
                    // index = 2 pwd
                    if (contentDatas.length == 3) {
                        nfcWifiAction.setSsid(contentDatas[0]);
                        nfcWifiAction.setCipherType(WifiCipherType.values()[Integer
                                .parseInt(contentDatas[1])]);
                        nfcWifiAction.setPassword(contentDatas[2]);
                    } else if (contentDatas.length == 2) {
                        // no pwd
                        nfcWifiAction.setSsid(contentDatas[0]);
                        nfcWifiAction.setCipherType(WifiCipherType.values()[Integer
                                .parseInt(contentDatas[1])]);
                    }
                } else {
                    Log.d("sam test", "parse wifi action error");
                }
            } else {
                Log.d("sam test", "parse nfc action error");
            }
        }
        return nfcWifiAction;

    }

}
