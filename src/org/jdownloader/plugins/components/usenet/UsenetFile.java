package org.jdownloader.plugins.components.usenet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jd.plugins.DownloadLink;
import jd.plugins.download.HashInfo;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.CharSequenceInputStream;

public class UsenetFile implements Storable {
    private String hash = null;
    private long   size = -1;

    public void _setHashInfo(HashInfo hashInfo) {
        if (hashInfo != null) {
            this.hash = hashInfo.exportAsString();
        } else {
            this.hash = null;
        }
    }

    public HashInfo _getHashInfo() {
        return HashInfo.importFromString(hash);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumSegments() {
        return numSegments;
    }

    public void setNumSegments(int numSegments) {
        this.numSegments = numSegments;
    }

    public ArrayList<UsenetFileSegment> getSegments() {
        return segments;
    }

    public void setSegments(ArrayList<UsenetFileSegment> segments) {
        this.segments = segments;
    }

    private String                       name        = null;
    private int                          numSegments = -1;
    private ArrayList<UsenetFileSegment> segments    = new ArrayList<UsenetFileSegment>();

    public UsenetFile() {
    }

    public static final String  PROPERTY = "useNetFile";
    public static final Charset UTF8     = Charset.forName("UTF-8");

    public static UsenetFile _read(final DownloadLink downloadLink) throws IOException {
        final String compressedJSonString = downloadLink.getStringProperty(PROPERTY, null);
        if (compressedJSonString != null) {
            final InputStream is = new GZIPInputStream(new Base64InputStream(new CharSequenceInputStream(compressedJSonString, UTF8)));
            final UsenetFile ret = JSonStorage.getMapper().inputStreamToObject(is, new TypeRef<UsenetFile>() {
            });
            return ret;
        }
        return null;
    }

    public void _write(final DownloadLink downloadLink) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Base64OutputStream b64os = new Base64OutputStream(bos);
        final GZIPOutputStream gos = new GZIPOutputStream(b64os);
        JSonStorage.getMapper().writeObject(gos, this);
        gos.close();
        downloadLink.setProperty(PROPERTY, bos.toString(UTF8.name()));
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash
     *            the hash to set
     */
    public void setHash(String hash) {
        this.hash = hash;
    }
}
