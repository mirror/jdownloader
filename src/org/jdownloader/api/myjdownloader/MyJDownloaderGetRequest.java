package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.CharSequenceInputStream;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.jdownloader.myjdownloader.client.json.JSonRequest;

public class MyJDownloaderGetRequest extends GetRequest implements MyJDownloaderRequestInterface {
    public static final String API_VERSION    = "apiVer";
    public static final String RID            = "rid";
    public static final String SIGNATURE      = "signature";
    public static final String CALLBACK       = "callback";
    public static final String DIFF_KEEPALIVE = "diffKA";
    public static final String DIFF_ID        = "diffID";

    // private static final String DIFF_TYPE = null;
    public static class GetData {
        public static final GetData EMPTY         = new GetData();
        public long                 rid           = -1;
        public int                  apiVersion    = -1;
        public String               signature     = null;
        public String               callback      = null;
        public long                 diffKeepalive = -1;
        public String               diffID        = null;
        public String               diffType      = null;
    }

    private JSonRequest jsonRequest = null;

    @Override
    public String toString() {
        if (jsonRequest == null) {
            return "Non JSonRequest\r\n" + super.toString();
        }
        return "RID: " + jsonRequest.getRid() + "\r\nAPIVersion: " + jsonRequest.getApiVer() + "\r\n" + super.toString();
    }

    public static GetData parseGetData(final List<KeyValuePair> requestedURLParameters) {
        final GetData ret = new GetData();
        if (requestedURLParameters != null) {
            for (final KeyValuePair param : requestedURLParameters) {
                if (param.key != null) {
                    /* key=value(parameter) */
                    if (MyJDownloaderGetRequest.CALLBACK.equalsIgnoreCase(param.key)) {
                        /* filter jquery callback */
                        ret.callback = param.value;
                        continue;
                    } else if (MyJDownloaderGetRequest.SIGNATURE.equalsIgnoreCase(param.key)) {
                        /* filter url signature */
                        ret.signature = param.value;
                        continue;
                    } else if (MyJDownloaderGetRequest.RID.equalsIgnoreCase(param.key)) {
                        ret.rid = Long.parseLong(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.API_VERSION.equalsIgnoreCase(param.key)) {
                        ret.apiVersion = Integer.parseInt(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.DIFF_KEEPALIVE.equalsIgnoreCase(param.key)) {
                        ret.diffKeepalive = Long.parseLong(param.value);
                        continue;
                    } else if (MyJDownloaderGetRequest.DIFF_ID.equalsIgnoreCase(param.key)) {
                        ret.diffID = param.value;
                        continue;
                    }
                    // else if (MyJDownloaderGetRequest.DIFF_TYPE.equalsIgnoreCase(param.key)) {
                    // ret.diffType = param.value;
                    // continue;
                    // }
                }
            }
        }
        return ret;
    }

    public MyJDownloaderGetRequest(HttpConnection connection) {
        super(connection);
    }

    public String getRequestConnectToken() {
        return getConnection().getRequestConnectToken();
    }

    @Override
    public MyJDownloaderHttpConnection getConnection() {
        return (MyJDownloaderHttpConnection) super.getConnection();
    }

    private GetData requestProperties = GetData.EMPTY;

    @Override
    public void setRequestedURLParameters(final List<KeyValuePair> requestedURLParameters) {
        super.setRequestedURLParameters(requestedURLParameters);
        requestProperties = MyJDownloaderGetRequest.parseGetData(requestedURLParameters);
        try {
            jsonRequest = parseJSonRequest(requestedURLParameters);
            if (jsonRequest != null) {
                final List<KeyValuePair> parameters = new ArrayList<KeyValuePair>();
                if (jsonRequest.getParams() != null) {
                    for (final Object param : jsonRequest.getParams()) {
                        if (param == null) {
                            parameters.add(new KeyValuePair("null"));
                        } else {
                            parameters.add(new KeyValuePair(JSonStorage.toString(param)));
                        }
                    }
                }
                super.setRequestedURLParameters(parameters);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected JSonRequest parseJSonRequest(final List<KeyValuePair> requestedURLParameters) throws IOException {
        if (requestedURLParameters != null && requestedURLParameters.size() >= 1 && "b64aesjson".equalsIgnoreCase(requestedURLParameters.get(0).key)) {
            try {
                final IvParameterSpec ivSpec = new IvParameterSpec(getConnection().getIv());
                final SecretKeySpec skeySpec = new SecretKeySpec(getConnection().getKey(), "AES");
                final Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                aesCipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                final String value = requestedURLParameters.get(0).value;
                final byte[] jsonBytes = IO.readStream(-1, new CipherInputStream(new Base64InputStream(new CharSequenceInputStream(value)), aesCipher));
                final String json = new String(jsonBytes, "UTF-8");
                return JSonStorage.restoreFromString(json, new TypeRef<JSonRequest>() {
                });
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    public int getApiVersion() {
        if (requestProperties.apiVersion >= 0) {
            return requestProperties.apiVersion;
        }
        try {
            final JSonRequest jsonr = getJsonRequest();
            if (jsonr != null) {
                return jsonr.getApiVer();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getRid() throws IOException {
        if (requestProperties.rid >= 0) {
            return requestProperties.rid;
        }
        final JSonRequest jsonr = getJsonRequest();
        if (jsonr != null) {
            return jsonr.getRid();
        }
        return -1;
    }

    @Override
    public String getSignature() {
        return requestProperties.signature;
    }

    @Override
    public String getJqueryCallback() {
        return requestProperties.callback;
    }

    @Override
    public long getDiffKeepAlive() throws IOException {
        if (requestProperties.diffKeepalive >= 0) {
            return requestProperties.diffKeepalive;
        }
        final JSonRequest jsonr = getJsonRequest();
        if (jsonr != null) {
            return jsonr.getDiffKA();
        }
        return 0;
    }

    @Override
    public String getDiffID() throws IOException {
        if (requestProperties.diffID != null) {
            return requestProperties.diffID;
        }
        final JSonRequest jsonr = getJsonRequest();
        if (jsonr != null) {
            return jsonr.getDiffID();
        }
        return null;
    }

    public JSonRequest getJsonRequest() throws IOException {
        return jsonRequest;
    }

    @Override
    public String getDiffType() throws IOException {
        if (requestProperties.diffType != null) {
            return requestProperties.diffType;
        }
        final JSonRequest jsonr = getJsonRequest();
        if (jsonr != null) {
            return jsonr.getDiffType();
        }
        return null;
    }
}
