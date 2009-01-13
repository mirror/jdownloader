package jd.update;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JDUpdateUtils {

    private static long last_updateLists_Internal = 0;
    private static long interval_updateLists_Internal = 1000 * 60 * 5;

    private static String addonlist = null;
    private static String updatelist = null;

    private static String listpath = "http://service.jdownloader.net/update/jdupdate.zip";

    private static Integer lock = 1;

    private static synchronized void updateLists_Internal() {
        if (System.currentTimeMillis() - last_updateLists_Internal < interval_updateLists_Internal) return;
        System.out.println("Initialize UpdateList");
        ByteBuffer updateLists_Internal = null;
        try {
            updateLists_Internal = download(listpath, -1);
            ZipInputStream ZipStream = new ZipInputStream(InputStreamfromByteBuffer(updateLists_Internal));
            ZipEntry ze = null;
            synchronized (lock) {
                while ((ze = ZipStream.getNextEntry()) != null) {
                    if (ze.getName().equalsIgnoreCase("update.lst")) {
                        updatelist = readfromZip(ZipStream);
                    } else if (ze.getName().equalsIgnoreCase("addon.lst")) {
                        addonlist = readfromZip(ZipStream);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("UpdateList: failed!");
            last_updateLists_Internal = System.currentTimeMillis();
            return;
        }
        last_updateLists_Internal = System.currentTimeMillis();
        System.out.println("UpdateList: ok!");
    }

    public static String get_AddonList() {
        if (last_updateLists_Internal == 0) updateLists_Internal();
        synchronized (lock) {
            if (addonlist == null) return "";
            return addonlist;
        }
    }

    public static String get_UpdateList() {
        updateLists_Internal();
        synchronized (lock) {
            if (updatelist == null) return "";
            return updatelist;
        }
    }

    private static String readfromZip(ZipInputStream zs) throws IOException {
        ByteBuffer bigbuffer = ByteBuffer.allocateDirect(4096);
        byte[] minibuffer = new byte[1000];
        int len;
        while ((len = zs.read(minibuffer)) != -1) {
            if (bigbuffer.remaining() < 1000) {
                ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            if (len > 0) bigbuffer.put(minibuffer, 0, len);
        }
        bigbuffer.flip();
        byte[] b = new byte[bigbuffer.limit()];
        bigbuffer.get(b);
        return new String(b, "UTF-8");
    }

    private static InputStream InputStreamfromByteBuffer(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) { return -1; }
                return buf.get();
            }

            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                if (!buf.hasRemaining()) { return -1; }
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

    private static ByteBuffer download(String fileurl, int limit) throws IOException {
        URL url = new URL(fileurl);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_PROXY", false)) {
            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS", "");

            httpConnection.setRequestProperty("Proxy-Authorization", "Basic " + WebUpdater.Base64Encode(user + ":" + pass));

        }

        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_SOCKS", false)) {

            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER_SOCKS", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("ROXY_PASS_SOCKS", "");

            httpConnection.setRequestProperty("Proxy-Authorization", "Basic " + WebUpdater.Base64Encode(user + ":" + pass));

        }

        httpConnection.setReadTimeout(20000);
        httpConnection.setReadTimeout(20000);
        httpConnection.setInstanceFollowRedirects(true);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");

        BufferedInputStream input;
        if (httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            input = new BufferedInputStream(new GZIPInputStream(httpConnection.getInputStream()));
        } else {
            input = new BufferedInputStream(httpConnection.getInputStream());
        }

        ByteBuffer bigbuffer = ByteBuffer.allocateDirect(4096);
        byte[] minibuffer = new byte[1000];
        int len;
        while ((len = input.read(minibuffer)) != -1) {
            if (bigbuffer.remaining() < 1000) {
                ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            if (len > 0) bigbuffer.put(minibuffer, 0, len);
            if (limit != -1 && bigbuffer.position() > limit) {
                httpConnection.disconnect();
                throw new IOException("FileContent too big");
            }
        }
        httpConnection.disconnect();
        bigbuffer.flip();
        return bigbuffer;
    }

    public static String getUpdateUrl() {
        return listpath;
    }

    public static void setUpdateUrl(String url) {
        listpath = url;
    }

}
