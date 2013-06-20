package org.jdownloader.extensions.myjdownloader;

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

public class MyJDownloaderPostRequest extends PostRequest {

    private MyJDownloaderHttpConnection myJDConnection;

    public MyJDownloaderPostRequest(MyJDownloaderHttpConnection myJDownloaderHttpConnection) {
        super(myJDownloaderHttpConnection);
        myJDConnection = myJDownloaderHttpConnection;
    }

    public synchronized LinkedList<String[]> getPostParameter() throws IOException {
        if (postParameterParsed) { return postParameters; }
        JSonRequest jsonRequest = null;
        final byte[] jsonBytes = IO.readStream(-1, getInputStream());
        final String json = new String(jsonBytes, "UTF-8");
        jsonRequest = JSonStorage.restoreFromString(json, new TypeRef<JSonRequest>() {
        });

        if (!this.myJDConnection.isJSonRequestValid(jsonRequest)) {
            //
            throw new IOException("Invalid AESJSON Request");
        }
        postParameters = new LinkedList<String[]>();
        for (final Object parameter : jsonRequest.getParams()) {
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
        postParameterParsed = true;
        return postParameters;
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(myJDConnection.getPayloadEncryptionToken(), 0, 16));
            final SecretKeySpec skeySpec = new SecretKeySpec(Arrays.copyOfRange(myJDConnection.getPayloadEncryptionToken(), 16, 32), "AES");
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
