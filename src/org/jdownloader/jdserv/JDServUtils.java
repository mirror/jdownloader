package org.jdownloader.jdserv;

import java.io.File;
import java.io.IOException;

import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;

public class JDServUtils {
    // private static String BASE = "http://nas:81/thomas/fcgi/"
    private static String BASE = "http://update3.jdownloader.org/jdserv/";

    public static String uploadLog(File zip, String id) throws IOException {
        return uploadLog(zip, "", id);
    }

    public static String uploadLog(File zip, String name, String id) throws IOException {
        //
        return upload(IO.readFile(zip), name, id);
    }

    public static String upload(byte[] bytes, String name, String id) {

        Browser br = new Browser();
        String url = BASE + "UploadInterface/upload";
        br.setProxySelector(ProxyController.getInstance());

        try {

            PostFormDataRequest req = (PostFormDataRequest) br.createPostFormDataRequest(url);

            req.addFormData(new FormData("0", "Data", bytes));
            req.addFormData(new FormData("1", Encoding.urlEncode(JSonStorage.serializeToJson(name))));
            req.addFormData(new FormData("2", Encoding.urlEncode(JSonStorage.serializeToJson(id))));
            System.out.println("Length: " + bytes.length);
            URLConnectionAdapter con = br.openRequestConnection(req);
            String red = br.loadConnection(con).getHtmlCode();
            red = JSonStorage.restoreFromString(red, TypeRef.STRING);
            return red;
        } catch (final Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }

    }

}
