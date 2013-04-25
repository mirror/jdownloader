//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.streaming.rarstream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.parser.Regex;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.io.streamingio.Streaming;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.streaming.StreamingProvider;
import org.jdownloader.logging.LogController;

/**
 * Used to join the separated rar files.
 * 
 * @author botzi
 * 
 */
public class RarStreamProvider implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword {
    private Map<String, IInStream>       openStreamMap   = new HashMap<String, IInStream>();
    private String                       name;
    private String                       password;
    private Archive                      archive;
    private HashMap<String, ArchiveFile> map;
    private String                       firstName;
    private Logger                       logger;
    private RandomAccessStreaming        latestAccessedStream;
    private StreamingProvider            streamProvider;
    private boolean                      readyForExtract = false;

    public boolean isReadyForExtract() {
        return readyForExtract;
    }

    RarStreamProvider(Archive archive, String password, StreamingProvider streamProvider) {
        if (password == null) password = "";
        this.password = password;
        this.archive = archive;
        this.streamProvider = streamProvider;
        init();
    }

    public Archive getArchive() {
        return archive;
    }

    public StreamingProvider getStreamProvider() {
        return streamProvider;
    }

    private void init() {
        map = new HashMap<String, ArchiveFile>();
        logger = LogController.CL();
        // support for test.part01-blabla.tat archives.
        // we have to create a rename matcher map in this case because 7zip cannot handle this type
        if (logger != null) logger.info("Init Map:");
        if (archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?\\d+\\D.*?\\.rar$")) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                String name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(part\\d+)").getMatch(0) + ".rar";

                if (logger != null) logger.info(af.getFilePath() + " name: " + name);
                if (af == archive.getFirstArchiveFile()) {
                    firstName = name;
                    if (logger != null) logger.info(af.getFilePath() + " FIRSTNAME name: " + name);
                }
                if (map.put(name, af) != null) {
                    //
                    throw new WTFException("Cannot handle " + af.getFilePath());
                }
            }

        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
        case NAME:
            return name;
        }
        return null;
    }

    public boolean isStreamOpen(String filename) {
        return openStreamMap.containsKey(filename);
    }

    public IInStream getStream(ArchiveFile firstArchiveFile) throws SevenZipException {
        return getStream(firstName == null ? firstArchiveFile.getFilePath() : firstName);
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {

            if (logger != null) logger.info("Stream request: " + filename);
            IInStream stream = openStreamMap.get(filename);
            ArchiveFile af = map.get(filename);
            if (stream != null) {

                name = filename;
                return stream;
            }

            if (logger != null) logger.info("New RandomAccess: " + (af == null ? filename : af.getFilePath()));
            name = filename;
            ArchiveFile archiveFile = af == null ? archive.getArchiveFileByPath(filename) : af;
            if (archiveFile == null) return null;
            if (archiveFile instanceof DownloadLinkArchiveFile) {
                Streaming streaming = streamProvider.getStreamingProvider(((DownloadLinkArchiveFile) archiveFile).getDownloadLinks().get(0));

                stream = new RandomAccessStreaming(archiveFile, filename, this, streaming);
                openStreamMap.put(filename, stream);

            } else {

                stream = new ExtRandomAccessFileInStream(archiveFile, filename, this);
                openStreamMap.put(filename, stream);
            }
            return stream;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes open files.
     * 
     * @throws IOException
     */
    void close() throws IOException {
        Iterator<Entry<String, IInStream>> it = openStreamMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, IInStream> next = it.next();
            if (next.getValue() instanceof RandomAccessStreaming) {
                try {
                    ((RandomAccessStreaming) next.getValue()).close();
                } catch (final Throwable e) {
                }
            }
            it.remove();
        }
    }

    public void setCompleted(Long files, Long bytes) throws SevenZipException {
        
    }

    public void setTotal(Long files, Long bytes) throws SevenZipException {
        System.out.println(2);
    }

    public String cryptoGetTextPassword() throws SevenZipException {

        return password;
    }

    public void setLatestAccessedStream(RandomAccessStreaming extRandomAccessFileInStream) {
        if (extRandomAccessFileInStream != latestAccessedStream) {
            logger.info("Extract from: " + extRandomAccessFileInStream.getFilename());
            latestAccessedStream = extRandomAccessFileInStream;
        }
    }

    public RandomAccessStreaming getLatestAccessedStream() {
        return latestAccessedStream;
    }

    public void setReadyForExtract(boolean b) {
        readyForExtract = b;

    }

    public RandomAccessStreaming getPart1Stream() throws SevenZipException {
        return (RandomAccessStreaming) getStream(getArchive().getFirstArchiveFile());

    }

}