package jd.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.plugins.event.PluginEvent;
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

    private DownloadLink              downloadLink;

    private HTTPConnection            connection;

    private int                       status                                 = STATUS_INITIALIZED;

    private int                       chunkNum                               = 1;

    private int                       readTimeout                            = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 10000);

    private int                       requestTimeout                         = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 10000);

    private int                       chunksInProgress                       = 0;

    private Vector<Integer>           errors                                 = new Vector<Integer>();

    private Vector<Chunk>             chunks                                 = new Vector<Chunk>();

    private boolean                   resume                                 = false;

    private boolean                   speedLimited                           = true;

    private Plugin                    plugin;

    private int                       bytesLoaded                            = 0;

    private volatile RandomAccessFile outputFile;

    private FileChannel               outputChannel;

    public static Logger              logger                                 = JDUtilities.getLogger();

    public Download(Plugin plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        this.downloadLink = downloadLink;
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
        if (errors.indexOf(id) < 0) errors.add(id);
    }

    public boolean startDownload() {
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            error(ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            return false;
        }
        File fileOutput = new File(downloadLink.getFileOutput());
        if (fileOutput == null || fileOutput.getParentFile() == null) {
            error(ERROR_OUTPUTFILE_INVALID);
            return false;
        }
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }

        if (fileOutput.exists()) {
            logger.severe("File already exists. " + fileOutput);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
            error(ERROR_OUTPUTFILE_ALREADYEXISTS);
            return false;
        }

        File part = new File(downloadLink.getFileOutput() + ".part");
        try {
            this.outputFile = new RandomAccessFile(part, "rwd");
            //outputFile.setLength(connection.getContentLength());
            outputChannel = outputFile.getChannel();
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
            long parts = connection.getContentLength() / chunkNum;
            logger.finer("Start Download in " + chunkNum + " chunks. Chunksize: " + parts);
            for (int i = 0; i < chunkNum; i++) {
                if (i == (chunkNum - 1)) {
                    addChunk(new Chunk(i * parts, connection.getContentLength(), connection));
                }
                else {
                    addChunk(new Chunk(i * parts, (i + 1) * parts, connection));
                }

            }
            waitForChunks();
            logger.info("Errors: " + this.errors);
            outputFile.close();
            outputChannel.close();
            if(!handleErrors())return false;
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

    public boolean handleErrors() {
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);
      
        if (errors.contains(ERROR_ABORTED_BY_USER)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_TODO);
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
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
        chunk.start();

    }

    private int getMaximalChunkSpeed() {
        int allowedLinkSpeed =  Math.min(downloadLink.getMaximalspeed()*40,10000000);

        int chunkSpeed = allowedLinkSpeed / chunksInProgress;
        Iterator<Chunk> it = chunks.iterator();
        Chunk next;
        long nextChunkSpeed;
        int i = 0;
        while (it.hasNext()) {
            next = it.next();
            if (next.isAlive()) {
                i++;
                if ((chunksInProgress - i) == 0) {
                    chunkSpeed = allowedLinkSpeed;
                    break;
                }
                nextChunkSpeed = next.getBytesPerSecond();

                if (nextChunkSpeed == -1) {
                    nextChunkSpeed = chunkSpeed;
                }
                allowedLinkSpeed -= nextChunkSpeed;
                chunkSpeed = allowedLinkSpeed / (chunksInProgress - i);

            }
        }

        return chunkSpeed;

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

        private long           startByte;

        private long           endByte;

        private File           fileOutput;

        private HTTPConnection connection;

        private long           currentBytePosition;

        private long           bytesPerSecond = -1;

        public Chunk(long startByte, long endByte, HTTPConnection connection) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.connection=connection;

            currentBytePosition = startByte;
            if (startByte >= endByte) {
                logger.severe("Startbyte has to be less than endByte");
            }
        }

        private HTTPConnection copyConnection(HTTPConnection connection) {
            try {
                URL link = connection.getURL();
                HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
                httpConnection.setReadTimeout(getReadTimeout());
                httpConnection.setConnectTimeout(getRequestTimeout());
                httpConnection.setInstanceFollowRedirects(true);
                Map<String, List<String>> request = connection.getRequestProperties();
                Set<Entry<String, List<String>>> requestEntries = request.entrySet();
                Iterator<Entry<String, List<String>>> it = requestEntries.iterator();
                String value;
                while (it.hasNext()) {
                    Entry<String, List<String>> next = it.next();

                    value = next.getValue().toString();
                    httpConnection.setRequestProperty(next.getKey(), value.substring(1, value.length() - 1));
                }

                // if(chunkNum>1){
                httpConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                logger.info(httpConnection.getRequestProperties() + "");
                // }
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
            fileOutput = new File(downloadLink.getFileOutput() + ".part" + startByte);
            logger.finer("Start Chunk " + startByte + " - " + endByte + ": " + fileOutput);
            
            this.connection = copyConnection(connection);
            download();
            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            logger.finer("Chunk finished: " + fileOutput);
            chunksInProgress--;
        }

        private void download() {
            int maxspeed = getMaximalChunkSpeed();
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxspeed);

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
                while (!plugin.aborted && !downloadLink.isAborted()) {
                    bytes = 0;

                    timer = System.currentTimeMillis();
                    while (buffer.hasRemaining() && !plugin.aborted && !downloadLink.isAborted()&&(System.currentTimeMillis() - timer)<1000) {
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

                    if (block == -1) break;

                    while ((System.currentTimeMillis() - timer) < 1000) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;
                    this.bytesPerSecond = (1000 * bytes) / deltaTime;

                    if (maxspeed != (maxspeed = getMaximalChunkSpeed())) {
                        buffer = ByteBuffer.allocateDirect(Math.max(1,maxspeed));
                        buffer.clear();
                    }

                }
                if (currentBytePosition != endByte) {

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

        public File getFileOutput() {
            return fileOutput;
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

}
