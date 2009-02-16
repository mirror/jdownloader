package jd.update;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JDUpdateUtils {

    private static long last_updateLists_Internal = 0;
    private static long interval_updateLists_Internal = 1000 * 60 * 1;

    private static String addonlist = null;
    private static String updatelist = null;

    private static String listpath = "http://212.117.163.148/update/";

    private static String oldhash = null;

    private static Integer lock = 1;

    private static boolean ServerListUpdated = false;

    private static void updateLists_ParseZip(ByteBuffer updateLists_Internal) throws IOException {
        ZipInputStream ZipStream = new ZipInputStream(InputStreamfromByteBuffer(updateLists_Internal));
        ZipEntry ze = null;
        synchronized (lock) {
            while ((ze = ZipStream.getNextEntry()) != null) {
                if (ze.getName().equalsIgnoreCase("hashlist.lst")) {
                    updatelist = readfromZip(ZipStream);
                } else if (ze.getName().equalsIgnoreCase("addonlist.lst")) {
                    addonlist = readfromZip(ZipStream);
                }
            }
        }
    }

    public static synchronized void update_ServerList() {
        if (ServerListUpdated) return;
        try {
            String ServerList = ByteBuffer2String(download(listpath + "server.lst", -1));
            String Servers[] = splitLines(ServerList);
            if (Servers.length > 0) {
                try {
                    new URL(Servers[0].trim());
                    WebUpdater.setprimaryUpdatePrefixfromServer(Servers[0].trim());
                } catch (Exception e) {
                }
            }
            if (Servers.length > 1) {
                try {
                    new URL(Servers[1].trim());
                    WebUpdater.setsecondaryUpdatePrefixfromServer(Servers[1].trim());
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ServerListUpdated = true;
    }

    private static String ByteBuffer2String(ByteBuffer input) {
        if (input == null) return null;
        byte[] b = new byte[input.limit()];
        input.get(b);
        try {
            return new String(b, "UTF-8").trim();
        } catch (Exception e) {
        }
        return null;
    }

    private static synchronized void updateLists_Internal() {
        if (System.currentTimeMillis() - last_updateLists_Internal < interval_updateLists_Internal) return;
        String newhash = null;
        /* update.md5 holen */
        System.out.println("Fetch Update Hash");
        try {
            newhash = ByteBuffer2String(download(listpath + "update.md5", -1));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not fetch Update Hash!");
        }
        /* mal schaun ob wir eine Update.zip in der ConfigFile habe */
        if (newhash != null && oldhash == null) {
            ByteBuffer updateList_cached = ByteArraytoByteBuffer((byte[]) SubConfiguration.getSubConfig("WEBUPDATE").getProperty("update.zip", null));
            if (updateList_cached != null) {
                System.out.println("Fetch UpdateList from ConfigFile");
                try {
                    updateLists_ParseZip(updateList_cached.duplicate());
                    oldhash = getMD5fromByteBuffer(updateList_cached.duplicate());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Could not fetch UpdateList from ConfigFile");
                }
            }
        }
        /* Sind die Hashes gleich */
        if (oldhash != null && newhash != null && oldhash.equalsIgnoreCase(newhash)) {
            System.out.println("Update Hash has not changed! No need to fetch new UpdateList!");
            last_updateLists_Internal = System.currentTimeMillis();
            return;
        }
        /* update.zip holen */
        System.out.println("Fetch UpdateList from Server");
        try {
            ByteBuffer updateList_downloaded = download(listpath + "update.zip", -1);
            updateLists_ParseZip(updateList_downloaded.duplicate());
            SubConfiguration.getSubConfig("WEBUPDATE").setProperty("update.zip", ByteBuffertoByteArray(updateList_downloaded.duplicate()));
            SubConfiguration.getSubConfig("WEBUPDATE").save();
            oldhash = getMD5fromByteBuffer(updateList_downloaded.duplicate());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not fetch UpdateList");
            last_updateLists_Internal = System.currentTimeMillis();
            return;
        }
        last_updateLists_Internal = System.currentTimeMillis();
        System.out.println("UpdateList: ok!");
    }

    private static byte[] ByteBuffertoByteArray(ByteBuffer buffer) {
        if (buffer == null) return null;
        byte[] b = new byte[buffer.limit()];
        buffer.get(b);
        return b;
    }

    private static ByteBuffer ByteArraytoByteBuffer(byte[] b) {
        if (b == null) return null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(b.length);
        buffer.put(b);
        buffer.flip();
        return buffer;
    }

    private static String getMD5fromByteBuffer(ByteBuffer buffer) {
        if (buffer == null) return null;
        String md5 = null;
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("md5");
            byte[] b = new byte[1024];
            InputStream in = InputStreamfromByteBuffer(buffer);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (byte element : digest) {
                String tmp = Integer.toHexString(element & 0xFF);
                if (tmp.length() < 2) {
                    tmp = "0" + tmp;
                }
                ret += tmp;
            }
            md5 = ret.trim();
        } catch (Exception e3) {
            e3.printStackTrace();
            System.out.println("Could not create Hash");
        }
        return md5;
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
        String ret = new String(b, "UTF-8");
        return ret;
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
        fileurl += (fileurl.contains("?") ? "&" : "?") + System.currentTimeMillis();
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

    public static String[] splitLines(String source) {
        return source.split("\r\n|\r|\n");
    }

    public static boolean backupDataBase() {
        String[] filenames = new String[] { "JDU.cfg", "WEBUPDATE.cfg", "database.properties", "database.script" };
        byte[] buf = new byte[8192];
        File file = new File(WebUpdater.getJDDirectory(), "backup/database.zip");
        if (file.exists()) {
            File old = new File(WebUpdater.getJDDirectory(), "backup/database_" + file.lastModified() + ".zip");
            file.getParentFile().mkdirs();
            if (file.exists()) {
                file.renameTo(old);
            }
            file.delete();
        } else {
            file.getParentFile().mkdirs();
        }
        try {
            String outFilename = file.getAbsolutePath();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

            for (int i = 0; i < filenames.length; i++) {
                File filein = new File(WebUpdater.getJDDirectory(), "config/" + filenames[i]);
                if (!filein.exists()) continue;
                FileInputStream in = new FileInputStream(filein.getAbsoluteFile());
                out.putNextEntry(new ZipEntry(filenames[i]));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
