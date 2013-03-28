package org.jdownloader.myjdownloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.api.jd.JDAPI;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.exceptions.APIException;
import org.jdownloader.myjdownloader.client.exceptions.InvalidResponseCodeException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderUnexpectedIOException;

public class Test {
    /**
     * @param args
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws APIException
     * @throws MyJDownloaderException
     */
    public static void main(String[] args) throws APIException, MyJDownloaderException {
        final Browser br = new Browser();
        br.setAllowedResponseCodes(200);
        // br.forceDebug(true);
        // Log.L.setLevel(Level.ALL);
        AbstractMyJDClient api = new AbstractMyJDClient() {

            @Override
            protected String post(String query, String object) throws MyJDownloaderException {
                try {

                    String ret = br.postPage(getServerRoot() + query, object == null ? "" : object);

                    URLConnectionAdapter con = br.getRequest().getHttpConnection();

                    if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) {

                    throw new InvalidResponseCodeException(con.getResponseCode());

                    }
                    System.out.println(con);
                    System.out.println(ret);
                    return ret;
                } catch (BrowserException e) {

                    if (e.getConnection() != null && e.getConnection().getResponseCode() > 0 && e.getConnection().getResponseCode() != 200) {

                    throw new InvalidResponseCodeException(e.getConnection().getResponseCode());

                    }

                    throw new MyJDownloaderUnexpectedIOException(e);
                } catch (IOException e) {

                    throw new MyJDownloaderUnexpectedIOException(e);
                } finally {
                    URLConnectionAdapter con = br.getRequest().getHttpConnection();
                    System.out.println(con);
                }

            }

            @Override
            protected byte[] base64decode(String base64encodedString) {
                return Base64.decode(base64encodedString);
            }

            @Override
            protected String base64Encode(byte[] encryptedBytes) {
                return Base64.encodeToString(encryptedBytes, false);
            }

            @Override
            protected String objectToJSon(Object payload) {
                return JSonStorage.toString(payload);
            }

            @Override
            protected <T> T jsonToObject(String dec, Type clazz) {
                return (T) JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
                });
            }

        };

        api.connect("", "");
        JDAPI jdapi = api.link(JDAPI.class, "jd");
        System.out.println(jdapi.uptime());

        // List<FilePackageAPIStorable> ret = api.link(DownloadsAPI.class, "downloads").queryPackages(new APIQuery());
        // // api.link(DownloadsAPI.class, "downloads").start();
        // System.out.println(ret);
        api.disconnect();
        System.out.println(jdapi.uptime());
    }
}
