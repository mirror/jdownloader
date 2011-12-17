package org.jdownloader.api.cnl2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.utils.JDUtilities;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class Cnl2APIImpl implements Cnl2APIBasics, Cnl2APIFlash {

    public void crossdomainxml(RemoteAPIResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\r\n");
        sb.append("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
        sb.append("<cross-domain-policy>\r\n");
        sb.append("<allow-access-from domain=\"*\" />\r\n");
        sb.append("</cross-domain-policy>\r\n");
        writeString(response, sb.toString());
    }

    /**
     * writes given String to response and sets content-type to text/html
     * 
     * @param response
     * @param string
     */
    private void writeString(RemoteAPIResponse response, String string) {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "text/html", false));
            out = RemoteAPI.getOutputStream(response, null, false, true);
            out.write(string.getBytes("UTF-8"));
        } catch (Throwable e) {
            throw new RemoteAPIException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    public void jdcheckjs(RemoteAPIResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdownloader=true;\r\n");
        sb.append("var version='" + JDUtilities.getRevision() + "';\r\n");
        writeString(response, sb.toString());
    }

    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            String crypted = getParameterbyKey(request, "crypted");
            String jk = getParameterbyKey(request, "jk");
            String k = getParameterbyKey(request, "k");
            String passwords = getParameterbyKey(request, "passwords");
            String source = getParameterbyKey(request, "source");
            String links = decrypt(crypted, jk, k);

        } catch (Throwable e) {
            throw new RemoteAPIException(e);
        }
    }

    /**
     * returns the value of a given key, if found in the request
     * 
     * @param request
     * @param key
     * @return
     * @throws IOException
     */
    private static String getParameterbyKey(RemoteAPIRequest request, String key) throws IOException {
        LinkedList<String[]> params = request.getRequestedURLParameters();
        if (params != null) {
            for (String[] param : params) {
                if (key.equalsIgnoreCase(param[0]) && param.length == 2) return param[1];
            }
        }
        params = request.getPostParameter();
        if (params != null) {
            for (String[] param : params) {
                if (key.equalsIgnoreCase(param[0]) && param.length == 2) return param[1];
            }
        }
        return null;
    }

    /* decrypt given bytearray with given aes key */
    public static String decrypt(byte[] b, byte[] key) {
        Cipher cipher;
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(key);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b));
        } catch (Throwable e) {
            Log.exception(e);
        }
        return null;
    }

    /* decrypt given crypted string with js encrypted aes key */
    public static String decrypt(String crypted, final String jk, String k) {
        byte[] key = null;
        if (jk != null) {
            Context cx = null;
            try {
                try {
                    cx = ContextFactory.getGlobal().enterContext();
                    cx.setClassShutter(new ClassShutter() {
                        public boolean visibleToScripts(String className) {
                            if (className.startsWith("adapter")) {
                                return true;
                            } else {
                                throw new RuntimeException("Security Violation");
                            }
                        }
                    });
                } catch (java.lang.SecurityException e) {
                    /* in case classshutter already set */
                }
                Scriptable scope = cx.initStandardObjects();
                String fun = jk + "  f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                key = HexFormatter.hexToByteArray(Context.toString(result));
            } finally {
                try {
                    Context.exit();
                } catch (final Throwable e) {
                }
            }
        } else {
            key = HexFormatter.hexToByteArray(k);
        }
        byte[] baseDecoded = Base64.decode(crypted);
        return decrypt(baseDecoded, key).trim();
    }
}
