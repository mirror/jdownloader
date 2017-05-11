package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.HexInputStream;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderGetRequest.GetData;
import org.jdownloader.myjdownloader.client.json.JSonRequest;

public class MyJDownloaderPostRequest extends PostRequest implements MyJDownloaderRequestInterface {
    private JSonRequest jsonRequest = null;

    public MyJDownloaderPostRequest(MyJDownloaderHttpConnection myJDownloaderHttpConnection) {
        super(myJDownloaderHttpConnection);
    }

    @Override
    public String toString() {
        if (jsonRequest == null) {
            return "Non JSonRequest\r\n" + super.toString();
        }
        return "RID: " + jsonRequest.getRid() + "\r\nAPIVersion: " + jsonRequest.getApiVer() + "\r\n" + super.toString();
    }

    private GetData requestProperties = GetData.EMPTY;

    @Override
    public void setRequestedURLParameters(final List<KeyValuePair> requestedURLParameters) {
        super.setRequestedURLParameters(requestedURLParameters);
        requestProperties = MyJDownloaderGetRequest.parseGetData(requestedURLParameters);
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
    public String getSignature() {
        return requestProperties.signature;
    }

    @Override
    public String getJqueryCallback() {
        return requestProperties.callback;
    }

    @Override
    public MyJDownloaderHttpConnection getConnection() {
        return (MyJDownloaderHttpConnection) super.getConnection();
    }

    public String getRequestConnectToken() {
        return getConnection().getRequestConnectToken();
    }

    public synchronized List<KeyValuePair> getPostParameter() throws IOException {
        if (postParameterParsed) {
            return postParameters;
        }
        postParameters = new LinkedList<KeyValuePair>();
        final Object[] params = getJsonRequest().getParams();
        if (params != null) {
            for (final Object parameter : params) {
                if (parameter instanceof JSonObject) {
                    /*
                     * JSonObject has customized .toString which converts Map to Json!
                     */
                    postParameters.add(new KeyValuePair(parameter.toString()));
                } else {
                    final String jsonParameter = parameter + "";
                    postParameters.add(new KeyValuePair(jsonParameter));
                }
            }
        }
        postParameterParsed = true;
        for (final KeyValuePair param : postParameters) {
            if (param.key != null) {
                /* key=value(parameter) */
                if (MyJDownloaderGetRequest.CALLBACK.equalsIgnoreCase(param.key)) {
                    /* filter jquery callback */
                    requestProperties.callback = param.value;
                    continue;
                } else if (MyJDownloaderGetRequest.SIGNATURE.equalsIgnoreCase(param.key)) {
                    /* filter url signature */
                    requestProperties.signature = param.value;
                    continue;
                } else if (MyJDownloaderGetRequest.RID.equalsIgnoreCase(param.key)) {
                    requestProperties.rid = Long.parseLong(param.value);
                    continue;
                } else if (MyJDownloaderGetRequest.API_VERSION.equalsIgnoreCase(param.key)) {
                    requestProperties.apiVersion = Integer.parseInt(param.value);
                    continue;
                } else if (MyJDownloaderGetRequest.DIFF_KEEPALIVE.equalsIgnoreCase(param.key)) {
                    requestProperties.diffKeepalive = Long.parseLong(param.value);
                    continue;
                } else if (MyJDownloaderGetRequest.DIFF_ID.equalsIgnoreCase(param.key)) {
                    requestProperties.diffID = param.value;
                    continue;
                }
            }
        }
        return postParameters;
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

    public JSonRequest getJsonRequest() throws IOException {
        if (jsonRequest != null) {
            return jsonRequest;
        }
        synchronized (this) {
            if (jsonRequest != null) {
                return jsonRequest;
            }
            try {
                final byte[] jsonBytes = IO.readStream(-1, getInputStream());
                final String json = new String(jsonBytes, "UTF-8");
                jsonRequest = JSonStorage.restoreFromString(json, new TypeRef<JSonRequest>() {
                });
                return jsonRequest;
            } catch (IOException e) {
                throw e;
            }
        }
    }

    private InputStream finalInputStream = null;

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        if (finalInputStream != null) {
            return finalInputStream;
        }
        try {
            final String contentType = getRequestHeaders().getValue(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
            final InputStream aesInputStream;
            if (StringUtils.startsWithCaseInsensitive(contentType, "application/rsajson")) {
                final InputStream is = super.getInputStream();
                final InputStream rsaKey = new InputStream() {
                    private boolean eof = false;

                    @Override
                    public void close() throws IOException {
                    }

                    @Override
                    public int read() throws IOException {
                        if (eof) {
                            return -1;
                        } else {
                            final int ret = is.read();
                            if (ret == -1) {
                                eof = true;
                                return -1;
                            } else if (ret == '|') {
                                // Delimiter between rsaKey and aes encrypted content
                                eof = true;
                                return -1;
                            } else {
                                return ret;
                            }
                        }
                    }
                };
                final Base64InputStream rsaKeyStream = new Base64InputStream(rsaKey);
                final Cipher rsaCipher = Cipher.getInstance("RSA");
                rsaCipher.init(Cipher.DECRYPT_MODE, getConnection().getRSAKeyPair().getPrivate());
                final byte[] ivAesBytes = IO.readStream(-1, new HexInputStream(new CipherInputStream(rsaKeyStream, rsaCipher)));
                final byte[] iv;
                final byte[] key;
                if (ivAesBytes.length == 32) {
                    iv = Arrays.copyOfRange(ivAesBytes, 0, 16);
                    key = Arrays.copyOfRange(ivAesBytes, 16, 32);
                } else {
                    iv = Arrays.copyOfRange(ivAesBytes, 0, 16);
                    key = Arrays.copyOfRange(ivAesBytes, 16, 48);
                }
                getConnection().setKey(key);
                getConnection().setIv(iv);
                aesInputStream = is;
            } else {
                aesInputStream = super.getInputStream();
            }
            final IvParameterSpec ivSpec = new IvParameterSpec(getConnection().getIv());
            final SecretKeySpec skeySpec = new SecretKeySpec(getConnection().getKey(), "AES");
            final Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            finalInputStream = new CipherInputStream(new Base64InputStream(aesInputStream), aesCipher);
            return finalInputStream;
        } catch (final NoSuchPaddingException e) {
            throw new IOException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (final InvalidKeyException e) {
            throw new IOException(e);
        } catch (final InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        }
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
