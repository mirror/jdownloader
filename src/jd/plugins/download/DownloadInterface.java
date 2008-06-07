//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

abstract public class DownloadInterface {
    public static final int STATUS_INITIALIZED = 0;

    // Errorids unter 100 sind für DownloadLink reserviert
    public static final int ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK = DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK;

    public static final int ERROR_OUTPUTFILE_INVALID = 100;

    public static final int ERROR_OUTPUTFILE_ALREADYEXISTS = DownloadLink.STATUS_ERROR_ALREADYEXISTS;

    public static final int ERROR_CHUNK_INCOMPLETE = DownloadLink.STATUS_DOWNLOAD_INCOMPLETE;

    public static final int ERROR_FILE_NOT_FOUND = DownloadLink.STATUS_ERROR_FILE_NOT_FOUND;

    public static final int ERROR_SECURITY = DownloadLink.STATUS_ERROR_SECURITY;

    public static final int ERROR_UNKNOWN = DownloadLink.STATUS_ERROR_UNKNOWN;

    public static final int ERROR_COULD_NOT_RENAME = 101;

    public static final int ERROR_ABORTED_BY_USER = 102;

    public static final int ERROR_TOO_MUCH_BUFFERMEMORY = 103;

    public static final int ERROR_CHUNKLOAD_FAILED = DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED;

    public static final int ERROR_NO_CONNECTION = 104;

    public static final int ERROR_TIMEOUT_REACHED = 105;

    public static final int ERROR_LOCAL_IO = 106;

    public static final int ERROR_NIBBLE_LIMIT_REACHED = 107;
    public static final int ERROR_CRC = 108;

    protected DownloadLink downloadLink;

    protected HTTPConnection connection;

    private int status = STATUS_INITIALIZED;

    private int chunksDownloading = 0;

    protected int chunkNum = 1;

