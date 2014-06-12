/** Copyright 2014 sam, xiao_nie@163.com  
 *  More info : http://www.elecfreaks.com 
 */

package com.dreamcatcher.nfc.pro;

public class NfcWifiAction {
    private WifiCipherType cipherType = WifiCipherType.NONE;

    private String ssid; // wifi ssid

    private String password; // pwd

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public WifiCipherType getCipherType() {
        return cipherType;
    }

    public void setCipherType(WifiCipherType cipherType) {
        this.cipherType = cipherType;
    }

    @Override
    public String toString() {
        return "SSID = " + ssid + ",type = " + cipherType + ",pwd = " + password;
    }
}
