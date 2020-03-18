package org.jdownloader.jdserv;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.net.URLHelper;

public class JDServUtils {
    // private static String BASE = "http://nas:81/thomas/fcgi/"
    public final static String BASE = "https://update3.jdownloader.org/jdserv/UploadInterface/";

    public static String uploadLog(File zip, String id) throws IOException {
        return uploadLog(zip, "", id);
    }

    public static String uploadLog(final File zip, String name, String id) throws IOException { //
        final Browser br = new Browser();
        br.setVerbose(true);
        final String url = URLHelper.parseLocation(new URL(BASE), "upload");
        br.setProxySelector(ProxyController.getInstance());
        URLConnectionAdapter con = null;
        try {
            final PostFormDataRequest req = br.createPostFormDataRequest(url);
            req.addFormData(new FormData("0", "Data", zip));
            req.addFormData(new FormData("1", Encoding.urlEncode(JSonStorage.serializeToJson(name))));
            req.addFormData(new FormData("2", Encoding.urlEncode(JSonStorage.serializeToJson(id))));
            con = br.openRequestConnection(req);
            final String response = br.loadConnection(con).getHtmlCode();
            final String string = JSonStorage.restoreFromString(response, TypeRef.STRING);
            return string;
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new IOException(e);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