    private int readTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 20000);

    private int requestTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 20000);

    private int chunksInProgress = 0;

    private Vector<Integer> errors = new Vector<Integer>();

    private Vector<Chunk> chunks = new Vector<Chunk>();

    private boolean resume = false;

    protected PluginForHost plugin;

    protected int totaleLinkBytesLoaded = 0;

    protected int maxBytes = -1;

    protected long fileSize = -1;

    private boolean abortByError = false;

    private int preBytes = 0;

    protected boolean speedDebug = false;

    private Vector<Exception> exceptions = null;

    // private int totalLoadedBytes = 0;

    private boolean aborted = false;

    private boolean waitFlag = true;

    public static Logger logger = JDUtilities.getLogger();

    public DownloadInterface(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        this.downloadLink = downloadLink;
        downloadLink.setDownloadInstance(this);
        this.connection = urlConnection;
        this.plugin = plugin;
    }

    /**
     * Gibt die Anzahl der Chunks an die dieser Download verwenden soll. Chu8nks
     * können nur vor dem Downloadstart gesetzt werden!
     * 
     * @param num
     */
    public void setChunkNum(int num) {
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

    /**
     * Schreibt den puffer eines chunks in die zugehörige Datei
     * 
     * @param buffer
     * @param currentBytePosition
     */

    abstract protected void writeChunkBytes(Chunk chunk);

    protected void writeBytes(Chunk chunk) {
        
        writeChunkBytes(chunk);
      
        if (maxBytes > 0 && getChunkNum() == 1 && this.totaleLinkBytesLoaded >= maxBytes) {
            error(ERROR_NIBBLE_LIMIT_REACHED);
        }
        if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.getCurrentBytesPosition() - 1;

        // 152857135
        // logger.info("Bytes " + totalLoadedBytes);
    }

    /**
     * File soll resumed werden
     * 
     * @param value
     */
    public void setResume(boolean value) {
        this.resume = value;
    }

    /**
     * Ist resume aktiv?
     * 
     * @return
     */
    public boolean isResume() {
        return resume;
    }

    /**
     * über error() kann ein fehler gemeldet werden. DIe Methode entscheided
     * dann ob dieser fehler zu einem Abbruch führen muss
     * 
     * @param id
     */
    protected void error(int id) {
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
        case ERROR_NIBBLE_LIMIT_REACHED:
            terminate(id);

        }

    }

    /**
     * Bricht den Download komplett ab.
     * 
     * @param id
     */
    private void terminate(int id) {

        logger.severe("A critical Downloaderror occured. Terminate...");
        this.abortByError = true;

    }

    /**
     * Startet den Download. Nach dem Aufruf dieser Funktion können keine
     * Downlaodparameter mehr gesetzt werden bzw bleiben wirkungslos.
     * 
     * @return
     */
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
                    logger.severe("--->Overwritten");
                } else {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                    error(ERROR_OUTPUTFILE_ALREADYEXISTS);
                    if (!handleErrors()) return false;
                }

            } else {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                error(ERROR_OUTPUTFILE_ALREADYEXISTS);
                if (!handleErrors()) return false;
            }

        }

        if (this.maxBytes > 0) {
            logger.finer("Nibble feature active: " + maxBytes + " rest chunks to 1");
            chunkNum = 1;
        }
        try {
            this.setupChunks();
            waitForChunks();

            this.onChunksReady();

            if (!handleErrors()) {

                return false;
            } else {

                return true;
            }
        }

        catch (Exception e) {
            handleErrors();
            if (plugin.getCurrentStep().getStatus() != PluginStep.STATUS_ERROR) {
                // e.printStackTrace();
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);

                plugin.getCurrentStep().setParameter(JDUtilities.convertExceptionReadable(e));
                plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);

            }
            return false;
        }

    }

    /**
     * Wird aufgerufen sobald alle Chunks fertig geladen sind
     * 
     * @throws DownloadFailedException
     */
    abstract protected void onChunksReady();

    /**
     * Wird aufgerufen um die Chunks zu initialisieren
     * 
     * @throws IOException
     * 
     */
    abstract protected void setupChunks();

    /**
     * Gibt eine bestmögliche abschätzung der Dateigröße zurück
     * 
     * @return
     */
    protected long getFileSize() {
        if (connection.getContentLength() > 0) {

        return connection.getContentLength(); }
        if (fileSize > 0) {

        return fileSize; }
        if (downloadLink.getDownloadMax() > 0) {

        return downloadLink.getDownloadMax();

        }
        return -1;
    }

    /**
     * Setzt im Downloadlink und PLugin die entsprechende Fehlerids
     * 
     * @return
     */
    public boolean handleErrors() {

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

        if (errors.contains(ERROR_TIMEOUT_REACHED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NOCONNECTION);
            return false;
        }
        if (errors.contains(ERROR_LOCAL_IO)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatusText(JDLocale.L("download.error.message.io", "Schreibfehler"));

            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            return false;
        }
        if (errors.contains(ERROR_NIBBLE_LIMIT_REACHED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setEnabled(false);
            downloadLink.setStatusText(String.format(JDLocale.L("download.error.message.nibble", "Nibbling aborted after %s bytes"), "" + this.maxBytes));
            return false;
        }
        if (errors.contains(ERROR_CRC)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            // downloadLink.setEnabled(false);
            downloadLink.setStatusText(JDLocale.L("download.error.message.crc", "Falsche Checksum"));
            return false;
        }

        if (exceptions != null) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setStatusText(JDLocale.L("download.error.message.exception", "Ausnahmefehler: ") + JDUtilities.convertExceptionReadable(exceptions.get(0)));
            return false;
        }
        if (errors.contains(ERROR_UNKNOWN)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            return false;
        }
        if (abortByError) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            return false;
        }
        if (errors.contains(ERROR_CHUNK_INCOMPLETE)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setStatusText(JDLocale.L("download.error.message.chunk_incomplete", "Chunk(s) incomplete"));
            return false;
        }
        if (this.totaleLinkBytesLoaded != this.fileSize && fileSize > 0) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setStatusText(JDLocale.L("download.error.message.incomplete", "Download unvollständig"));
            return false;
        }

        if (getExceptions() != null && getExceptions().size() > 0) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setStatusText(JDUtilities.convertExceptionReadable(getExceptions().firstElement()));
            return false;
        }
        plugin.getCurrentStep().setStatus(PluginStep.STATUS_DONE);
        // downloadLink.setStatus(DownloadLink.STATUS_DONE);
        downloadLink.setStatus(DownloadLink.STATUS_DONE);
        return true;
    }

    /**
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink
     * regelmäsig und fordert beim Controller eine aktualisierung des links an
     */
    private void onChunkFinished() {
        synchronized (this) {
            if (this.waitFlag) {
                this.waitFlag = false;
                this.notify();
            }

        }
    }

    private void waitForChunks() {
        int i = 0;
        int interval = 150;
        while (chunksInProgress > 0) {
            synchronized (this) {

                if (waitFlag) {
                    try {
                        this.wait(interval);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            i++;
            waitFlag = true;
            // checkChunkParts();
            downloadLink.setDownloadCurrent(this.totaleLinkBytesLoaded);
            downloadLink.requestGuiUpdate();
            if (i == 1000 / interval) {
             
                assignChunkSpeeds();
                
                i = 0;
            }

        }

    }

    /**
     * Verteilt den verfügbraen downloadspeed auf die Chunks
     */
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

            it = chunks.iterator();
          
            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    next.checkTimeout(120000);
                    next.setMaximalSpeed(Math.max(mChunk, (int) next.bytesPerSecond + overhead / Math.max(1, getRunningChunks())));
                }

            }
            
        }
      

    }

    /**
     * Fügt einen Chunk hinzu und startet diesen
     * 
     * @param chunk
     */
    protected void addChunk(Chunk chunk) {
        this.chunks.add(chunk);

        if (chunkNum == 1) {
            chunk.startChunk();
        } else {
            chunk.startChunk();
        }

    }

    /**
     * Gibt den aktuellen readtimeout zurück
     * 
     * @return
     */
    public int getReadTimeout() {
        return Math.max(10000, readTimeout);
    }

    protected void addException(Exception e) {
        if (exceptions == null) exceptions = new Vector<Exception>();
        exceptions.add(e);
    }

    /**
     * Setzt den aktuellen readtimeout(nur vor dem dl start)
     * 
     * @param readTimeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Gibt den requesttimeout zurück
     * 
     * @return
     */
    public int getRequestTimeout() {
        return Math.max(10000, requestTimeout);
    }

    /**
     * Setzt vor ! dem download dden requesttimeout. Sollte nicht zu niedrig
     * sein weil sonst das automatische kopieren der Connections fehl schlägt.,
     * 
     * @param requestTimeout
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Addiert alle chunkspeeds und giubt diese an den downloadlink weiter
     */
    public void updateSpeed() {
        int speed = 0;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext())
                speed += it.next().bytesPerSecond;
        }

        downloadLink.addSpeedValue(speed);

    }

    /**
     * Gibt die aufgetretenen Fehler zurück
     * 
     * @return
     */

    public Vector<Integer> getErrors() {
        return errors;
    }

    /**
     * Gibt die Anzahl der verwendeten Chunks zurück
     * 
     * @return
     */
    public int getChunkNum() {

        return chunkNum;
    }

    /**
     * Gibt zurück wieviele Chunks gerade am arbeiten sind
     * 
     * @return
     */
    public int getRunningChunks() {
        return this.chunksInProgress;
    }

    // public int checkChunkParts() {
    // int total = 0;
    // int overhead = 0;
    // int loaded = 0;
    // Chunk lastChunk = null;
    // for (Chunk chunk : chunks) {
    // total += chunk.getChunkSize();
    // loaded += chunk.getBytesLoaded();
    // // logger.info("Chunk "+chunk.getID()+" :
    // //
    // "+"("+chunk.loaded+"|"+chunk.getBytesLoaded()+")/"+chunk.getChunkSize());
    // if (lastChunk == null) {
    // if (chunk.preBytes > 0) {
    // if (chunk.startByte != 0) {
    // logger.severe("First Chunk does not Start at 0");
    // overhead += 0 - chunk.startByte;
    // } else if (chunk.startByte != chunk.preBytes) {
    // logger.severe("PreBytes: " + chunk.preBytes + " First Chunk does not
    // Start at " + chunk.preBytes);
    // overhead += chunk.preBytes - chunk.startByte;
    // }
    // } else {
    // // chunk OK
    // }
    // } else {
    // if (chunk.startByte != lastChunk.endByte + 1) {
    // logger.severe("Chunk " + chunk.getID() + " should start at " +
    // (lastChunk.endByte + 1) + " but starts at " + chunk.startByte);
    // overhead += lastChunk.endByte + 1 - chunk.startByte;
    // } else {
    // // ok
    // }
    // }
    // lastChunk = chunk;
    //
    // }
    // if (loaded > this.bytesLoaded) {
    // logger.severe("COunt error. loaded Bytes are " + loaded + " counted: " +
    // this.bytesLoaded);
    //
    // }
    // if (lastChunk.endByte != fileSize - 1 && lastChunk.endByte != -1) {
    // logger.severe("last Chunk " + lastChunk.getID() + " Should end at " +
    // (fileSize - 1) + " But ends at " + lastChunk.endByte);
    // overhead += lastChunk.endByte - (fileSize - 1);
    // }
    // if (total != fileSize) {
    // logger.severe("Total Chunks Size should be " + fileSize + " but is " +
    // total);
    // }
    // return overhead;
    // }

    /**
     * Gibt zurüc wieviele Chunks tatsächlich in der Downloadphase sind
     * 
     * @return
     */
    public int getChunksDownloading() {
        return chunksDownloading;
    }

    private int getPreBytes(Chunk chunk) {
        if (chunk.getID() != 0 || chunk.startByte > 0) return 0;
        return preBytes;
    }

    /**
     * Wird hier ein wert >0 Angegeben läd die instanz nur bis zu dieser
     * byteanzahl
     * 
     * @param integerProperty
     */
    public void setMaxBytesToLoad(int integerProperty) {
        this.maxBytes = integerProperty;

    }

    /**
     * Setzt die filesize.
     * 
     * @param length
     */
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

    /**
     * Chunk Klasse verwaltet eine einzellne Downloadverbindung.
     * 
     * @author coalado
     * 
     */
    public class Chunk extends Thread {
        // Wird durch die speedbegrenzung ein chunk uter diesen wert geregelt,
        // so wird er weggelassen.
        // sehr niedrig geregelte chunks haben einen kleinen buffer und eine
        // sehr hohe intervalzeit.
        // Das führt zu verstärkt intervalartigem laden und ist ungewünscht
        public static final int MIN_CHUNKSIZE = 1 * 1024 * 1024;

        private long MAX_BUFFERSIZE = 4 * 1024 * 1024;

        private static final long MIN_BUFFERSIZE = 1024;

        private static final int TIME_BASE = 2000;

        private long startByte;

        private long endByte;

        private HTTPConnection connection;

        private long bytesPerSecond = -1;

        private double bufferTimeFaktor;

        private long desiredBps;

        private int maxSpeed;

        ByteBuffer buffer;

        private int id = -1;

        // private int preBytes = -1;

        private int totalPartBytes = 0;

        private int chunkBytesLoaded = 0;

        private long blockStart = 0;

        private InputStream inputStream;

        private ReadableByteChannel source;

        /**
         * die connection wird entsprechend der start und endbytes neu
         * aufgebaut.
         * 
         * @param startByte
         * @param endByte
         * @param connection
         */
        public Chunk(long startByte, long endByte, HTTPConnection connection) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.connection = connection;
            this.setPriority(Thread.MIN_PRIORITY);
            MAX_BUFFERSIZE=JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty("MAX_BUFFER_SIZE", 4)*1024*1024l;

        }

        public void checkTimeout(long timeout) {
          long timer = blockStart;
            if (interrupted() || !this.isAlive()) return;
//            try {
//                if (this.inputStream.available() > 0) {
//                    blockStart = -1;
//                }
//            } catch (IOException e) {
//            }
            if (isExternalyAborted()) {
                this.interrupt();
            }
            if (timer <= 0) return;
            long dif = System.currentTimeMillis() - timer;
            //logger.info(this + " " + dif);
            if (dif >= timeout) {
                logger.severe("Timeout or termination detected: interrupt: " + timeout + " - " + dif+" - "+timer);
                this.interrupt();

            } else if (dif >= 5000) {
                downloadLink.setStatusText(JDLocale.L("download.connection.idle", "Idle"));
                downloadLink.requestGuiUpdate();
            } else {
                downloadLink.setStatusText(JDLocale.L("download.connection.normal", "Download"));
            }

        }

        public void startChunk() {

            this.start();

        }

        /**
         * Gibt die Geladenen ChunkBytes zurück
         * 
         * @return
         */
        public int getBytesLoaded() {
            return (int) (this.getCurrentBytesPosition() - this.startByte);
        }

        /**
         * Gibt die Aktuelle Endposition in der gesamtfile zurück. Diese Methode
         * gibt die Endposition unahängig davon an Ob der aktuelle BUffer schon
         * geschrieben wurde oder nicht.
         * 
         * @return
         */
        long getCurrentBytesPosition() {

            return this.startByte + chunkBytesLoaded;
        }

        public void finalize() {
            if (speedDebug) logger.finer("Finalized: " + downloadLink + " : " + this.getID());
            buffer = null;
            System.gc();
            System.runFinalization();

        }

        public int getChunkSize() {
            return (int) (endByte - startByte + 1);
        }

        public int getID() {
            if (id < 0) {
                if (speedDebug) logger.finer("INIT " + chunks.indexOf(this));
                id = chunks.indexOf(this);
            }
            return id;
        }

        /**
         * Gibt dem Chunk sein speedlimit vor. der chunk versucht sich an dieser
         * Grenze einzuregeln
         * 
         * @param i
         */
        public void setMaximalSpeed(int i) {
            maxSpeed = i;

        }

        /**
         * Gibt die Speedgrenze an.
         * 
         * @return
         */
        public int getMaximalSpeed() {
            try {
                if (this.maxSpeed <= 0) {
                    this.maxSpeed = downloadLink.getMaximalspeed() / getRunningChunks();
                    if (speedDebug) logger.finer("Def speed: " + downloadLink.getMaximalspeed() + "/" + getRunningChunks() + "=" + maxSpeed);

                }
                if (speedDebug) logger.finer("return speed: min " + maxSpeed + " - " + (this.desiredBps * 1.5));
                if (desiredBps < 1024) return maxSpeed;
                return Math.min(maxSpeed, (int) (this.desiredBps * 1.3));
            } catch (Exception e) {
                addException(e);
                error(ERROR_UNKNOWN);
            }
            return 0;
        }

        /**
         * Einige Anbieter erlauben das resumen von files, aber nicht
         * multistreamloading. Dazu verbieten sie die range 0-xxx. Um das zu
         * umgehen werden die ersten bytes via preloading geladen und der erste
         * chunk fängt bei 1-xxx an
         * 
         * @param preBytes
         */
        public int loadPreBytes() {

            try {

                InputStream inputStream = connection.getInputStream();

                if (inputStream.available() > preBytes) preBytes = inputStream.available();
                ReadableByteChannel channel = Channels.newChannel(inputStream);
                buffer = ByteBuffer.allocateDirect(preBytes);

                while (buffer.hasRemaining()) {

                    channel.read(buffer);

                }
                if (speedDebug) logger.finer("loaded Prebytes " + preBytes);
                if (speedDebug) logger.finer("Preloading produced " + inputStream.available() + " bytes overhead");
                inputStream.close();
                channel.close();
                connection.getHTTPURLConnection().disconnect();

                buffer.flip();

                addPartBytes(buffer.limit());
                addToTotalLinkBytesLoaded(buffer.limit());
                addChunkBytesLoaded(buffer.limit());
                writeBytes(this);
                return preBytes;

            } catch (Exception e) {
                error(ERROR_CHUNKLOAD_FAILED);
                addException(e);
                e.printStackTrace();
            }
            return -1;

        }

        private void addChunkBytesLoaded(int limit) {
            this.chunkBytesLoaded += limit;

        }

        /**
         * Darf NUR von Interface.addBytes() aufgerufen werden. Zählt die Bytes
         * 
         * @param bytes
         */
        private void addPartBytes(int bytes) {
            totalPartBytes += bytes;

        }

        /**
         * Kopiert die verbindung. Es wird bis auf die Range und timeouts exakt
         * die selbe verbindung nochmals aufgebaut
         * 
         * @param connection
         * @return
         */
        private HTTPConnection copyConnection(HTTPConnection connection) {

            int start = (int) startByte + getPreBytes(this);
            String end = (endByte > 0 ? endByte + 1 : "") + "";
            if (start == 0) return connection;
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

                httpConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);

                if (connection.getHTTPURLConnection().getDoOutput()) {
                    httpConnection.setDoOutput(true);
                    httpConnection.connect();
                    httpConnection.post(connection.getPostData());
                    httpConnection.getHTTPURLConnection();

                } else {
                    httpConnection.connect();
                }
                if (speedDebug) {
                    // logger.finer("Org request headers " + this.getID() + ":"
                    // + request);
                    // logger.finer("Coppied request headers " + this.getID() +
                    // ":" + httpConnection.getRequestProperties());
                    // logger.finer("Server chunk Headers: " + this.getID() +
                    // ":" + httpConnection.getHeaderFields());
                }
                // connection.getHTTPURLConnection().disconnect();
                return httpConnection;

            } catch (Exception e) {
                addException(e);
                error(ERROR_CHUNKLOAD_FAILED);
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Thread runner
         */
        public void run() {
            plugin.setCurrentConnections(plugin.getCurrentConnections() + 1);
            run0();
            plugin.setCurrentConnections(plugin.getCurrentConnections() - 1);
            addToChunksInProgress(-1);
            onChunkFinished();
        }

        public void run0() {

            logger.finer("Start Chunk " + this.getID() + " : " + startByte + " - " + endByte);
            if ((startByte >= endByte && endByte > 0) || (startByte >= getFileSize() && endByte > 0)) {

                // Korrektur Byte
                if (speedDebug) logger.finer("correct -1 byte");
                addToTotalLinkBytesLoaded(-1);

                return;
            }

            if (chunkNum > 1) {
                if (DownloadInterface.this.getPreBytes(this) > 0) {
                    loadPreBytes();
                    if (speedDebug) logger.finer("After prebytes: " + startByte + " - " + endByte);
                }
                this.connection = copyConnection(connection);

                if (connection == null) {
                    error(ERROR_CHUNKLOAD_FAILED);
                    logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));

                    return;
                }

                if ((startByte + getPreBytes(this)) > 0 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                    error(ERROR_CHUNKLOAD_FAILED);
                    logger.severe("ERROR Chunk (no range header response)" + chunks.indexOf(this));

                    return;

                }
            } else if (startByte > 0) {
                this.connection = copyConnection(connection);

                if (connection == null) {
                    error(ERROR_CHUNKLOAD_FAILED);
                    logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));

                    return;
                }

                if ((startByte + getPreBytes(this)) > 0 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                    error(ERROR_CHUNKLOAD_FAILED);
                    logger.severe("ERROR Chunk (no range header response)" + chunks.indexOf(this));

                    return;

                }
            }

            // Content-Range=[133333332-199999999/200000000]}
            if ((startByte + getPreBytes(this)) > 0) {
                String[] range = Plugin.getSimpleMatches("[" + connection.getHeaderField("Content-Range") + "]", "[°-°/°]");
                if (speedDebug) logger.finer("Range Header " + connection.getHeaderField("Content-Range"));

                if (range == null && chunkNum > 1) {
                    error(ERROR_CHUNKLOAD_FAILED);
                    logger.severe("ERROR Chunk (range header parse error)" + chunks.indexOf(this) + connection.getHeaderField("Content-Range") + ": " + connection.getHeaderField("Content-Range"));

                    return;

                } else if (range != null) {
                    int gotSB = JDUtilities.filterInt(range[0]);
                    int gotEB = JDUtilities.filterInt(range[1]);
                    if (gotSB != startByte + (getPreBytes(this) > 0 ? getPreBytes(this) : 0)) logger.severe("Range Conflict " + range[0] + " - " + range[1] + " wished start: " + (startByte + (getPreBytes(this) > 0 ? getPreBytes(this) : 0)));

                    if (endByte <= 0) {
                        this.endByte = gotEB;
                    } else {
                        if (gotEB == endByte) {
                            logger.finer("ServerType: RETURN Rangeend-1");
                        } else if (gotEB == endByte + 1) {
                            logger.finer("ServerType: RETURN exact rangeend");
                        }
                        if (gotEB < endByte) logger.severe("Range Conflict " + range[0] + " - " + range[1] + " wishedend: " + endByte);
                        if (gotEB > endByte + 1) logger.warning("Possible RangeConflict or Servermisconfiguration. wished endByte: " + endByte + " got: " + gotEB);
                        this.endByte = Math.min(endByte, gotEB);

                    }

                    if (speedDebug) logger.finer("Resulting Range" + startByte + " - " + endByte);
                } else if (maxBytes < 0) {

                    endByte = connection.getContentLength() - 1;
                    if (speedDebug) logger.finer("Endbyte set to " + endByte);
                }
            }
            if (endByte <= 0) {

                endByte = connection.getContentLength() - 1;
                if (speedDebug) logger.finer("Endbyte set to " + endByte);
            }

            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            addChunksDownloading(+1);

            download();
            this.bytesPerSecond = 0;
            this.desiredBps = 0;
            addChunksDownloading(-1);

            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            logger.finer("Chunk finished " + chunks.indexOf(this) + " " + getBytesLoaded() + " bytes");

        }

        /**
         * Über buffersize und timeinterval wird die dwonloadgeschwindigkeit
         * eingestellt. Eine zu hohe INtervalzeit sorgt für stark
         * intervalartiges laden und unregelmäsige gui aktualisierungen. Der
         * Download "ruckelt". Zu kleine INtervalzeiten belasten die Festplatte
         * sehr
         * 
         * @return
         */
        private int getTimeInterval() {
            if (!downloadLink.isLimited()) return TIME_BASE;

            return Math.min(TIME_BASE * 5, (int) (TIME_BASE * this.bufferTimeFaktor));

        }

        /**
         * Gibt zurück ob der chunk von einem externen eregniss unterbrochen
         * wurde
         * 
         * @return
         */
        private boolean isExternalyAborted() {
            return aborted || plugin.aborted || abortByError || downloadLink.isAborted();
        }

        /**
         * Die eigentliche downloadfunktion
         */

        private void download() {
            int bufferSize = 1;

            if (speedDebug) logger.finer("resume Chunk with " + totalPartBytes + "/" + this.getChunkSize() + " at " + getCurrentBytesPosition());
            try {
                bufferSize = getBufferSize(getMaximalSpeed());
                if (endByte > 0 && bufferSize > endByte - getCurrentBytesPosition() + 1) {
                    bufferSize = (int) (endByte - getCurrentBytesPosition() + 1);
                }
                // logger.finer(bufferSize+" - "+this.getTimeInterval());
                buffer = ByteBuffer.allocateDirect(bufferSize);

            } catch (OutOfMemoryError e) {
                error(ERROR_TOO_MUCH_BUFFERMEMORY);
                return;
            }

            // InputStream inputStream = null;
            // ReadableByteChannel source = null;

            try {
                // logger.finer("Set timeouts: "+getReadTimeout()+" -
                // "+getRequestTimeout());
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
                ByteBuffer miniBuffer = ByteBuffer.allocateDirect(1024 * 128);
                int ti = 0;
                while (!isExternalyAborted()) {
                    bytes = 0;
                    ti = getTimeInterval();
                    timer = System.currentTimeMillis();
                    if (speedDebug) logger.finer("load Block buffer: " + buffer.hasRemaining() + "/" + buffer.capacity() + " interval: " + ti);
                    while (buffer.hasRemaining() && !isExternalyAborted() && (System.currentTimeMillis() - timer) < ti) {
                        block = 0;

                        // PrÃŒft ob bytes zum Lesen anliegen.

                        // kann den connectiontimeout nicht auswerten
                        

                        try {

                            this.blockStart = System.currentTimeMillis();

                            if (miniBuffer.capacity() > buffer.remaining()) {
                                block = source.read(buffer);
                            } else {
                                miniBuffer.clear();
                                block = source.read(miniBuffer);
                                miniBuffer.flip();
                                buffer.put(miniBuffer);

                            }
                            this.blockStart = -1;
                        } catch (ClosedByInterruptException e) {
                            if (this.isExternalyAborted()) {

                            } else {
                                logger.severe("Timeout detected");
                                error(ERROR_TIMEOUT_REACHED);
                            }
                            this.blockStart = -1;
                            block = -1;
                            break;
                        }

                        if (block == -1) {

                            break;
                        }

                        addPartBytes(block);
                        addToTotalLinkBytesLoaded(block);
                        addChunkBytesLoaded(block);
                        bytes += block;

                    }
                    if (block == -1 && bytes == 0) break;
                    deltaTime = Math.max(System.currentTimeMillis() - timer, 1);
                    desiredBps = (1000 * (long) bytes) / deltaTime;
                    if (speedDebug) logger.finer("desired: " + desiredBps + " - loaded: " + (System.currentTimeMillis() - timer) + " - " + bytes);

                    buffer.flip();
                    if (speedDebug) logger.finer("write bytes");
                    writeBytes(this);

                    buffer.clear();

                    // logger.info(this.getID() + ": " + this.startByte + " -->
                    // " + currentBytePosition + " -->" + this.endByte + "/" +
                    // bytesLoaded + ":" + (100.0 * (currentBytePosition -
                    // startByte) / (double) (endByte - startByte)));

                    if (block == -1 || isExternalyAborted()) break;

                    if (getCurrentBytesPosition() > this.endByte) {

                        if (speedDebug) logger.severe(this.getID() + " OVERLOAD!!! " + (getCurrentBytesPosition() - this.endByte - 1));
                        break;
                    }

                    /*
                     * War der download des buffers zu schnell, wird heir eine
                     * pause eingelegt
                     */
                    tempBuff = getBufferSize(getMaximalSpeed());
                    // Falls der Server bei den Ranges schlampt und als endByte
                    // immer das dateiende angibt wird hier der buffer
                    // korrigiert um overhead zu vermeiden
                    if (tempBuff > endByte - getCurrentBytesPosition() + 1) {
                        tempBuff = (int) (endByte - getCurrentBytesPosition()) + 1;
                    }
                    if (Math.abs(bufferSize - tempBuff) > 1000) {
                        bufferSize = tempBuff;
                        try {
                            buffer = ByteBuffer.allocateDirect(Math.max(1, bufferSize));

                        } catch (Exception e) {
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
                        addWait = (long) (0.995 * (ti - (System.currentTimeMillis() - timer)));
                        if (speedDebug) logger.finer("Wait " + addWait);
                        if (addWait > 0) Thread.sleep(addWait);
                    } catch (Exception e) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;

                    this.bytesPerSecond = (1000 * (long) bytes) / deltaTime;
                    updateSpeed();

                    if (speedDebug) logger.finer(downloadLink.getSpeedMeter().getSpeed() + " loaded" + bytes + " b in " + (deltaTime) + " ms: " + bytesPerSecond + "(" + desiredBps + ") ");

                }
                buffer = null;
                if (getCurrentBytesPosition() < endByte && endByte > 0) {

                    inputStream.close();
                    source.close();

                    logger.warning(" incomplete download: bytes loaded: " + getCurrentBytesPosition() + "/" + endByte);
                    error(ERROR_CHUNK_INCOMPLETE);
                }

                inputStream.close();
                source.close();

            } catch (FileNotFoundException e) {
                logger.severe("file not found. " + e.getLocalizedMessage());
                error(ERROR_FILE_NOT_FOUND);
            } catch (SecurityException e) {
                logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
                error(ERROR_SECURITY);

            } catch (IOException e) {
                if (e.getMessage()!=null&&e.getMessage().indexOf("timed out") >= 0) {
                    error(ERROR_TIMEOUT_REACHED);
                    ;

                } else {
                    logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
                    error(ERROR_SECURITY);
                }
            } catch (Exception e) {

                e.printStackTrace();
                error(ERROR_UNKNOWN);
                addException(e);

            }

            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (source != null) source.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.blockStart = -1;
        }

        /**
         * Schränkt die Buffergröße ein.
         * 
         * @param maxspeed
         * @return
         */
        private int getBufferSize(long maxspeed) {
            if (speedDebug) logger.finer("speed " + maxspeed);
            if (!downloadLink.isLimited()) return (int) MAX_BUFFERSIZE;
            maxspeed *= (TIME_BASE / 1000);
            long max = Math.max(MIN_BUFFERSIZE, maxspeed);
            int bufferSize = (int) Math.min(MAX_BUFFERSIZE, max);
            // logger.finer(MIN_BUFFERSIZE+"<>"+maxspeed+"-"+MAX_BUFFERSIZE+"><"+max);
            this.bufferTimeFaktor = Math.max(0.1, (double) bufferSize / maxspeed);
            if (speedDebug) logger.finer("Maxspeed= " + maxspeed + " buffer=" + bufferSize + "time: " + getTimeInterval());
            return bufferSize;
        }

        /**
         * Gibt die aktuelle downloadgeschwindigkeit des chunks zurück
         * 
         * @return
         */
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

        /**
         * Gibt die geladenen Partbytes zurück. Das entsüricht bei resumen nicht
         * den Chunkbytes!!!
         * 
         * @return
         */
        public int getTotalPartBytesLoaded() {
            return totalPartBytes;
        }

        /**
         * Setzt die anzahl der schon geladenen partbytes. Ist für resume
         * wichtig.
         * 
         * @param loaded
         */
        public void setLoaded(int loaded) {
            this.totalPartBytes = loaded;
            addToTotalLinkBytesLoaded(loaded);
        }

        public long getStartByte() {
            return startByte;
        }

        public long getEndByte() {
            return endByte;
        }

        /**
         * Gibt die Schreibposition des Chunks in der gesamtfile zurück
         */
        public long getWritePosition() {
            int c = (int) this.getCurrentBytesPosition();
            int l = buffer.limit();
            return c - l;
        }

    }

    protected synchronized void addToTotalLinkBytesLoaded(int block) {
        totaleLinkBytesLoaded += block;

    }

    protected synchronized void addChunksDownloading(int i) {

        chunksDownloading += i;

    }

    public synchronized void addToChunksInProgress(int i) {
        chunksInProgress += i;

    }

    public void abort() {
        this.aborted = true;

    }

    public Vector<Chunk> getChunks() {

        return this.chunks;
    }

    public Vector<Exception> getExceptions() {
        return exceptions;
    }

}
