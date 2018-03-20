package org.jdownloader.plugins.components.usenet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jd.plugins.DownloadLink;
import jd.plugins.download.HashInfo;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;

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

    public static final String PROPERTY = "useNetFile";

    public static UsenetFile _read(final DownloadLink downloadLink) throws IOException {
        final String compressedJSonString = downloadLink.getStringProperty(PROPERTY, null);
        if (compressedJSonString != null) {
            /* TODO: refactor to work directly on inputStream (gzip->base64input->charsequenceinput) */
            final byte[] bytes = org.appwork.utils.encoding.Base64.decode(compressedJSonString);
            final String jsonString = IO.readInputStreamToString(new GZIPInputStream(new ByteArrayInputStream(bytes)));
            return JSonStorage.restoreFromString(jsonString, new TypeRef<UsenetFile>() {
            }, null);
        }
        return null;
    }

    public void _write(final DownloadLink downloadLink) throws IOException {
        /*
         * TODO: refactor to work directly on outputstream (gzip->base64->charsequence)
         */
        final String jsonString = JSonStorage.serializeToJson(this);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(jsonString.getBytes("UTF-8"));
        gos.close();
        final String compressedJSonString = org.appwork.utils.encoding.Base64.encodeToString(bos.toByteArray(), false);
        downloadLink.setProperty(PROPERTY, compressedJSonString);
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
