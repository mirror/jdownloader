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

    private volatile RandomAccessFile outputFile;

    private FileChannel               outputChannel;

    private int                       maxBytes;

    private long                      lastChunkSpeedCheck;

    private int                       lastMaxChunkSpeed;

    private long                      fileSize                               = -1;

    private boolean abortByError=false;

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
            this.outputFile.seek(currentBytePosition);

            int length = buffer.limit() - buffer.position();
            byte[] tmp = new byte[length];
            // logger.info("wrote " + length + " at " + currentBytePosition);
            // buffer.get(tmp, buffer.position(), length);
            outputChannel.write(buffer);
            // this.outputFile.write(tmp);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void setResume(boolean value) {
        this.resume = value;
    }

    public boolean isResume() {
        return resume;
    }

    private void error(int id) {
        logger.severe("Error occured: "+id);
        if (errors.indexOf(id) < 0) errors.add(id);
        if(id==ERROR_TOO_MUCH_BUFFERMEMORY){
            terminate(id);
            
        }
        if(id==ERROR_UNKNOWN){
            terminate(id);
            
        }
    }

    private void terminate(int id) {
        
        logger.severe("A critical Downlaoderror occured. Terminate...");
      this.abortByError=true;
        
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

        File part = new File(downloadLink.getFileOutput() + ".part");
        try {
            this.outputFile = new RandomAccessFile(part, "rwd");
            // outputFile.setLength(connection.getContentLength());
            outputChannel = outputFile.getChannel();
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
            long fileSize = getFileSize();
            logger.info("Filsize: " + fileSize);
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
            if (!handleErrors()) return false;
            if (!part.renameTo(new File(downloadLink.getFileOutput()))) {
                logger.severe("Could not rename file " + fileOutput + " to " + downloadLink.getFileOutput());
                error(ERROR_COULD_NOT_RENAME);
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
        return false;

    }

    private long getFileSize() {
        if (connection.getContentLength() > 0) {
            logger.info("1 " + connection.getHeaderFields());

            return connection.getContentLength();
        }
        if (fileSize > 0) {
            logger.info("2");
            return fileSize;
        }
        if (downloadLink.getDownloadMax() > 0) {
            logger.info("3");
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
            return false;
        }

        plugin.getCurrentStep().setStatus(PluginStep.STATUS_DONE);
        downloadLink.setStatus(DownloadLink.STATUS_DONE);
        return true;
    }

    private void waitForChunks() {
        long bytesLoaded = this.bytesLoaded;
        long deltaBytes = 0;
        while (chunksInProgress > 0) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
            }
            deltaBytes = this.bytesLoaded - bytesLoaded;
            downloadLink.addBytes(deltaBytes, 1000);
            bytesLoaded = this.bytesLoaded;
            // firePluginEvent(new PluginEvent(this,
            // PluginEvent.PLUGIN_DATA_CHANGED, downloadLink));
            downloadLink.setDownloadCurrent(bytesLoaded);
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

    private int getMaximalChunkSpeed() {
        // if (System.currentTimeMillis() < (lastChunkSpeedCheck + 1000)) return
        // lastMaxChunkSpeed;
        // this.lastChunkSpeedCheck = System.currentTimeMillis();
        int allowedLinkSpeed = downloadLink.getMaximalspeed() * 40;

        int chunkSpeed = allowedLinkSpeed / getRunningChunks();
        // logger.info("Allowedperchunk "+chunkSpeed+"/"+allowedLinkSpeed);
        Iterator<Chunk> it = chunks.iterator();
        Chunk next;

        int currentTotalspeed = 0;
        while (it.hasNext()) {
            next = it.next();
            if (next.isAlive()) {

                currentTotalspeed += next.getBytesPerSecond() < 0 ? chunkSpeed : next.getBytesPerSecond();

            }
        }
        // logger.info("Total speed= "+currentTotalspeed);
        // logger.info("Max Chunkspeed:
        // "+(chunkSpeed+(allowedLinkSpeed-currentTotalspeed)/this.getRunningChunks()));
        this.lastMaxChunkSpeed = chunkSpeed + (allowedLinkSpeed - currentTotalspeed) / this.getRunningChunks();
        // logger.info("Chunk" +lastMaxChunkSpeed);
        return lastMaxChunkSpeed;

    }

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

        private static final int MAX_BUFFERSIZE = 2 * 1024 * 1024;

        private static final int MIN_BUFFERSIZE = 10 * 128 * 1024;

        private long             startByte;

        private long             endByte;

        private HTTPConnection   connection;

        private long             currentBytePosition;

        private long             bytesPerSecond = -1;

        private double           bufferTimeFaktor;

        public Chunk(long startByte, long endByte, HTTPConnection connection) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.connection = connection;

            currentBytePosition = startByte;
            if (startByte >= endByte && endByte > 0) {
                logger.severe("Startbyte has to be less than endByte");
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

                    logger.info(chunks.indexOf(this) + " - " + httpConnection.getRequestProperties() + "");
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
            logger.info("Start Chunk " + chunks.indexOf(this));
            chunksDownloading++;
            download();
            logger.info("END Chunk " + chunks.indexOf(this));
            chunksDownloading--;
            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            logger.finer("Chunk finished " + getBytesLoaded());
            chunksInProgress--;
        }

        private int getTimeInterval() {
            // logger.info("Timeinterval: "+(int)(1000*this.bufferTimeFaktor));
            return (int) (1000 * this.bufferTimeFaktor);

        }

        private void download() {
            ByteBuffer buffer = null;
            int bufferSize=1;
            try {
            bufferSize = getBufferSize(getMaximalChunkSpeed());
           
            
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
                while (!plugin.aborted && !downloadLink.isAborted()&&!abortByError) {
                    bytes = 0;

                    timer = System.currentTimeMillis();
                    while (buffer.hasRemaining() && !plugin.aborted &&!abortByError&& !downloadLink.isAborted() && (System.currentTimeMillis() - timer) < getTimeInterval()) {
                        // if(bytes>0)Thread.sleep(100);
                        block = source.read(buffer);
                        if (block == -1) {
                            break;
                        }
                        bytes += block;
                    }
                    if (block == -1 && bytes == 0) break;
                    buffer.flip();
                    addBytes(buffer, currentBytePosition);
                    addBytes(bytes);
                    buffer.clear();
                    currentBytePosition += bytes;

                    if (block == -1||abortByError||plugin.aborted||downloadLink.isAborted()) break;
                    try {
                        Thread.sleep(getTimeInterval() - (System.currentTimeMillis() - timer));
                    }
                    catch (Exception e) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;

                    this.bytesPerSecond = (1000 * bytes) / deltaTime;
                    // logger.info("loaded "+bytes+" b in "+(deltaTime)+" ms:
                    // "+bytesPerSecond);
                    // bufferSize=getBufferSize(getMaximalChunkSpeed());
                    if (bufferSize != (bufferSize = getBufferSize(getMaximalChunkSpeed()))) {
                       
                        
                        try {
                            buffer = ByteBuffer.allocateDirect(Math.max(1, bufferSize));

                        }
                        catch (Exception e) {
                            error(ERROR_TOO_MUCH_BUFFERMEMORY);
                            return;
                        }
                        
                        buffer.clear();
                    }

                }
                if (currentBytePosition != endByte && endByte > 0) {

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

                logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
                error(ERROR_SECURITY);
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

        private int getBufferSize(int maxspeed) {
            int bufferSize = Math.min(MAX_BUFFERSIZE, Math.max(MIN_BUFFERSIZE, maxspeed));
            this.bufferTimeFaktor = Math.max(0.1, (double) bufferSize / maxspeed);
            // logger.info("Buffersize: "+bufferSize+" at
            // "+this.getTimeInterval()+" ms.
            // chunkspeed:"+maxspeed+"("+(downloadLink.getMaximalspeed()*40)+")");
            return bufferSize;
        }

        public long getBytesPerSecond() {
            return bytesPerSecond;
        }

    }

    public boolean isSpeedLimited() {
        return speedLimited && downloadLink.isLimited;
    }

    public void setSpeedLimited(boolean speedLimited) {
        this.speedLimited = speedLimited;
    }

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

    public void setChunksDownloading(int chunksDownloading) {
        this.chunksDownloading = chunksDownloading;
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

}
