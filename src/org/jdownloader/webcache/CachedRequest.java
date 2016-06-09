package org.jdownloader.webcache;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;

public class CachedRequest implements Storable {
    public static final TypeRef<CachedRequest> TYPE_REF = new TypeRef<CachedRequest>() {
        public java.lang.reflect.Type getType() {
            return CachedRequest.class;
        };
    };

    public CachedRequest(/* Storable */) {
    }

    private RequestMethod method;
    private String        key;

    public RequestMethod getMethod() {
        return method;
    }

    public void setMethod(RequestMethod method) {
        this.method = method;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public byte[] _getBytes() {
        return _bytes;
    }

    public void _setBytes(byte[] bytes) {
        this._bytes = bytes;
    }

    public ArrayList<CachedHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(ArrayList<CachedHeader> headers) {
        this.headers = headers;
    }

    public String getBytes64() {
        return Base64.encodeToString(_bytes, false);
    }

    public void setBytes64(String bytes64) {
        _bytes = Base64.decode(bytes64);
    }

    private String                  url;
    private byte[]                  _bytes;
    private ArrayList<CachedHeader> headers;
    private long                    maxAge;
    private long                    createTime;
    private int                     responseCode;
    private String                  responseMessage;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public CachedRequest(RequestMethod method, String lookupKey, String url, int responseCode, String responseMessage, byte[] bytes, ArrayList<CachedHeader> headers) {
        this.method = method;
        this.key = lookupKey;
        this.url = url;
        this._bytes = bytes;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.headers = headers;
        String expires = null;
        String cacheControl = null;
        String date = null;
        for (CachedHeader header : headers) {
            for (String v : header.getValues()) {
                if ("Cache-Control".equalsIgnoreCase(header.getKey())) {
                    cacheControl = v;
                    break;
                }
                if ("Expires".equalsIgnoreCase(header.getKey())) {
                    expires = v;
                    break;
                }
                if ("Date".equalsIgnoreCase(header.getKey())) {
                    date = v;
                    break;
                }
            }
        }
        this.createTime = System.currentTimeMillis();

        if (cacheControl != null) {
            String maxAge = new Regex(cacheControl, "max-age=(\\d+)").getMatch(0);
            if (maxAge != null) {
                this.maxAge = Integer.parseInt(maxAge) * 1000;
            }
        }
        if (maxAge < 0) {
            if (expires != null) {
                long now = System.currentTimeMillis();
                long expiresLong = TimeFormatter.parseDateString(expires).getTime();
                if (date != null) {
                    now = TimeFormatter.parseDateString(date).getTime();
                }
                maxAge = expiresLong - now;
                System.out.println("Max Age " + maxAge);
            }
        }
        if (!_isExpired()) {
            System.out.println("Write Cache " + url);
            System.out.println(JSonStorage.serializeToJson(this));
        }
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean _isExpired() {

        if (getMaxAge() >= 0 && getCreateTime() + getMaxAge() <= System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

}
