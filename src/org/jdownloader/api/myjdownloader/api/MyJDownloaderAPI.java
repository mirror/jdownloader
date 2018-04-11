package org.jdownloader.api.myjdownloader.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLConnectionAdapterDirectImpl;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.KeyValueStringEntry;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64InputStream;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderAPI extends AbstractMyJDClientForDesktopJVM {
    private LogSource logger;

    @Override
    protected byte[] base64decode(String base64encodedString) {
        return Base64.decode(base64encodedString);
    }

    @Override
    protected String base64Encode(byte[] encryptedBytes) {
        return Base64.encodeToString(encryptedBytes, false);
    }

    @Override
    public String urlencode(String text) {
        return Encoding.urlEncode(text);
    }

    @Override
    protected String objectToJSon(final Object payload) {
        return JSonStorage.serializeToJson(payload);
    }

    @Override
    protected <T> T jsonToObject(final String dec, final Type clazz) {
        return JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
        });
    }

    @Override
    protected byte[] post(final String query, final String object, final byte[] keyAndIV) throws ExceptionResponse {
        URLConnectionAdapter con = null;
        byte[] ret = null;
        try {
            final Browser br = getBrowser();
            final byte[] sendBytes = (object == null ? "" : object).getBytes("UTF-8");
            final PostRequest request = br.createPostRequest(this.getServerRoot() + query, new ArrayList<KeyValueStringEntry>(), null);
            request.setPostBytes(sendBytes);
            request.setContentType("application/json; charset=utf-8");
            if (keyAndIV != null) {
                request.getHeaders().put("Accept-Encoding", "gazeisp");
                con = openRequestConnection(br, request);
                String contentEncoding = con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
                String xContentEncoding = con.getHeaderField("X-" + HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
                if (xContentEncoding != null && (StringUtils.containsIgnoreCase(xContentEncoding, "gazeisp") || StringUtils.containsIgnoreCase(xContentEncoding, "gzip_aes"))) {
                    contentEncoding = xContentEncoding;
                }
                final String content_Type = con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE);
                if (con.getResponseCode() == 200) {
                    if (StringUtils.contains(contentEncoding, "gazeisp") || StringUtils.contains(contentEncoding, "gzip_aes")) {
                        final byte[] aes = IO.readStream(-1, con.getInputStream());
                        final byte[] decrypted = this.decrypt(aes, keyAndIV);
                        return IO.readStream(-1, new GZIPInputStream(new ByteArrayInputStream(decrypted)));
                    } else if (StringUtils.contains(content_Type, "aesjson-server")) {
                        final byte[] aes = IO.readStream(-1, new Base64InputStream(con.getInputStream()));
                        return this.decrypt(aes, keyAndIV);
                    }
                    return IO.readStream(-1, con.getInputStream());
                } else {
                    ret = IO.readStream(-1, con.getInputStream());
                }
            } else {
                request.getHeaders().put("Accept-Encoding", null);
                con = openRequestConnection(br, request);
                ret = IO.readStream(-1, con.getInputStream());
            }
            // System.out.println(con);
            if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) {
                throw new ExceptionResponse(toString(ret), con.getResponseCode(), con.getResponseMessage());
            } else {
                return ret;
            }
        } catch (final ExceptionResponse e) {
            throw e;
        } catch (final Exception e) {
            if (con != null) {
                throw new ExceptionResponse(e, con.getResponseCode(), con.getResponseMessage());
            } else {
                throw new ExceptionResponse(e);
            }
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    protected URLConnectionAdapter openRequestConnection(Browser br, Request request) throws IOException {
        int retryDirectSocketTimeoutException = 3;
        while (true) {
            try {
                return br.openRequestConnection(request);
            } catch (BrowserException e) {
                logger.log(e);
                if (Exceptions.containsInstanceOf(e, SocketTimeoutException.class) && request.getHttpConnection() instanceof URLConnectionAdapterDirectImpl && retryDirectSocketTimeoutException-- > 0) {
                    request.getHeaders().put("X-RDSTE", Integer.toString(retryDirectSocketTimeoutException));
                    logger.info("retryDirectSocketTimeoutException:" + retryDirectSocketTimeoutException);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        throw e;
                    }
                    continue;
                }
                throw e;
            }
        }
    }

    protected Browser getBrowser() {
        return new MyJDownloaderAPIBrowser();
    }

    protected volatile String connectToken = null;

    private static String getRevision() {
        try {
            final HashMap<String, Object> map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), TypeRef.HASHMAP);
            final Object ret = map.get("JDownloaderRevision");
            if (ret != null) {
                return "core_" + ret.toString();
            }
        } catch (final Throwable e) {
        }
        final String revision = new Regex("$Revision$", "Revision:\\s*?(\\d+)").getMatch(0);
        if (revision == null) {
            return "api_0";
        } else {
            return "api_" + revision;
        }
    }

    private class MyJDownloaderAPIBrowser extends Browser {
        {
            setDebug(true);
            setVerbose(true);
            setAllowedResponseCodes(200, 503, 401, 407, 403, 500, 429);
        }

        @Override
        protected void onBeforeRequestConnect(Request request) throws IOException {
            if (request.getProxy() == null || request.getProxy().isNone() || request.getProxy().isDirect()) {
                request.setConnectTimeout(10 * 1000);
            }
        }

        @Override
        public Browser cloneBrowser() {
            final Browser br = new MyJDownloaderAPIBrowser();
            return cloneBrowser(br);
        }

        @Override
        public LogInterface getLogger() {
            return MyJDownloaderAPI.this.getLogger();
        };
    }

    public MyJDownloaderAPI() {
        super("JD_" + getRevision());
        setServerRoot("https://" + CFG_MYJD.SERVER_HOST.getValue());
    }

    public LogSource getLogger() {
        return logger;
    }

    public void setLogger(LogSource logger) {
        this.logger = logger;
    }
}
