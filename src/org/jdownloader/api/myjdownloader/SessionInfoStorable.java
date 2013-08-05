package org.jdownloader.api.myjdownloader;

import org.appwork.storage.Storable;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.myjdownloader.client.SessionInfo;

public class SessionInfoStorable implements Storable {

    private String deviceSecret = null;

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public String getDeviceEncryptionToken() {
        return deviceEncryptionToken;
    }

    public void setDeviceEncryptionToken(String deviceEncryptionToken) {
        this.deviceEncryptionToken = deviceEncryptionToken;
    }

    public String getServerEncryptionToken() {
        return serverEncryptionToken;
    }

    public void setServerEncryptionToken(String serverEncryptionToken) {
        this.serverEncryptionToken = serverEncryptionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getRegainToken() {
        return regainToken;
    }

    public void setRegainToken(String regainToken) {
        this.regainToken = regainToken;
    }

    private String deviceEncryptionToken = null;
    private String serverEncryptionToken = null;
    private String sessionToken          = null;
    private String regainToken           = null;

    public SessionInfoStorable(/* Storable */) {

    }

    public SessionInfoStorable(SessionInfo info) {
        this.regainToken = info.getRegainToken();
        this.sessionToken = info.getSessionToken();
        this.deviceEncryptionToken = HexFormatter.byteArrayToHex(info.getDeviceEncryptionToken());
        this.serverEncryptionToken = HexFormatter.byteArrayToHex(info.getServerEncryptionToken());
        this.deviceSecret = HexFormatter.byteArrayToHex(info.getDeviceSecret());
    }

    public SessionInfo _getSessionInfo() {
        return new SessionInfo(HexFormatter.hexToByteArray(getDeviceSecret()), HexFormatter.hexToByteArray(getServerEncryptionToken()), HexFormatter.hexToByteArray(getDeviceEncryptionToken()), getSessionToken(), getRegainToken());
    }

}
