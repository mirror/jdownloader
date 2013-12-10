package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.IO;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.httpserver.requests.JSonRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;

public class MyJDownloaderPostRequest extends PostRequest implements MyJDownloaderRequestInterface {

    private JSonRequest jsonRequest;
    private String      jqueryCallback;
    private String      signature;
    private long        requestID;

    public MyJDownloaderPostRequest(MyJDownloaderHttpConnection myJDownloaderHttpConnection) {
        super(myJDownloaderHttpConnection);

    }

    @Override
    public void setRequestedURLParameters(LinkedList<String[]> requestedURLParameters) {
        super.setRequestedURLParameters(requestedURLParameters);
        if (requestedURLParameters != null) {
            for (final String[] param : requestedURLParameters) {
                if (param[1] != null) {
                    /* key=value(parameter) */
                    if ("callback".equalsIgnoreCase(param[0])) {
                        /* filter jquery callback */
                        jqueryCallback = param[1];
                        continue;
                    } else if ("signature".equalsIgnoreCase(param[0])) {
                        /* filter url signature */
                        signature = param[1];
                        continue;
                    } else if ("rid".equalsIgnoreCase(param[0])) {
                        requestID = Long.parseLong(param[1]);
                        continue;
                    }

                }
            }
        }
    }

    @Override
    public MyJDownloaderHttpConnection getConnection() {
        return (MyJDownloaderHttpConnection) super.getConnection();
    }

    public String getRequestConnectToken() {
        return getConnection().getRequestConnectToken();
    }

    public synchronized LinkedList<String[]> getPostParameter() throws IOException {
        if (postParameterParsed) { return postParameters; }

        postParameters = new LinkedList<String[]>();
        Object[] params = getJsonRequest().getParams();
        if (params != null) {
            for (final Object parameter : params) {
                if (parameter instanceof JSonObject) {
                    /*
                     * JSonObject has customized .toString which converts Map to Json!
                     */
                    postParameters.add(new String[] { parameter.toString(), null });
                } else {
                    final String jsonParameter = parameter + "";
                    postParameters.add(new String[] { jsonParameter, null });
                }
            }
        }
        postParameterParsed = true;

        for (final String[] param : postParameters) {
            if (param[1] != null) {
                /* key=value(parameter) */
                if ("callback".equalsIgnoreCase(param[0])) {
                    /* filter jquery callback */
                    jqueryCallback = param[1];
                    continue;
                } else if ("signature".equalsIgnoreCase(param[0])) {
                    /* filter url signature */
                    signature = param[1];
                    continue;
                } else if ("rid".equalsIgnoreCase(param[0])) {
                    requestID = Long.parseLong(param[1]);
                    continue;
                }

            }
        }
        return postParameters;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public long getRid() throws IOException {
        return getJsonRequest().getRid();
    }

    public JSonRequest getJsonRequest() throws IOException {
        if (jsonRequest != null) return jsonRequest;
        synchronized (this) {
            if (jsonRequest != null) return jsonRequest;

            final byte[] jsonBytes = IO.readStream(-1, getInputStream());
            final String json = new String(jsonBytes, "UTF-8");
            jsonRequest = JSonStorage.restoreFromString(json, new TypeRef<JSonRequest>() {
            });

            return jsonRequest;
        }
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(getConnection().getPayloadEncryptionToken(), 0, 16));
            final SecretKeySpec skeySpec = new SecretKeySpec(Arrays.copyOfRange(getConnection().getPayloadEncryptionToken(), 16, 32), "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new CipherInputStream(new Base64InputStream(super.getInputStream()), cipher);
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
    public String getJqueryCallback() {
        return jqueryCallback;
    }

}
