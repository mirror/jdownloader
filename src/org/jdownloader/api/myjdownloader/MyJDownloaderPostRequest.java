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
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderGetRequest.GetData;
import org.jdownloader.myjdownloader.client.json.JSonRequest;

public class MyJDownloaderPostRequest extends PostRequest implements MyJDownloaderRequestInterface {

    private JSonRequest jsonRequest;

    public MyJDownloaderPostRequest(MyJDownloaderHttpConnection myJDownloaderHttpConnection) {
        super(myJDownloaderHttpConnection);

    }

    private GetData requestProperties = GetData.EMPTY;

    @Override
    public void setRequestedURLParameters(final LinkedList<String[]> requestedURLParameters) {
        super.setRequestedURLParameters(requestedURLParameters);

        requestProperties = MyJDownloaderGetRequest.parseGetData(requestedURLParameters);

    }

    public int getApiVersion() {
        if (requestProperties.apiVersion >= 0) { return requestProperties.apiVersion; }
        JSonRequest jsonr;

        try {
            jsonr = getJsonRequest();

            if (jsonr != null) { return jsonr.getApiVer(); }
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
                if (MyJDownloaderGetRequest.CALLBACK.equalsIgnoreCase(param[0])) {
                    /* filter jquery callback */
                    requestProperties.callback = param[1];
                    continue;
                } else if (MyJDownloaderGetRequest.SIGNATURE.equalsIgnoreCase(param[0])) {
                    /* filter url signature */
                    requestProperties.signature = param[1];
                    continue;
                } else if (MyJDownloaderGetRequest.RID.equalsIgnoreCase(param[0])) {
                    requestProperties.rid = Long.parseLong(param[1]);
                    continue;
                } else if (MyJDownloaderGetRequest.API_VERSION.equalsIgnoreCase(param[0])) {
                    requestProperties.apiVersion = Integer.parseInt(param[1]);
                    continue;
                }

            }
        }
        return postParameters;
    }

    @Override
    public long getRid() throws IOException {
        if (requestProperties.rid >= 0) { return requestProperties.rid; }
        JSonRequest jsonr;

        jsonr = getJsonRequest();

        if (jsonr != null) {

        return jsonr.getRid(); }
        return -1;

    }

    public JSonRequest getJsonRequest() throws IOException {
        if (jsonRequest != null) { return jsonRequest; }
        synchronized (this) {
            if (jsonRequest != null) { return jsonRequest; }

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

}
