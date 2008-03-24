//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Download {
    public static final int           STATUS_INITIALIZED                     = 0;

    // Errorids unter 100 sind für DownloadLink reserviert
    public static final int           ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK = DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK;

    public static final int           ERROR_OUTPUTFILE_INVALID               = 100;

    public static final int           ERROR_OUTPUTFILE_ALREADYEXISTS         = DownloadLink.STATUS_ERROR_ALREADYEXISTS;

    public static final int           ERROR_CHUNK_INCOMPLETE                 = DownloadLink.STATUS_DOWNLOAD_INCOMPLETE;

    public static final int           ERROR_FILE_NOT_FOUND                   = DownloadLink.STATUS_ERROR_FILE_NOT_FOUND;

    public static final int           ERROR_SECURITY                         = DownloadLink.STATUS_ERROR_SECURITY;

    public static final int           ERROR_UNKNOWN                          = DownloadLink.STATUS_ERROR_UNKNOWN;

    public static final int           ERROR_COULD_NOT_RENAME                 = 101;

    public static final int           ERROR_ABORTED_BY_USER                  = 102;

    public static final int           ERROR_TOO_MUCH_BUFFERMEMORY            = 103;

    public static final int           ERROR_CHUNKLOAD_FAILED                 = DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED;

    public static final int           ERROR_NO_CONNECTION                    = 104;

    public static final int           ERROR_TIMEOUT_REACHED                  = 105;

    private static final int          ERROR_LOCAL_IO                         = 106;

    private DownloadLink              downloadLink;

    private HTTPConnection            connection;

    private int                       status                                 = STATUS_INITIALIZED;

    private int                       chunksDownloading                      = 0;

    private int                       chunkNum                               = 1;

    private int                       readTimeout                            = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 10000);

    private int                       requestTimeout                         = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 10000);

    private int                       chunksInProgress                       = 0;

    private Vector<Integer>           errors                                 = new Vector<Integer>();

    private Vector<Chunk>             chunks                                 = new Vector<Chunk>();

    private boolean                   resume                                 = false;

    private boolean                   speedLimited                           = true;

    private Plugin                    plugin;

    private int                       bytesLoaded                            = 0;

    private volatile Braf outputFile;

    private FileChannel               outputChannel;

    private int                       maxBytes;

    private long                      lastChunkSpeedCheck;

    private int                       lastMaxChunkSpeed;

    private long                      fileSize                               = -1;

    private boolean                   abortByError                           = false;

    private int                       preBytes                               = 0;

    public static Logger              logger                                 = JDUtilities.getLogger();

    public Download(Plugin plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        this.downloadLink = downloadLink;
        downloadLink.setDownloadInstance(this);
        this.connection = urlConnection;
        this.plugin = plugin;
    }

    public void setChunks(int num) {
        if (status != STATUS_INITIALIZED) {
            logger.severe("CHunks musst be set before starting download");
            return;
        }
        if (num <= 0) {
            logger.severe("Chunks value must be >=1");
            return;
        }
        this.chunkNum = num;
    }

    private void addBytes(int bytes) {
        this.bytesLoaded += bytes;

    }

    private synchronized void addBytes(ByteBuffer buffer, long currentBytePosition) {
        try {
            // currentBytePosition=-1;
            this.outputFile.seek(currentBytePosition);

            int length = buffer.limit() - buffer.position();
            // byte[] tmp = new byte[length];
            // logger.info("wrote " + length + " at " + currentBytePosition);
            // buffer.get(tmp, buffer.position(), length);
            outputChannel.write(buffer);
            // this.outputFile.write(tmp);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            error(ERROR_LOCAL_IO);
        }

    }

    public void setResume(boolean value) {
        this.resume = value;
    }

    public boolean isResume() {
        return resume;
    }

    private void error(int id) {
        logger.severe("Error occured: " + id);
        if (errors.indexOf(id) < 0) errors.add(id);
        if (id == ERROR_TOO_MUCH_BUFFERMEMORY) {
            terminate(id);

        }
        switch (id) {
            case ERROR_UNKNOWN:
            case ERROR_TIMEOUT_REACHED:
            case ERROR_FILE_NOT_FOUND:
            case ERROR_TOO_MUCH_BUFFERMEMORY:
            case ERROR_LOCAL_IO:
            case ERROR_NO_CONNECTION:
            case ERROR_SECURITY:
            case ERROR_CHUNKLOAD_FAILED:
                terminate(id);

        }

    }

    private void terminate(int id) {

        logger.severe("A critical Downloaderror occured. Terminate...");
        this.abortByError = true;

    }

    public boolean startDownload() {
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            error(ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            if (!handleErrors()) return false;
        }
        File fileOutput = new File(downloadLink.getFileOutput());
        if (fileOutput == null || fileOutput.getParentFile() == null) {
            error(ERROR_OUTPUTFILE_INVALID);
            if (!handleErrors()) return false;
        }
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }

        if (fileOutput.exists()) {

            logger.severe("File already exists. " + fileOutput);
            String todo = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_FILE_EXISTS, JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));

            if (!todo.equals(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"))) {

                if (new File(downloadLink.getFileOutput()).delete()) {
                }
                else {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                    error(ERROR_OUTPUTFILE_ALREADYEXISTS);
                    if (!handleErrors()) return false;
                }

            }

        }
        boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTSPPEDCHUNKS", true);
        if (correctChunks) {
            int maxSpeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            if (maxSpeed == 0) maxSpeed = Integer.MAX_VALUE;
            int allowedLinkSpeed = maxSpeed / Math.max(1, JDUtilities.getController().getRunningDownloadNum());
            // logger.finer("Linkspeed: " + allowedLinkSpeed);
            int tmp = Math.min(Math.max(1, allowedLinkSpeed / Chunk.MIN_CHUNKSPEED), chunkNum);
            if (tmp != chunkNum) {

                logger.finer("Corrected Chunknum: " + chunkNum + " -->" + tmp);
                chunkNum = tmp;
            }
        }
        File part = new File(downloadLink.getFileOutput() + ".part");

        try {
           // this.outputFile = new RandomAccessFile(part, "rwd");
            this.outputFile= new Braf(downloadLink.getFileOutput() + ".part", "rwd", 1024*1024*10);
            // outputFile.setLength(connection.getContentLength());
            outputChannel = outputFile.getChannel();

            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
            long fileSize = getFileSize();
            // logger.info("Filsize: " + fileSize);
            long parts = fileSize > 0 ? fileSize / chunkNum : -1;
            if (parts == -1) {
                logger.warning("Could not get Filesize.... reset chunks to 1");
                chunkNum = 1;
            }
            logger.finer("Start Download in " + chunkNum + " chunks. Chunksize: " + parts);
            downloadLink.setDownloadMax((int) fileSize);
            for (int i = 0; i < chunkNum; i++) {
                if (i == (chunkNum - 1)) {
                    addChunk(new Chunk(i * parts, fileSize, connection));
                }
                else {
                    addChunk(new Chunk(i * parts, (i + 1) * parts, connection));
                }

            }
            waitForChunks();
            logger.info("Errors: " + this.errors);
            outputFile.close();
            outputChannel.close();

            if (!part.renameTo(new File(downloadLink.getFileOutput()))) {
                logger.severe("Could not rename file " + fileOutput + " to " + downloadLink.getFileOutput());
                error(ERROR_COULD_NOT_RENAME);

                return false;
            }
            if (!handleErrors()) {

                return false;
            }

            return true;
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);
        return false;

    }

    private void getMissingRanges(File part) {

    }

    private long getFileSize() {
        if (connection.getContentLength() > 0) {
            // logger.info("1 " + connection.getHeaderFields());

            return connection.getContentLength();
        }
        if (fileSize > 0) {
            // logger.info("2");
            return fileSize;
        }
        if (downloadLink.getDownloadMax() > 0) {
            // logger.info("3");
            return downloadLink.getDownloadMax();

        }
        return -1;
    }

    public boolean handleErrors() {
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);

        if (errors.contains(ERROR_ABORTED_BY_USER)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_TODO);
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            return false;
        }
        if (errors.contains(ERROR_TOO_MUCH_BUFFERMEMORY)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED);
            return false;
        }
        if (errors.contains(ERROR_CHUNKLOAD_FAILED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED);

            return false;
        }

        if (errors.contains(ERROR_COULD_NOT_RENAME)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_RETRY);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
            return false;
        }
        if (errors.contains(ERROR_FILE_NOT_FOUND)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
            return false;
        }
        if (errors.contains(ERROR_OUTPUTFILE_ALREADYEXISTS)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
            return false;
        }

        if (errors.contains(ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            return false;
        }

        if (errors.contains(ERROR_SECURITY)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_SECURITY);
            return false;
        }

        if (errors.contains(ERROR_UNKNOWN)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        }
        if (errors.contains(ERROR_TIMEOUT_REACHED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NOCONNECTION);
            return false;
        }
        if (abortByError) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        }
        plugin.getCurrentStep().setStatus(PluginStep.STATUS_DONE);
        // downloadLink.setStatus(DownloadLink.STATUS_DONE);
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);
        return true;
    }

    private void waitForChunks() {

        while (chunksInProgress > 0) {
            try {
                Thread.sleep(1000);

            }
            catch (InterruptedException e) {
            }

            downloadLink.setDownloadCurrent(this.bytesLoaded);
            JDUtilities.getController().requestDownloadLinkUpdate(downloadLink);
            // logger.info("UüdatebytesLoaded " + bytesLoaded);
            assignChunkSpeeds();

        }

    }

    private void assignChunkSpeeds() {
        int MAX_ALLOWED_OVERHEAD = 10 * 1024;
        int allowedLinkSpeed = downloadLink.getMaximalspeed();
        int mChunk = (int) ((allowedLinkSpeed / chunkNum) * 0.4);
        int currentSpeed = 0;
        Chunk next;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    currentSpeed += next.bytesPerSecond;
                }

            }
            int overhead = allowedLinkSpeed - currentSpeed;
            if (Math.abs(overhead) < MAX_ALLOWED_OVERHEAD) return;
            // logger.info("Speed: "+currentSpeed+" overhead_: "+overhead);
            it = chunks.iterator();
            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    next.setMaximalSpeed(Math.max(mChunk, (int) next.bytesPerSecond + overhead / getRunningChunks()));
                }
            }
        }

    }

    private void addChunk(Chunk chunk) {
        this.chunks.add(chunk);
        chunksInProgress++;

        if (chunkNum == 1) {
            chunk.start();
        }
        else {
            chunk.start();
        }

    }

    //
    // private int getMaximalChunkSpeed() {
    // // if (System.currentTimeMillis() < (lastChunkSpeedCheck + 1000)) return
    // // lastMaxChunkSpeed;
    // // this.lastChunkSpeedCheck = System.currentTimeMillis();
    // int allowedLinkSpeed = downloadLink.getMaximalspeed();
    //        
    // int chunkSpeed = allowedLinkSpeed / getRunningChunks();
    // // logger.info("Allowedperchunk "+chunkSpeed+"/"+allowedLinkSpeed);
    // synchronized (chunks) {
    // Iterator<Chunk> it = chunks.iterator();
    // Chunk next;
    //
    // int currentTotalspeed = 0;
    // while (it.hasNext()) {
    // next = it.next();
    // if (next.isAlive()) {
    //
    // currentTotalspeed += next.getBytesPerSecond() < 0 ? chunkSpeed :
    // next.getBytesPerSecond();
    //
    // }
    // }
    //
    // // logger.info("Total speed= "+currentTotalspeed);
    // // logger.info("Max Chunkspeed:
    // //
    // "+(chunkSpeed+(allowedLinkSpeed-currentTotalspeed)/this.getRunningChunks()));
    // this.lastMaxChunkSpeed = chunkSpeed + (allowedLinkSpeed -
    // currentTotalspeed) / this.getRunningChunks();
    // // logger.info("Chunk" +lastMaxChunkSpeed);
    // }
    // return lastMaxChunkSpeed;
    //
    // }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    class Chunk extends Thread {
        // Wird durch die speedbegrenzung ein chunk uter diesen wert geregelt,
        // so wird er weggelassen.
        // sehr niedrig geregelte chunks haben einen kleinen buffer und eine
        // sehr hohe intervalzeit.
        // Das führt zu verstärkt intervalartigem laden und ist ungewünscht
        public static final int  MIN_CHUNKSPEED = 120 * 1024;

        private static final int MAX_BUFFERSIZE = 6 * 1024 * 1024;

        private static final int MIN_BUFFERSIZE = 1024;

        private long             startByte;

        private long             endByte;

        private HTTPConnection   connection;

        private long             currentBytePosition;

        private long             bytesPerSecond = -1;

        private double           bufferTimeFaktor;

        private long             desiredBps;

        private int              maxSpeed;

        public Chunk(long startByte, long endByte, HTTPConnection connection) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.connection = connection;
            if (this.startByte == 0 && preBytes > 0 && chunkNum > 1) {
                this.startByte = preBytes;
                this.loadStartBytes(preBytes);
            }
            currentBytePosition = this.startByte;
            if (startByte >= endByte && endByte > 0) {
                logger.severe("Startbyte has to be less than endByte");
            }
        }

        public void setMaximalSpeed(int i) {
            maxSpeed = i;
            // logger.info(chunks.indexOf(this)+" chunkspeed: "+i);

        }

        public int getMaximalSpeed() {
            if (this.maxSpeed <= 0) this.maxSpeed = downloadLink.getMaximalspeed() / getRunningChunks();

            return Math.min(maxSpeed, (int) (this.desiredBps * 1.5));
        }

        private void loadStartBytes(int preBytes) {

            try {
                ByteBuffer buffer = ByteBuffer.allocateDirect(preBytes);
                byte[] b = new byte[preBytes];
                InputStream inputStream = connection.getInputStream();
                int i = 0;
                while (preBytes > 0) {
                    int tmp = inputStream.read(b, i, preBytes);
                    i += tmp;
                    preBytes -= tmp;
                    logger.info("Preloaded " + i + " bytes " + new String(b));

                }
                logger.info("Preloading produced " + inputStream.available() + " bytes overhead");
                inputStream.close();
                connection.getHTTPURLConnection().disconnect();
                buffer.put(b);
                buffer.flip();
                addBytes(buffer, 0);
                addBytes(i);

            }
            catch (IOException e) {
                error(ERROR_CHUNKLOAD_FAILED);
                e.printStackTrace();
            }

        }

        private HTTPConnection copyConnection(HTTPConnection connection) {

            try {
                URL link = connection.getURL();
                HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
                httpConnection.setReadTimeout(getReadTimeout());
                httpConnection.setConnectTimeout(getRequestTimeout());
                httpConnection.setInstanceFollowRedirects(false);
                Map<String, List<String>> request = connection.getRequestProperties();
                if (request != null) {
                    Set<Entry<String, List<String>>> requestEntries = request.entrySet();
                    Iterator<Entry<String, List<String>>> it = requestEntries.iterator();
                    String value;
                    while (it.hasNext()) {
                        Entry<String, List<String>> next = it.next();

                        value = next.getValue().toString();
                        httpConnection.setRequestProperty(next.getKey(), value.substring(1, value.length() - 1));
                    }
                }

                if (chunkNum > 1) {
                    httpConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                    // logger.info(chunks.indexOf(this) + " - " +
                    // httpConnection.getRequestProperties() + "");
                }
                if (connection.getHTTPURLConnection().getDoOutput()) {
                    httpConnection.setDoOutput(true);
                    httpConnection.connect();
                    httpConnection.post(connection.getPostData());
                    httpConnection.getHTTPURLConnection();

                }
                else {
                    httpConnection.connect();
                }
                logger.info("ChunkHeaders: " + httpConnection.getHeaderFields());

                return httpConnection;

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public void run() {

            logger.finer("Start Chunk " + startByte + " - " + endByte);
            if (chunkNum > 1) this.connection = copyConnection(connection);
            if (connection == null) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));
                chunksInProgress--;
                return;
            }

            if (chunkNum > 1 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk " + chunks.indexOf(this));
                chunksInProgress--;
                return;

            }
            // Content-Range=[133333332-199999999/200000000]}

            String[] range = Plugin.getSimpleMatches("[" + connection.getHeaderField("Content-Range") + "]", "[°-°/°]");
            logger.info("Range Header " + connection.getHeaderField("Content-Range"));
            if (range == null && chunkNum > 1) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk " + chunks.indexOf(this));
                chunksInProgress--;
                return;

            }
            else if (range != null) {

                this.startByte = JDUtilities.filterInt(range[0]);
                this.endByte = JDUtilities.filterInt(range[1]);

                logger.finer("Resulting Range" + startByte + " - " + endByte);
            }
            logger.finer("Start Chunk " + chunks.indexOf(this));
            chunksDownloading++;
            download();
            this.bytesPerSecond = 0;
            this.desiredBps = 0;
            chunksDownloading--;
            logger.finer("END Chunk " + chunks.indexOf(this));

            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            logger.finer("Chunk finished " + getBytesLoaded());
            chunksInProgress--;

        }

        private int getTimeInterval() {
            if (!downloadLink.isLimited()) return 2000;

            return Math.min(5000, (int) (2000 * this.bufferTimeFaktor));

        }

        /**
         * Gibt zurück ob der chunk von einem externen eregniss unterbrochen
         * wurde
         * 
         * @return
         */
        private boolean isExternalyAborted() {
            return plugin.aborted || abortByError || downloadLink.isAborted();
        }

        private void download() {
            ByteBuffer buffer = null;
            int bufferSize = 1;
            try {
                bufferSize = getBufferSize(getMaximalSpeed());

                // logger.info(bufferSize+" - "+this.getTimeInterval());
                buffer = ByteBuffer.allocateDirect(bufferSize);

            }
            catch (OutOfMemoryError e) {
                error(ERROR_TOO_MUCH_BUFFERMEMORY);
                return;
            }

            InputStream inputStream = null;
            ReadableByteChannel source = null;

            try {

                connection.setReadTimeout(getReadTimeout());
                connection.setConnectTimeout(getRequestTimeout());

                inputStream = connection.getInputStream();
                source = Channels.newChannel(inputStream);

                buffer.clear();

                long deltaTime;
                long timer;

                int bytes;
                int block = 0;
                int tempBuff = 0;
                long addWait;
                byte b[] = new byte[1];
                int read = 0;

                while (!isExternalyAborted()) {
                    bytes = 0;

                    timer = System.currentTimeMillis();

                    while (buffer.hasRemaining() && !isExternalyAborted() && (System.currentTimeMillis() - timer) < getTimeInterval()) {
                        block = 0;

                        // Prüft ob bytes zum Lesen anliegen.
                        if (inputStream.available() > 0) {
                            // kann den connectiontimeout nicht auswerten
                            block = source.read(buffer);
                        }
                        else {

                            // logger.info(""+inputStream.getClass());

                            // wertet den Timeout der connection aus
                            // (HTTPInputStream)
                            read = inputStream.read(b, 0, 1);
                            if (read > 0) {
                                buffer.put(b);
                                block = 1;
                                // Pause falls das Ende nicht erreicht worden
                                // ist. Die Schelife läuft zu schnell
                                // logger.info("Pause");
                                // Thread.sleep(25);
                            }
                            else if (read < 0) {
                                block = -1;
                                break;
                            }
                        }

                        if (block == -1) {
                            break;
                        }
                        addBytes(block);

                        bytes += block;
                    }

                    if (block == -1 && bytes == 0) break;
                    buffer.flip();
                    addBytes(buffer, currentBytePosition);

                    buffer.clear();
                    currentBytePosition += bytes;

                    if (block == -1 || isExternalyAborted()) break;
                    /*
                     * War der download des buffers zu schnell, wird heir eine
                     * pause eingelegt
                     */
                    deltaTime = Math.max(System.currentTimeMillis() - timer, 1);
                    desiredBps = (1000 * bytes) / deltaTime;
                    tempBuff = getBufferSize(getMaximalSpeed());
                    if (Math.abs(bufferSize - tempBuff) > 1000) {
                        bufferSize = tempBuff;
                        try {
                            buffer = ByteBuffer.allocateDirect(Math.max(1, bufferSize));

                        }
                        catch (Exception e) {
                            error(ERROR_TOO_MUCH_BUFFERMEMORY);
                            return;
                        }

                        buffer.clear();
                    }
                    try {
                        // 0.995 ist eine Anpassung an die Zeit, die die
                        // unerfasste Schleife noch frisst. das macht am ende
                        // ein paar wenige bytes/sekunde in der speederfassung
                        // aus.
                        addWait = (long) (0.995 * (getTimeInterval() - (System.currentTimeMillis() - timer)));
                        // logger.info("Wait " + addWait);
                        if (addWait > 0) Thread.sleep(addWait);
                    }
                    catch (Exception e) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;

                    this.bytesPerSecond = (1000 * bytes) / deltaTime;
                    updateSpeed();

                    // logger.info(downloadLink.getSpeedMeter().getSpeed() +
                    // "loaded" + bytes + " b in " + (deltaTime) + " ms:// " +
                    // bytesPerSecond + "(" + desiredBps + ") ");

                }
                if (currentBytePosition < endByte && endByte > 0) {

                    inputStream.close();
                    source.close();

                    logger.info(" incomplete download: bytes loaded: " + currentBytePosition + "/" + endByte);
                    error(ERROR_CHUNK_INCOMPLETE);
                }

                inputStream.close();
                source.close();

            }
            catch (FileNotFoundException e) {
                logger.severe("file not found. " + e.getLocalizedMessage());
                error(ERROR_FILE_NOT_FOUND);
            }
            catch (SecurityException e) {
                logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
                error(ERROR_SECURITY);

            }
            catch (IOException e) {
                if (e.getMessage().indexOf("timed out") >= 0) {
                    error(ERROR_TIMEOUT_REACHED);
                    ;

                }
                else {
                    logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
                    error(ERROR_SECURITY);
                }
            }
            catch (Exception e) {

                e.printStackTrace();
                error(ERROR_UNKNOWN);
            }

            try {
                if (inputStream != null) inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (source != null) source.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * Schränkt die Buffergröße ein.
         * 
         * @param maxspeed
         * @return
         */
        private int getBufferSize(int maxspeed) {
            if (!downloadLink.isLimited()) return MAX_BUFFERSIZE;
            int max = Math.max(MIN_BUFFERSIZE, maxspeed);
            int bufferSize = Math.min(MAX_BUFFERSIZE, max);
            // logger.info(MIN_BUFFERSIZE+"<>"+maxspeed+"-"+MAX_BUFFERSIZE+"><"+max);
            this.bufferTimeFaktor = Math.max(0.1, (double) bufferSize / maxspeed);
            // logger.info("Maxspeed= " + maxspeed + " buffer=" + bufferSize +
            // "time: " + getTimeInterval());
            return bufferSize;
        }

        public long getBytesPerSecond() {
            return bytesPerSecond;
        }

        /**
         * Gibt eine Abschätzung zurück wie schnell der Chunk laden könnte wenn
         * man ihn nicht bremsen würde.
         * 
         * @return
         */
        public long getDesiredBps() {
            return desiredBps;
        }

    }

    // public boolean isSpeedLimited() {
    // return speedLimited && downloadLink.isLimited();
    // }

    public void updateSpeed() {
        int speed = 0;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext())
                speed += it.next().bytesPerSecond;
        }

        downloadLink.addSpeedValue(speed);

    }

    // public void setSpeedLimited(boolean speedLimited) {
    // this.speedLimited = speedLimited;
    // }

    public Vector<Integer> getErrors() {
        return errors;
    }

    public int getChunks() {

        return chunkNum;
    }

    public int getRunningChunks() {
        return this.chunksInProgress;
    }

    public int getChunksDownloading() {
        return chunksDownloading;
    }

    public void setMaxBytesToLoad(int integerProperty) {
        this.maxBytes = integerProperty;

    }

    public int getBytesLoaded() {
        return bytesLoaded;
    }

    public void setFilesize(long length) {
        this.fileSize = length;

    }

    /**
     * Machne Hoster wollen das resumen erlauben, aber chunkload verbieten.
     * Deshalb akzeptieren sie keine range:0-** Um Trotzdem Chunkload nutzen zu
     * können werden die ersten bytes normal geladen. Und der rest normal über
     * chunks.
     * 
     * @param i
     */
    public void setLoadPreBytes(int i) {
        this.preBytes = i;

    }

    public class Braf extends RandomAccessFile {
        byte        buffer[];

        int         buf_end  = 0;

        int         buf_pos  = 0;

        long        real_pos = 0;

        private int BUF_SIZE = 1024 * 1024 * 6;

        public Braf(String filename, String mode, int bufsize) throws IOException {
            super(filename, mode);
            invalidate();
            BUF_SIZE = bufsize;
            buffer = new byte[BUF_SIZE];
        }

        public final int read() throws IOException {
            if (buf_pos >= buf_end) {
                if (fillBuffer() < 0) return -1;
            }
            if (buf_end == 0) {
                return -1;
            }
            else {
                return buffer[buf_pos++];
            }
        }

        private int fillBuffer() throws IOException {
            int n = super.read(buffer, 0, BUF_SIZE);
            if (n >= 0) {
                real_pos += n;
                buf_end = n;
                buf_pos = 0;
            }
            return n;
        }

        private void invalidate() throws IOException {
            buf_end = 0;
            buf_pos = 0;
            real_pos = super.getFilePointer();
        }

        public int read(byte b[], int off, int len) throws IOException {
            int leftover = buf_end - buf_pos;
            if (len <= leftover) {
                System.arraycopy(buffer, buf_pos, b, off, len);
                buf_pos += len;
                return len;
            }
            for (int i = 0; i < len; i++) {
                int c = this.read();
                if (c != -1)
                    b[off + i] = (byte) c;
                else {
                    return i;
                }
            }
            return len;
        }

        public long getFilePointer() throws IOException {
            long l = real_pos;
            return (l - buf_end + buf_pos);
        }

        public void seek(long pos) throws IOException {
            int n = (int) (real_pos - pos);
            if (n >= 0 && n <= buf_end) {
                buf_pos = buf_end - n;
            }
            else {
                super.seek(pos);
                invalidate();
            }
        }

        /**
         * return a next line in String
         */
        public final String getNextLine() throws IOException {
            String str = null;
            if (buf_end - buf_pos <= 0) {
                if (fillBuffer() < 0) {
                    throw new IOException("error in filling buffer!");
                }
            }
            int lineend = -1;
            for (int i = buf_pos; i < buf_end; i++) {
                if (buffer[i] == '\n') {
                    lineend = i;
                    break;
                }
            }
            if (lineend < 0) {
                StringBuffer input = new StringBuffer(256);
                int c;
                while (((c = read()) != -1) && (c != '\n')) {
                    input.append((char) c);
                }
                if ((c == -1) && (input.length() == 0)) {
                    return null;
                }
                return input.toString();
            }
            if (lineend > 0 && buffer[lineend - 1] == '\r')
                str = new String(buffer, 0, buf_pos, lineend - buf_pos - 1);
            else
                str = new String(buffer, 0, buf_pos, lineend - buf_pos);
            buf_pos = lineend + 1;
            return str;
        }
    }

}
