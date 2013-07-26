package org.jdownloader.api.myjdownloader.api;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import jd.nutils.encoding.Encoding;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderAPI extends AbstractMyJDClient {

    private BasicHTTP br;
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
    protected String post(final String query, final String object, final byte[] keyAndIV) throws ExceptionResponse {
        HTTPConnection con = null;
        String ret = null;
        try {
            if (keyAndIV != null) {
                br.putRequestHeader("Accept-Encoding", "gzip_aes");
                final byte[] sendBytes = (object == null ? "" : object).getBytes("UTF-8");
                final HashMap<String, String> header = new HashMap<String, String>();
                header.put(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "" + sendBytes.length);
                con = br.openPostConnection(new URL(this.getServerRoot() + query), null, new ByteArrayInputStream(sendBytes), header);
                final String content_Encoding = con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
                if (con.getResponseCode() == 200) {
                    if ("gzip_aes".equals(content_Encoding)) {
                        final byte[] aes = IO.readStream(-1, con.getInputStream());
                        final byte[] decrypted = this.decrypt(aes, keyAndIV);
                        ret = IO.readInputStreamToString(new GZIPInputStream(new ByteArrayInputStream(decrypted)));
                    } else {
                        final byte[] aes = IO.readStream(-1, new Base64InputStream(con.getInputStream()));
                        final byte[] decrypted = this.decrypt(aes, keyAndIV);
                        ret = new String(decrypted, "UTF-8");
                    }
                } else {
                    ret = IO.readInputStreamToString(con.getInputStream());
                }
            } else {
                br.putRequestHeader("Accept-Encoding", null);
                ret = br.postPage(new URL(this.getServerRoot() + query), object == null ? "" : object);
                con = br.getConnection();
            }
            System.out.println(con);
            if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) { throw new ExceptionResponse(ret, con.getResponseCode()); }
            return ret;
        } catch (final ExceptionResponse e) {
            throw e;
        } catch (final Exception e) {
            throw new ExceptionResponse(e);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    protected AtomicLong              TIMESTAMP    = new AtomicLong(System.currentTimeMillis());
    protected volatile String         connectToken = null;

    private MyJDownloaderController   extension;
    private HashMap<String, RIDArray> rids;

    public static int getRevision() {
        String revision = new Regex("$Revision$", "Revision:\\s*?(\\d+)").getMatch(0);
        if (revision == null) return 0;
        return Integer.parseInt(revision);
    }

    public MyJDownloaderAPI(MyJDownloaderController myJDownloaderExtension) {
        super("JD_" + getRevision());
        extension = myJDownloaderExtension;
        setServerRoot("http://" + CFG_MYJD.CONNECT_IP.getValue() + ":" + CFG_MYJD.CLIENT_CONNECT_PORT.getValue());
        logger = extension.getLogger();
        br = new BasicHTTP();
        br.setAllowedResponseCodes(200, 503, 401, 407, 403, 500, 429);
        br.putRequestHeader("Content-Type", "application/json; charset=utf-8");
        rids = new HashMap<String, RIDArray>();

    }

    public LogSource getLogger() {
        return logger;
    }

    /* TODO: add session support, currently all sessions share the same validateRID */
    public synchronized boolean validateRID(long rid, String sessionToken) {

        // TODO CLeanup
        RIDArray ridList = rids.get(sessionToken);
        if (ridList == null) {
            ridList = new RIDArray();
            rids.put(sessionToken, ridList);
        }

        // lowest RID
        long lowestRid = Long.MIN_VALUE;
        RIDEntry next;
        for (Iterator<RIDEntry> it = ridList.iterator(); it.hasNext();) {
            next = it.next();
            if (next.getRid() == rid) {
                // dupe rid is always bad
                logger.warning("received an RID Dupe. Possible Replay Attack avoided");
                return false;
            }
            if (System.currentTimeMillis() - next.getTimestamp() > 15000) {
                it.remove();
                if (next.getRid() > lowestRid) {
                    lowestRid = next.getRid();
                }

            }
        }
        if (lowestRid > ridList.getMinAcceptedRID()) {
            ridList.setMinAcceptedRID(lowestRid);
        }
        if (rid <= ridList.getMinAcceptedRID()) {
            // rid too low
            logger.warning("received an outdated RID. Possible Replay Attack avoided");
            return false;
        }
        RIDEntry ride = new RIDEntry(rid);
        ridList.add(ride);

        return true;
    }
}
