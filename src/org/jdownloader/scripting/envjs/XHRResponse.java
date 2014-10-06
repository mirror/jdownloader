package org.jdownloader.scripting.envjs;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class XHRResponse implements Storable {

    public XHRResponse(/* Storable */) {
    }

    public HashMap<String, String> responseHeader = new HashMap<String, String>();
    public int                     responseCode   = -1;
    private String                 reponseMessage = null;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getReponseMessage() {
        return reponseMessage;
    }

    public void setReponseMessage(String reponseMessage) {
        this.reponseMessage = reponseMessage;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    private String encoding     = null;
    private String responseText = null;

    public HashMap<String, String> getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(HashMap<String, String> responseHeader) {
        this.responseHeader = responseHeader;
    };
}