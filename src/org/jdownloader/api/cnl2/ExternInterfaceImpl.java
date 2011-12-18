package org.jdownloader.api.cnl2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.ImageIcon;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.http.Browser;
import jd.utils.JDUtilities;
import net.sf.image4j.codec.ico.ICOEncoder;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequestInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.RemoteAPIConfig;
import org.jdownloader.api.cnl2.translate.ExternInterfaceTranslation;
import org.jdownloader.images.NewTheme;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class ExternInterfaceImpl implements Cnl2APIBasics, Cnl2APIFlash {

    private final static String jdpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + File.separator + "JDownloader.jar";

    public void crossdomainxml(RemoteAPIResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\r\n");
        sb.append("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
        sb.append("<cross-domain-policy>\r\n");
        sb.append("<allow-access-from domain=\"*\" />\r\n");
        sb.append("</cross-domain-policy>\r\n");
        writeString(response, null, sb.toString(), false);
    }

    /**
     * writes given String to response and sets content-type to text/html
     * 
     * @param response
     * @param string
     */
    private void writeString(RemoteAPIResponse response, RemoteAPIRequest request, String string, boolean wrapCallback) {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "text/html", false));
            out = RemoteAPI.getOutputStream(response, request, false, true);
            if (wrapCallback && request.getJqueryCallback() != null) {
                if (string == null) string = "";
                string = "{\"content\": \"" + string.trim() + "\"}";
            }
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
        writeString(response, null, sb.toString(), false);
    }

    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            askPermission(request);
            String crypted = HttpRequest.getParameterbyKey(request, "crypted");
            String jk = HttpRequest.getParameterbyKey(request, "jk");
            String k = HttpRequest.getParameterbyKey(request, "k");
            String urls = decrypt(crypted, jk, k);
            clickAndLoad2Add(urls, request);
            /*
             * we need the \r\n else the website will not handle response
             * correctly
             */
            writeString(response, request, "success\r\n", true);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    private void clickAndLoad2Add(String urls, RemoteAPIRequest request) throws IOException {
        String passwords = HttpRequest.getParameterbyKey(request, "passwords");
        String source = HttpRequest.getParameterbyKey(request, "source");
        String comment = HttpRequest.getParameterbyKey(request, "comment");
        LinkCollectingJob job = new LinkCollectingJob(urls);
        job.setCustomSourceUrl(source);
        job.setCustomComment(comment);
        LinkCollector.getInstance().addCrawlerJob(job);
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

    public void add(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            askPermission(request);
            String urls = HttpRequest.getParameterbyKey(request, "urls");
            clickAndLoad2Add(urls, request);
            writeString(response, request, "success\r\n", true);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    public void addcrypted(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            askPermission(request);
            String dlcContent = HttpRequest.getParameterbyKey(request, "crypted");
            if (dlcContent == null) throw new IllegalArgumentException("no DLC Content available");
            String dlc = dlcContent.trim().replace(" ", "+");
            File tmp = JDUtilities.getResourceFile("tmp/jd_" + System.currentTimeMillis() + ".dlc", true);
            tmp.deleteOnExit();
            IO.writeToFile(tmp, dlc.getBytes("UTF-8"));
            String url = "file://" + tmp.getAbsolutePath();
            clickAndLoad2Add(url, request);
            writeString(response, request, "success\r\n", true);
        } catch (Throwable e) {
            writeString(response, request, "failed " + e.getMessage() + "\r\n", true);
        }
    }

    private synchronized void askPermission(HttpRequestInterface request) throws IOException, DialogNoAnswerException {
        HTTPHeader jdrandomNumber = request.getRequestHeaders().get("jd.randomnumber");
        if (jdrandomNumber != null && jdrandomNumber.getValue() != null && jdrandomNumber.getValue().equalsIgnoreCase(System.getProperty("jd.randomNumber"))) {
            /*
             * request knows secret jd.randomnumber, it is okay to handle this
             * request
             */
            return;
        }
        HTTPHeader referer = request.getRequestHeaders().get(HTTPConstants.HEADER_REQUEST_REFERER);
        String check = null;
        if (referer != null && (check = referer.getValue()) != null) {
            if (check.equalsIgnoreCase("http://localhost:9666/flashgot") || check.equalsIgnoreCase("http://127.0.0.1:9666/flashgot")) {
                /*
                 * security check for flashgot referer, skip asking if we find
                 * valid flashgot referer
                 */
                return;
            }
        }
        String app = "unknown application";
        HTTPHeader agent = request.getRequestHeaders().get(HTTPConstants.HEADER_REQUEST_USER_AGENT);
        if (agent != null && agent.getValue() != null) {
            /* try to parse application name from user agent header */
            app = agent.getValue().replaceAll("\\(.*\\)", "");
        }
        String url = null;
        if (referer != null) {
            /* lets use the referer as source of the request */
            url = referer.getValue();
        }
        if (url == null) {
            /* no referer available, maybe a source variable is? */
            url = HttpRequest.getParameterbyKey(request, "source");
        }
        if (url != null) {
            url = Browser.getHost(url);
        }
        ArrayList<String> allowed = JsonConfig.create(RemoteAPIConfig.class).getExternInterfaceAuth();
        if (allowed != null && url != null && allowed.contains(url)) {
            /* the url is already allowed to add links */
            return;
        }
        String from = url != null ? url : app;
        try {
            Dialog.getInstance().showConfirmDialog(0, ExternInterfaceTranslation._.jd_plugins_optional_interfaces_jdflashgot_security_title(from), ExternInterfaceTranslation._.jd_plugins_optional_interfaces_jdflashgot_security_message(), null, ExternInterfaceTranslation._.jd_plugins_optional_interfaces_jdflashgot_security_btn_allow(), ExternInterfaceTranslation._.jd_plugins_optional_interfaces_jdflashgot_security_btn_deny());
        } catch (DialogNoAnswerException e) {
            throw e;
        }
        if (url != null) {
            /* we can only save permission if an url is available */
            if (allowed == null) allowed = new ArrayList<String>();
            allowed.add(url);
            JsonConfig.create(RemoteAPIConfig.class).setExternInterfaceAuth(allowed);
        }

    }

    public void alive(RemoteAPIResponse response, RemoteAPIRequest request) {
        writeString(response, request, "JDownloader\r\n", true);
    }

    public void favicon(RemoteAPIResponse response) {
        OutputStream out = null;
        try {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/x-icon", false));
            out = RemoteAPI.getOutputStream(response, null, false, false);
            ImageIcon logo = NewTheme.I().getIcon("logo/jd_logo_128_128", 32);
            ICOEncoder.write(IconIO.toBufferedImage(logo.getImage()), out);
        } catch (Throwable e) {
            throw new RemoteAPIException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    public void flashgot(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(jdpath + "\r\n");
            sb.append("java -Xmx512m -jar " + jdpath + "\r\n");
            String urls[] = Regex.getLines(HttpRequest.getParameterbyKey(request, "urls"));
            String desc[] = Regex.getLines(HttpRequest.getParameterbyKey(request, "descriptions"));
            String fnames[] = Regex.getLines(HttpRequest.getParameterbyKey(request, "fnames"));
            String packname = HttpRequest.getParameterbyKey(request, "package");
            String directory = HttpRequest.getParameterbyKey(request, "dir");
            String cookies = HttpRequest.getParameterbyKey(request, "cookies");
            String post = HttpRequest.getParameterbyKey(request, "postData");
            String referer = HttpRequest.getParameterbyKey(request, "referer");
            boolean autostart = "1".equalsIgnoreCase(HttpRequest.getParameterbyKey(request, "autostart"));

            writeString(response, request, sb.toString(), true);
        } catch (final Throwable e) {
            throw new RemoteAPIException(e);
        }
    }
}
