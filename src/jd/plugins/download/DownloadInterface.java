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
import java.net.UnknownHostException;
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
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

abstract public class DownloadInterface {

    /**
     * Chunk Klasse verwaltet eine einzellne Downloadverbindung.
     * 
     * @author coalado
     * 
     */
    public class Chunk extends Thread {
        private static final long MIN_BUFFERSIZE = 1024;

        // Wird durch die speedbegrenzung ein chunk uter diesen wert geregelt,
        // so wird er weggelassen.
        // sehr niedrig geregelte chunks haben einen kleinen buffer und eine
        // sehr hohe intervalzeit.
        // Das führt zu verstärkt intervalartigem laden und ist ungewünscht
        public static final long MIN_CHUNKSIZE = 1 * 1024 * 1024;

        private static final int TIME_BASE = 2000;

        private long blockStart = 0;

        ByteBuffer buffer;

        private double bufferTimeFaktor;

        private long bytesPerSecond = -1;

        private long chunkBytesLoaded = 0;

        private HTTPConnection connection;

        private long desiredBps;

        private long endByte;

        private int id = -1;

        private InputStream inputStream;

        // private int preBytes = -1;

        private long MAX_BUFFERSIZE = 4 * 1024 * 1024;

        private int maxSpeed;

        private ReadableByteChannel source;

        private long startByte;

        private long totalPartBytes = 0;

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
            setPriority(Thread.MIN_PRIORITY);
            MAX_BUFFERSIZE = (long) JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty("MAX_BUFFER_SIZE", 4) * 1024 * 1024l;

        }

        private void addChunkBytesLoaded(long limit) {
            chunkBytesLoaded += limit;

        }

        /**
         * Gibt Fortschritt in % an (10000 entspricht 100%))
         * 
         * @return
         */
        public int getPercent() {
            return (int) (10000 * chunkBytesLoaded / Math.max(1, Math.max(chunkBytesLoaded, (endByte - startByte))));
        }

        /**
         * Darf NUR von Interface.addBytes() aufgerufen werden. Zählt die Bytes
         * 
         * @param bytes
         */
        private void addPartBytes(long bytes) {
            totalPartBytes += bytes;

        }

        public void checkTimeout(long timeout) {
            long timer = blockStart;
            if (Thread.interrupted() || !isAlive()) { return; }
            // try {
            // if (this.inputStream.available() > 0) {
            // blockStart = -1;
            // }
            // } catch (IOException e) {
            // }
            if (isExternalyAborted()) {
                logger.severe("INTERRUPT");

                interrupt();

            }
            if (timer <= 0) { return; }
            long dif = System.currentTimeMillis() - timer;
            // logger.info(this + " " + dif);
            if (dif >= timeout) {
                logger.severe("Timeout or termination detected: interrupt: " + timeout + " - " + dif + " - " + timer);
                interrupt();

            } else if (dif >= 5000) {
                downloadLink.getLinkStatus().setStatusText(JDLocale.L("download.connection.idle", "Idle"));
                downloadLink.requestGuiUpdate();
            } else {
                downloadLink.getLinkStatus().setStatusText(null);
            }

        }

        /**
         * Kopiert die verbindung. Es wird bis auf die Range und timeouts exakt
         * die selbe verbindung nochmals aufgebaut
         * 
         * @param connection
         * @return
         */
        private HTTPConnection copyConnection(HTTPConnection connection) {
            try {
                downloadLink.getPlugin().waitForNextConnectionAllowed();
            } catch (InterruptedException e1) {
                return null;
            }
            long start = startByte;
            String end = (endByte > 0 ? endByte + 1 : "") + "";

            if (start == 0) {
                logger.finer("Übernehme 0 Verbindung");
                return connection;
            }
            if (connection.getRange() != null && connection.getRange()[0] == (start)) {
                logger.finer("Übernehme Verbindung bei " + connection.getRange()[0]);
                return connection;
            }
            // connection.getHTTPURLConnection().disconnect();

            try {
                Browser br = plugin.getBrowser().cloneBrowser();
                br.setReadTimeout(getReadTimeout());
                br.setConnectTimeout(getRequestTimeout());

                Map<String, List<String>> request = connection.getRequestProperties();

                if (request != null) {
                    Set<Entry<String, List<String>>> requestEntries = request.entrySet();
                    Iterator<Entry<String, List<String>>> it = requestEntries.iterator();
                    String value;
                    while (it.hasNext()) {
                        Entry<String, List<String>> next = it.next();

                        value = next.getValue().toString();
                        br.getHeaders().put(next.getKey(), value.substring(1, value.length() - 1));
                    }
                }

                br.getHeaders().put("Range", "bytes=" + start + "-" + end);
                HTTPConnection con;
                if (connection.getHTTPURLConnection().getDoOutput()) {
                    con = br.openPostConnection(connection.getURL() + "", connection.getPostData());

                } else {
                    con = br.openGetConnection(connection.getURL() + "");
                }

                if (!con.isOK()) {
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, "Server: " + con.getHTTPURLConnection().getResponseMessage());
                    return null;
                }
                if (con.getHeaderField("Location") != null) {
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, "Server: Redirect");
                    return null;

                }
                if (speedDebug) {
                    // logger.finer("Org request headers " + this.getID() + ":"
                    // + request);
                    // logger.finer("Coppied request headers " + this.getID() +
                    // ":" + httpConnection.getRequestProperties());
                    // logger.finer("Server chunk Headers: " + this.getID() +
                    // ":" + httpConnection.getHeaderFields());
                }
                return con;

            } catch (Exception e) {
                addException(e);
                error(LinkStatus.ERROR_RETRY, JDUtilities.convertExceptionReadable(e));

                e.printStackTrace();
            }

            return null;
        }

        /**
         * Die eigentliche downloadfunktion
         */

        private void download() {
            long bufferSize = 1;

            if (speedDebug) {
                logger.finer("resume Chunk with " + totalPartBytes + "/" + getChunkSize() + " at " + getCurrentBytesPosition());
            }
            try {
                bufferSize = getBufferSize(getMaximalSpeed());
                if (endByte > 0 && bufferSize > endByte - getCurrentBytesPosition() + 1) {
                    bufferSize = (int) (endByte - getCurrentBytesPosition() + 1);
                }
                // logger.finer(bufferSize+" - "+this.getTimeInterval());
                /* max 2gb buffer */
                buffer = ByteBuffer.allocateDirect((int) bufferSize);

            } catch (OutOfMemoryError e) {
                error(LinkStatus.ERROR_FATAL, JDLocale.L("download.error.message.outofmemory", "The downloadsystem is out of memory"));

                return;
            }

            try {
                connection.setReadTimeout(getReadTimeout());
                connection.setConnectTimeout(getRequestTimeout());

                inputStream = connection.getInputStream();
                source = Channels.newChannel(inputStream);

                buffer.clear();

                long deltaTime;
                long timer;

                long bytes;
                long miniblock = 0;
                long tempBuff = 0;
                long addWait;
                ByteBuffer miniBuffer = ByteBuffer.allocateDirect(1024 * 10);
                miniBuffer.clear();
                int ti = 0;
                blockStart = System.currentTimeMillis();

                while (!isExternalyAborted()) {
                    bytes = 0;
                    ti = getTimeInterval();
                    timer = System.currentTimeMillis();
                    if (speedDebug) {
                        logger.finer("load Block buffer: " + buffer.hasRemaining() + "/" + buffer.capacity() + " interval: " + ti);
                    }
                    // boolean a1 = buffer.hasRemaining();
                    // boolean a2 = isExternalyAborted();
                    // long a4 = (System.currentTimeMillis() - timer);
                    // boolean a3 = ((System.currentTimeMillis() - timer) < ti);
                    //

                    while (buffer.hasRemaining() && !isExternalyAborted() && System.currentTimeMillis() - timer < ti) {

                        // PrÃŒft ob bytes zum Lesen anliegen.

                        // kann den connectiontimeout nicht auswerten

                        try {
                            miniBuffer.clear();
                            if (miniBuffer.remaining() > buffer.remaining()) {
                                miniBuffer.limit(buffer.remaining());
                            }
                            miniblock = source.read(miniBuffer);
                            miniBuffer.flip();
                            buffer.put(miniBuffer);
                        } catch (ClosedByInterruptException e) {
                            if (isExternalyAborted()) {
                            } else {
                                logger.severe("Timeout detected");
                                error(LinkStatus.ERROR_TIMEOUT_REACHED, null);
                            }
                            miniblock = -1;
                            break;
                        }
                        if (miniblock > 0) {
                            blockStart = System.currentTimeMillis();
                        }
                        if (miniblock == -1) {
                            break;
                        }
                        addPartBytes(miniblock);
                        addToTotalLinkBytesLoaded(miniblock);
                        addChunkBytesLoaded(miniblock);
                        bytes += miniblock;
                    }

                    if (miniblock == -1 && bytes == 0) {
                        break;
                    }
                    // if(bytes==0)continue;
                    deltaTime = Math.max(System.currentTimeMillis() - timer, 1);
                    desiredBps = 1000 * bytes / deltaTime;
                    if (speedDebug) {
                        logger.finer("desired: " + desiredBps + " - loaded: " + (System.currentTimeMillis() - timer) + " - " + bytes);
                    }

                    buffer.flip();
                    if (speedDebug) {
                        logger.finer("write bytes");
                    }
                    writeBytes(this);

                    buffer.clear();

                    // logger.info(this.getID() + ": " + this.startByte + " -->
                    // " + currentBytePosition + " -->" + this.endByte + "/" +
                    // bytesLoaded + ":" + (100.0 * (currentBytePosition -
                    // startByte) / (double) (endByte - startByte)));

                    if (miniblock == -1 || isExternalyAborted()) {
                        break;
                    }

                    if (getCurrentBytesPosition() > endByte && endByte > 0) {
                        if (speedDebug) {
                            logger.severe(getID() + " OVERLOAD!!! " + (getCurrentBytesPosition() - endByte - 1));
                        }
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
                    if (tempBuff > endByte - getCurrentBytesPosition() + 1 && endByte > 0) {
                        tempBuff = (int) (endByte - getCurrentBytesPosition()) + 1;
                    }

                    bufferSize = Math.max(128, tempBuff);
                    try {
                        /* max 2gb buffer */
                        if ((int) bufferSize > buffer.capacity()) {
                            buffer = ByteBuffer.allocateDirect((int) bufferSize);
                            buffer.clear();
                        } else {
                            buffer.clear();
                            buffer.limit((int) bufferSize);
                        }

                    } catch (Exception e) {
                        error(LinkStatus.ERROR_FATAL, JDLocale.L("download.error.message.outofmemory", "The downloadsystem is out of memory"));

                        return;
                    }
                    try {
                        // 0.995 ist eine Anpassung an die Zeit, die die
                        // unerfasste Schleife noch frisst. das macht am ende
                        // ein paar wenige bytes/sekunde in der speederfassung
                        // aus.
                        addWait = (long) (0.995 * (ti - (System.currentTimeMillis() - timer)));
                        if (speedDebug) {
                            logger.finer("Wait " + addWait);
                        }
                        if (addWait > 0) {
                            Thread.sleep(addWait);
                        }
                    } catch (Exception e) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;

                    bytesPerSecond = 1000 * bytes / deltaTime;
                    updateSpeed();

                    if (speedDebug) {
                        logger.finer(downloadLink.getSpeedMeter().getSpeed() + " loaded" + bytes + " b in " + deltaTime + " ms: " + bytesPerSecond + "(" + desiredBps + ") ");
                    }

                }
                buffer = null;
                if (getCurrentBytesPosition() < endByte && endByte > 0 || getCurrentBytesPosition() <= 0) {

                    inputStream.close();
                    source.close();

                    logger.warning(" incomplete download: bytes loaded: " + getCurrentBytesPosition() + "/" + endByte);
                    error(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, JDLocale.L("download.error.message.incomplete", "Download unvollständig"));
                }

                inputStream.close();
                source.close();

            } catch (FileNotFoundException e) {
                logger.severe("file not found. " + e.getLocalizedMessage());
                error(LinkStatus.ERROR_FILE_NOT_FOUND, null);
            } catch (SecurityException e) {
                logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());

                error(LinkStatus.ERROR_LOCAL_IO, JDLocale.L("download.error.message.iopermissions", "No permissions to write to harddisk"));

            } catch (UnknownHostException e) {
                linkStatus.setValue(10 * 60000l);
                error(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("download.error.message.unavailable", "Service temp. unavailable"));

            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().indexOf("timed out") >= 0) {
                    error(LinkStatus.ERROR_TIMEOUT_REACHED, null);
                    e.printStackTrace();
                    ;

                } else {
                    e.printStackTrace();
                    if (e.getLocalizedMessage().contains("503")) {
                        linkStatus.setValue(10 * 60000l);
                        error(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("download.error.message.unavailable", "Service temp. unavailable"));

                    } else {
                        logger.severe("error occurred while writing to file. " + e.getMessage());
                        error(LinkStatus.ERROR_LOCAL_IO, JDLocale.L("download.error.message.iopermissions", "No permissions to write to harddisk"));
                    }
                }
            } catch (Exception e) {

                e.printStackTrace();
                error(LinkStatus.ERROR_RETRY, JDUtilities.convertExceptionReadable(e));
                addException(e);

            }

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (source != null) {
                    source.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void finalize() {
            if (speedDebug) {
                logger.finer("Finalized: " + downloadLink + " : " + getID());
            }
            buffer = null;
            System.gc();
            System.runFinalization();

        }

        /**
         * Schränkt die Buffergröße ein.
         * 
         * @param maxspeed
         * @return
         */
        private long getBufferSize(long maxspeed) {
            if (speedDebug) {
                logger.finer("speed " + maxspeed);
            }
            if (!downloadLink.isLimited()) { return (int) MAX_BUFFERSIZE; }
            maxspeed *= TIME_BASE / 1000;
            long max = Math.max(MIN_BUFFERSIZE, maxspeed);
            long bufferSize = Math.min(MAX_BUFFERSIZE, max);
            //logger.finer(MIN_BUFFERSIZE+"<>"+maxspeed+"-"+MAX_BUFFERSIZE+"><"+
            // max);
            bufferTimeFaktor = Math.max(0.1, (double) bufferSize / maxspeed);
            if (speedDebug) {
                logger.finer("Maxspeed= " + maxspeed + " buffer=" + bufferSize + "time: " + getTimeInterval());
            }
            return bufferSize;
        }

        /**
         * Gibt die Geladenen ChunkBytes zurück
         * 
         * @return
         */
        public long getBytesLoaded() {
            return getCurrentBytesPosition() - startByte;
        }

        /**
         * Gibt die aktuelle downloadgeschwindigkeit des chunks zurück
         * 
         * @return
         */
        public long getBytesPerSecond() {
            return bytesPerSecond;
        }

        public long getChunkSize() {
            return endByte - startByte + 1;
        }

        /**
         * Gibt die Aktuelle Endposition in der gesamtfile zurück. Diese Methode
         * gibt die Endposition unahängig davon an Ob der aktuelle BUffer schon
         * geschrieben wurde oder nicht.
         * 
         * @return
         */
        long getCurrentBytesPosition() {

            return startByte + chunkBytesLoaded;
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

        public long getEndByte() {
            return endByte;
        }

        public int getID() {
            if (id < 0) {
                if (speedDebug) {
                    logger.finer("INIT " + chunks.indexOf(this));
                }
                id = chunks.indexOf(this);
            }
            return id;
        }

        /**
         * Gibt die Speedgrenze an.
         * 
         * @return
         */
        public int getMaximalSpeed() {
            try {
                maxSpeed = downloadLink.getSpeedLimit() / getRunningChunks();
                if (speedDebug) {
                    logger.finer("Def speed: " + downloadLink.getSpeedLimit() + "/" + getRunningChunks() + "=" + maxSpeed);
                }

                if (speedDebug) {
                    logger.finer("return speed: min " + maxSpeed + " - " + desiredBps * 1.5);
                }
                if (desiredBps < 1024) { return maxSpeed; }
                return Math.min(maxSpeed, (int) (desiredBps * 1.3));
            } catch (Exception e) {
                addException(e);
                error(LinkStatus.ERROR_RETRY, JDUtilities.convertExceptionReadable(e));
            }
            return 0;
        }

        public long getStartByte() {
            return startByte;
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
            if (!downloadLink.isLimited()) { return TIME_BASE; }

            return Math.min(TIME_BASE * 5, (int) (TIME_BASE * bufferTimeFaktor));

        }

        /**
         * Gibt die geladenen Partbytes zurück. Das entsüricht bei resumen nicht
         * den Chunkbytes!!!
         * 
         * @return
         */
        public long getTotalPartBytesLoaded() {
            return totalPartBytes;
        }

        /**
         * Gibt die Schreibposition des Chunks in der gesamtfile zurück
         */
        public long getWritePosition() {
            long c = getCurrentBytesPosition();
            long l = buffer.limit();
            return c - l;
        }

        /**
         * Gibt zurück ob der chunk von einem externen eregniss unterbrochen
         * wurde
         * 
         * @return
         */
        private boolean isExternalyAborted() {
            return isInterrupted();
        }

        // /**
        // * Einige Anbieter erlauben das resumen von files, aber nicht
        // * multistreamloading. Dazu verbieten sie die range 0-xxx. Um das zu
        // * umgehen werden die ersten bytes via preloading geladen und der
        // erste
        // * chunk fängt bei 1-xxx an
        // *
        // * @param preBytes
        // */
        // public long loadPreBytes() {
        //
        // try {
        //
        // InputStream inputStream = connection.getInputStream();
        //
        // if (inputStream.available() > preBytes) {
        // preBytes = inputStream.available();
        // }
        // ReadableByteChannel channel = Channels.newChannel(inputStream);
        // /* max 2 gb buffer */
        // buffer = ByteBuffer.allocateDirect((int) preBytes);
        //
        // while (buffer.hasRemaining()) {
        //
        // channel.read(buffer);
        //
        // }
        // if (speedDebug) {
        // logger.finer("loaded Prebytes " + preBytes);
        // }
        // if (speedDebug) {
        // logger.finer("Preloading produced " + inputStream.available() +
        // " bytes overhead");
        // }
        // inputStream.close();
        // channel.close();
        // connection.getHTTPURLConnection().disconnect();
        //
        // buffer.flip();
        //
        // addPartBytes(buffer.limit());
        // addToTotalLinkBytesLoaded(buffer.limit());
        // addChunkBytesLoaded(buffer.limit());
        // writeBytes(this);
        // return preBytes;
        //
        // } catch (Exception e) {
        // error(LinkStatus.ERROR_DOWNLOAD_FAILED,
        // JDUtilities.convertExceptionReadable(e));
        // addException(e);
        // e.printStackTrace();
        // }
        // return -1;
        //
        // }

        /**
         * Thread runner
         */
        public void run() {
            plugin.setCurrentConnections(plugin.getCurrentConnections() + 1);
            run0();
            plugin.setCurrentConnections(plugin.getCurrentConnections() - 1);
            addToChunksInProgress(-1);

            boolean allConnected = true;
            synchronized (DownloadInterface.this.chunks) {
                for (Chunk chunk : DownloadInterface.this.chunks) {
                    if (chunk.connection == null || !chunk.connection.isConnected()) {
                        allConnected = false;
                        break;
                    }
                }
            }
            if (allConnected) {
                try {
                    this.connection.disconnect();
                } catch (Exception e) {
                }

            }

            if (this.isExternalyAborted()) {
                try {
                    this.connection.disconnect();
                } catch (Exception e) {
                }
            }
            onChunkFinished();
        }

        public void run0() {

            logger.finer("Start Chunk " + getID() + " : " + startByte + " - " + endByte);
            if (startByte >= endByte && endByte > 0 || startByte >= getFileSize() && endByte > 0) {

                // Korrektur Byte
                // logger.severe("correct -1 byte");
                // addToTotalLinkBytesLoaded(-1);

                return;
            }

            if (chunkNum > 1) {
                // if (getPreBytes(this) > 0) {
                // loadPreBytes();
                // if (speedDebug) {
                // logger.finer("After prebytes: " + startByte + " - " +
                // endByte);
                // }
                // }
                connection = copyConnection(connection);

                if (connection == null) {
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.L("download.error.message.connectioncopyerror", "Could not clone the connection"));

                    logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));

                    return;
                }

            } else if (startByte > 0) {
                connection = copyConnection(connection);

                if (connection == null) {
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.L("download.error.message.connectioncopyerror", "Could not clone the connection"));

                    logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));

                    return;
                }

                if (startByte > 0 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.L("download.error.message.rangeheaders", "Server does not support chunkload"));

                    logger.severe("ERROR Chunk (no range header response)" + chunks.indexOf(this));

                    return;

                }
            }

            // Content-Range=[133333332-199999999/200000000]}
            if (startByte > 0) {
                String[][] range = new Regex(connection.getHeaderField("Content-Range"), ".*?(\\d+).*?-.*?(\\d+).*?/.*?(\\d+)").getMatches();
                if (speedDebug) {
                    logger.finer("Range Header " + connection.getHeaderField("Content-Range"));
                }

                if (range == null && chunkNum > 1) {
                    if (connection.getContentLength() == startByte) {
                        // schon fertig
                        return;
                    }
                    error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:") + connection.getHeaderField("Content-Range"));

                    logger.severe("ERROR Chunk (range header parse error)" + chunks.indexOf(this) + connection.getHeaderField("Content-Range") + ": " + connection.getHeaderField("Content-Range"));

                    return;

                } else if (range != null) {
                    long gotSB = JDUtilities.filterLong(range[0][0]);
                    long gotEB = JDUtilities.filterLong(range[0][1]);
                    if (gotSB != startByte) {
                        logger.severe("Range Conflict " + range[0][0] + " - " + range[0][1] + " wished start: " + 0);
                    }

                    if (endByte <= 0) {
                        endByte = gotEB;
                    } else {
                        if (gotEB == endByte) {
                            logger.finer("ServerType: RETURN Rangeend-1");
                        } else if (gotEB == endByte + 1) {
                            logger.finer("ServerType: RETURN exact rangeend");
                        }
                        if (gotEB < endByte) {
                            logger.severe("Range Conflict " + range[0] + " - " + range[1] + " wishedend: " + endByte);
                        }
                        if (gotEB > endByte + 1) {
                            logger.warning("Possible RangeConflict or Servermisconfiguration. wished endByte: " + endByte + " got: " + gotEB);
                        }
                        endByte = Math.min(endByte, gotEB);

                    }

                    if (speedDebug) {
                        logger.finer("Resulting Range" + startByte + " - " + endByte);
                    }
                } else {

                    endByte = connection.getContentLength() - 1;
                    if (speedDebug) {
                        logger.finer("Endbyte set to " + endByte);
                    }
                }
            }
            if (endByte <= 0) {

                endByte = connection.getContentLength() - 1;
                if (speedDebug) {
                    logger.finer("Endbyte set to " + endByte);
                }
            }

            if (isInterrupted() || downloadLink.isAborted()) {
                // error(ERROR_ABORTED_BY_USER);
                logger.severe("ABBORTED BY USER");
                // error(LinkStatus.ERROR_DOWNLOAD_FAILED,JDLocale.L(
                // "download.error.message.rangeheaderparseerror","Unexpected
                // rangeheader
                // format:")+connection.getHeaderField("Content-Range"));

            }
            addChunksDownloading(+1);

            download();
            bytesPerSecond = 0;
            desiredBps = 0;
            addChunksDownloading(-1);

            if (isInterrupted() || downloadLink.isAborted()) {
                logger.severe("ABBORTED BY USER");
            }
            logger.finer("Chunk finished " + chunks.indexOf(this) + " " + getBytesLoaded() + " bytes");

        }

        /**
         * Setzt die anzahl der schon geladenen partbytes. Ist für resume
         * wichtig.
         * 
         * @param loaded
         */
        public void setLoaded(long loaded) {
            totalPartBytes = loaded;
            addToTotalLinkBytesLoaded(loaded);
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

        public void startChunk() {

            start();

        }

    }

    public static final int ERROR_REDIRECTED = -1;

    public static Logger logger = JDUtilities.getLogger();

    // private int status = STATUS_INITIALIZED;

    protected int chunkNum = 1;

    private Vector<Chunk> chunks = new Vector<Chunk>();

    private int chunksDownloading = 0;

    private int chunksInProgress = 0;

    protected HTTPConnection connection;

    protected DownloadLink downloadLink;

    private Vector<Integer> errors = new Vector<Integer>();

    private Vector<Exception> exceptions = null;

    protected long fileSize = -1;

    protected LinkStatus linkStatus;

    // protected int maxBytes = -1;

    protected PluginForHost plugin;

    private int readTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 100000);

    private int requestTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000);

    // private int totalLoadedBytes = 0;

    // private boolean aborted = false;

    private boolean resume = false;

    protected boolean speedDebug = false;

    protected long totaleLinkBytesLoaded = 0;

    private boolean waitFlag = true;

    private boolean fatalErrorOccured = false;

    private boolean doFileSizeCheck = true;

    private Request request = null;

    private boolean fileSizeVerified = false;

    private boolean connected;

    private boolean firstChunkRangeless;

    // public DownloadInterface(PluginForHost plugin, DownloadLink downloadLink,
    // HTTPConnection urlConnection) {
    // this(plugin, downloadLink);
    // connection = urlConnection;
    // if (connection.getContentLength() > 0)
    // this.downloadLink.setDownloadSize(connection.getContentLength());
    // this.downloadLink.setName(Plugin.getFileNameFormHeader(connection));
    //
    // fileSize = getFileSize();
    //
    // //
    // int tmp = Math.min(Math.max(1, (int) (fileSize / Chunk.MIN_CHUNKSIZE)),
    // getChunkNum());
    // setChunkNum(Math.min(tmp, plugin.getFreeConnections()));
    // if (tmp != getChunkNum()) {
    // logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
    // setChunkNum(tmp);
    // }
    //
    // }

    private DownloadInterface(PluginForHost plugin, DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
        linkStatus = downloadLink.getLinkStatus();
        linkStatus.setStatusText(JDLocale.L("download.connection.normal", "Download"));

        downloadLink.setDownloadInstance(this);
        this.plugin = plugin;
    }

    public DownloadInterface(PluginForHost plugin, DownloadLink downloadLink, Request request) throws IOException, PluginException {
        this(plugin, downloadLink);
        this.request = request;

    }

    /**
     * Es wird ein headrequest gemacht um die genaue dateigröße zu ermitteln
     * 
     * @return
     * @throws IOException
     * @throws PluginException
     */
    public long head() throws IOException, PluginException {
        Request head = request.toHeadRequest();

        head.load();

        if (this.plugin.getBrowser().isDebug()) logger.finest(head.printHeaders());
        if (head.getContentLength() > 1024) {

            logger.finer("Got filesze from Headrequest: " + head.getContentLength() + " bytes");
            downloadLink.setDownloadSize(fileSize = head.getContentLength());

            String name = Plugin.getFileNameFromDispositionHeader(head.getHttpConnection().getHeaderField("content-disposition"));
            if (name != null) this.downloadLink.setName(name);

            this.setFileSizeVerified(true);
        }
        return fileSize;
    }

    /**
     * Diese Funktion macht einen Request mit absichtlich falschen Range
     * Headern. Es soll ein 416 Fehler Provoziert werden, der die Dateigröße
     * zurückgibt, aber nicht die daten selbst DieFunktion dient zur ermittlung
     * der genauen dateigröße
     * 
     * @return
     * @throws IOException
     * @throws PluginException
     */
    public long headFake(String value) throws IOException, PluginException {
        request.getHeaders().put("Range", value == null ? "bytes=" : value);

        request.connect();

        if (this.plugin.getBrowser().isDebug()) logger.finest(request.printHeaders());

        // if (request.getHttpConnection().getResponseCode() != 416) {
        //
        // logger.severe("Fake head request failed!!!");
        //            
        // }
        request.getHttpConnection().disconnect();

        if (this.downloadLink.getFinalFileName() == null) {
            String name = Plugin.getFileNameFromDispositionHeader(request.getHttpConnection().getHeaderField("content-disposition"));
            this.downloadLink.setFinalFileName(name);
        }
        String range = request.getHttpConnection().getHeaderField("Content-Range");
        String length = new Regex(range, ".*?\\/(\\d+)").getMatch(0);
        if (length != null) {
            long size = Long.parseLong(length);
            downloadLink.setDownloadSize(fileSize = size);
            this.setFileSizeVerified(true);
        }
        return fileSize;
    }

    /**
     * Gibt zurück ob die Dateigröße 100% richtig ermittelt werden konnte
     * 
     * @return
     */
    public boolean isFileSizeVerified() {
        return fileSizeVerified;
    }

    /**
     * darf NUR dann auf true gesetzt werden, wenn die dateigröße 100% richtig
     * ist!
     * 
     * @param fileSizeVerified
     * @throws PluginException
     */
    public void setFileSizeVerified(boolean fileSizeVerified) throws PluginException {
        this.fileSizeVerified = fileSizeVerified;
        if (fileSize == 0 && fileSizeVerified) {
            logger.severe("Downloadsize==0");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

        }
    }

    /**
     * Validiert das Chunk Progress array
     * 
     * @return
     */
    protected boolean checkResumabled() {

        if (downloadLink.getChunksProgress() == null || downloadLink.getChunksProgress().length == 0) { return false; }

        long loaded = 0;
        long fileSize = getFileSize();
        int chunks = downloadLink.getChunksProgress().length;
        long part = fileSize / chunks;
        long dif;
        long last = -1;
        for (int i = 0; i < chunks; i++) {
            dif = downloadLink.getChunksProgress()[i] - i * part;
            if (dif < 0) return false;
            if (downloadLink.getChunksProgress()[i] <= last) return false;
            last = downloadLink.getChunksProgress()[i];
            loaded += dif;
        }
        if (chunks > 0) {

            setChunkNum(chunks);

            return true;
        }
        return false;

    }

    public HTTPConnection connect(Browser br) throws Exception {
        HTTPConnection ret = connect();
        br.setRequest(request);
        return ret;
    }

    public HTTPConnection connect() throws Exception {

        if (request == null) throw new IllegalStateException("Wrong Mode. Instance is in direct Connection mode");
        this.connected = true;

        if (this.isResume() && this.checkResumabled()) {

            connectResumable();
        } else {
            if (this.isFileSizeVerified()) {
                int tmp = Math.min(Math.max(1, (int) (downloadLink.getDownloadSize() / Chunk.MIN_CHUNKSIZE)), getChunkNum());
                tmp = Math.min(tmp, plugin.getFreeConnections());

                if (tmp != getChunkNum()) {
                    logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
                    setChunkNum(tmp);
                }
            }
            if (downloadLink.getDownloadSize() > 0 && this.getChunkNum() > 1 && !this.isFirstChunkRangeless()) {
                connectFirstRange();
            } else {
                request.getHeaders().remove("Range");
                request.connect();
            }

        }
        if (this.plugin.getBrowser().isDebug()) logger.finest(request.printHeaders());
        if (request.getLocation() != null) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, DownloadInterface.ERROR_REDIRECTED); }
        connection = request.getHttpConnection();
        if (connection.getRange() != null) {
            // Dateigröße wird aus dem Range-Response gelesen
            if (connection.getRange()[2] > 0) {
                this.setFilesizeCheck(true);
                this.downloadLink.setDownloadSize(connection.getRange()[2]);
            }
        } else {
            if (connection.getContentLength() > 0) {
                this.setFilesizeCheck(true);
                this.downloadLink.setDownloadSize(connection.getContentLength());
            }

        }

        if (this.downloadLink.getFinalFileName() == null) {
            String name = Plugin.getFileNameFormHeader(connection);
            this.downloadLink.setFinalFileName(name);
        }
        fileSize = downloadLink.getDownloadSize();

        return connection;
    }

    private void connectFirstRange() throws IOException {
        long part = downloadLink.getDownloadSize() / this.getChunkNum();
        request.getHeaders().put("Range", "bytes=" + (0) + "-" + (part - 1));
        request.connect();
        if (request.getHttpConnection().getResponseCode() == 416) {
            logger.warning("HTTP/1.1 416 Requested Range Not Satisfiable");
            if (this.plugin.getBrowser().isDebug()) logger.finest(request.printHeaders());
            throw new IllegalStateException("HTTP/1.1 416 Requested Range Not Satisfiable");

        } else if (request.getHttpConnection().getRange() == null) {
            logger.warning("No Chunkload");
            setChunkNum(1);
        } else {
            if (request.getHttpConnection().getRange()[0] != 0) { throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + ". Got range: " + request.getHttpConnection().getHeaderField("Content-Range")); }
            if (request.getHttpConnection().getRange()[1] < (part - 2)) { throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range")); }
            if (request.getHttpConnection().getRange()[1] == request.getHttpConnection().getRange()[2] - 1 && getChunkNum() > 1) {
                logger.warning(" Chunkload Protection.. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));

            } else if (request.getHttpConnection().getRange()[1] > (part - 1)) { throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));

            }

        }

    }

    private void connectResumable() throws IOException {
        // TODO: endrange prüfen

        long[] chunkProgress = downloadLink.getChunksProgress();
        String start, end;
        start = end = "";

        if (this.isFileSizeVerified()) {
            start = chunkProgress[0] == 0 ? "0" : (chunkProgress[0] + 1) + "";
            end = (fileSize / chunkProgress.length) + "";

        } else {
            start = chunkProgress[0] == 0 ? "0" : (chunkProgress[0] + 1) + "";
            end = chunkProgress.length > 1 ? (chunkProgress[1] + 1) + "" : "";

        }
        if (this.isFirstChunkRangeless() && start.equals("0")) {
            request.getHeaders().remove("Range");
        } else {
            request.getHeaders().put("Range", "bytes=" + start + "-" + end);
        }
        request.connect();

    }

    /**
     * Fügt einen Chunk hinzu und startet diesen
     * 
     * @param chunk
     */
    protected void addChunk(Chunk chunk) {
        chunks.add(chunk);

        if (chunkNum == 1) {
            chunk.startChunk();
        } else {
            chunk.startChunk();
        }

    }

    protected synchronized void addChunksDownloading(long i) {

        chunksDownloading += i;

    }

    protected void addException(Exception e) {
        if (exceptions == null) {
            exceptions = new Vector<Exception>();
        }
        exceptions.add(e);
    }

    public synchronized void addToChunksInProgress(long i) {
        chunksInProgress += i;

    }

    protected synchronized void addToTotalLinkBytesLoaded(long block) {
        totaleLinkBytesLoaded += block;

    }

    /**
     * Verteilt den verfügbraen downloadspeed auf die Chunks
     */
    private void assignChunkSpeeds() {
        int MAX_ALLOWED_OVERHEAD = 10 * 1024;
        int allowedLinkSpeed = downloadLink.getSpeedLimit();
        int mChunk = (int) (allowedLinkSpeed / chunkNum * 0.4);
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
            if (Math.abs(overhead) < MAX_ALLOWED_OVERHEAD) {
                it = chunks.iterator();

                while (it.hasNext()) {
                    next = it.next();
                    if (next.isAlive()) {
                        next.checkTimeout(180000);
                        // next.setMaximalSpeed(Math.max(mChunk, (int)
                        // next.bytesPerSecond + overhead / Math.max(1,
                        // getRunningChunks())));
                    }

                }
                return;
            }

            it = chunks.iterator();

            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    next.checkTimeout(180000);
                    next.setMaximalSpeed(Math.max(mChunk, (int) next.bytesPerSecond + overhead / Math.max(1, getRunningChunks())));
                }

            }

        }

    }

    // /**
    // * über error() kann ein fehler gemeldet werden. DIe Methode entscheided
    // * dann ob dieser fehler zu einem Abbruch führen muss
    // *
    // * @param id
    // */
    protected void error(int id, String string) {

        logger.severe("Error occured: " + LinkStatus.toString(id));
        if (errors.indexOf(id) < 0) {
            errors.add(id);
        }
        if (fatalErrorOccured) { return; }
        linkStatus.addStatus(id);

        linkStatus.setErrorMessage(string);
        switch (id) {
        case LinkStatus.ERROR_RETRY:
        case LinkStatus.ERROR_FATAL:
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_FILE_NOT_FOUND:
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
        case LinkStatus.ERROR_LOCAL_IO:
        case LinkStatus.ERROR_NO_CONNECTION:
        case LinkStatus.ERROR_ALREADYEXISTS:
        case LinkStatus.ERROR_LINK_IN_PROGRESS:
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            fatalErrorOccured = true;
            terminate(id);

        }

    }

    /**
     * Gibt die Anzahl der verwendeten Chunks zurück
     * 
     * @return
     */
    public int getChunkNum() {

        return chunkNum;
    }

    public Vector<Chunk> getChunks() {

        return chunks;
    }

    /**
     * Gibt zurüc wieviele Chunks tatsächlich in der Downloadphase sind
     * 
     * @return
     */
    public int getChunksDownloading() {
        return chunksDownloading;
    }

    /**
     * Gibt die aufgetretenen Fehler zurück
     * 
     * @return
     */

    public Vector<Integer> getErrors() {
        return errors;
    }

    public Vector<Exception> getExceptions() {
        return exceptions;
    }

    public File getFile() {
        return new File(downloadLink.getFileOutput());

    }

    /**
     * Gibt eine bestmögliche abschätzung der Dateigröße zurück
     * 
     * @return
     */
    protected long getFileSize() {
        if (fileSize > 0) {

        return fileSize; }
        if (connection != null && connection.getContentLength() > 0) {

        return connection.getContentLength(); }

        if (downloadLink.getDownloadSize() > 0) {

        return downloadLink.getDownloadSize();

        }
        return -1;
    }

    /**
     * Gibt den aktuellen readtimeout zurück
     * 
     * @return
     */
    public int getReadTimeout() {
        return Math.max(10000, readTimeout);
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
     * Gibt zurück wieviele Chunks gerade am arbeiten sind
     * 
     * @return
     */
    public int getRunningChunks() {
        return chunksInProgress;
    }

    /**
     * Setzt im Downloadlink und PLugin die entsprechende Fehlerids
     * 
     * @return
     */
    public boolean handleErrors() {

        if (this.doFileSizeCheck && (totaleLinkBytesLoaded <= 0 || totaleLinkBytesLoaded != fileSize && fileSize > 0)) {

            error(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, JDLocale.L("download.error.message.incomplete", "Download unvollständig"));

            if (totaleLinkBytesLoaded > fileSize) {
                logger.severe("loaded more than requested. filesize: " + fileSize + " loaded: " + totaleLinkBytesLoaded + ". This might be  a logic chunk setup error!");
            }
            return false;
        }

        if (getExceptions() != null && getExceptions().size() > 0) {
            error(LinkStatus.ERROR_RETRY, JDLocale.L("download.error.message.incomplete", "Download unvollständig"));

            return false;
        }
        if (!linkStatus.isFailed()) {
            linkStatus.setStatus(LinkStatus.FINISHED);
        }
        return true;
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
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink
     * regelmäsig und fordert beim Controller eine aktualisierung des links an
     */
    private void onChunkFinished() {
        synchronized (this) {
            if (waitFlag) {
                waitFlag = false;
                notify();
            }

        }
    }

    /**
     * Wird aufgerufen sobald alle Chunks fertig geladen sind
     * 
     * @throws DownloadFailedException
     */
    abstract protected void onChunksReady();

    /**
     * Gibt die Anzahl der Chunks an die dieser Download verwenden soll. Chu8nks
     * können nur vor dem Downloadstart gesetzt werden!
     * 
     * @param num
     */
    public void setChunkNum(int num) {
        // if (status != STATUS_INITIALIZED) {
        // logger.severe("CHunks musst be set before starting download");
        // return;
        // }
        if (num <= 0) {
            logger.severe("Chunks value must be >=1");
            return;
        }
        chunkNum = num;
    }

    /**
     * Setzt die filesize.
     * 
     * @param length
     */
    public void setFilesize(long length) {
        fileSize = length;

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
     * Setzt den aktuellen readtimeout(nur vor dem dl start)
     * 
     * @param readTimeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
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
     * File soll resumed werden
     * 
     * @param value
     */
    public void setResume(boolean value) {
        if (checkResumabled()) {

            resume = value;
        } else {
            logger.warning("Resumepoint not valid");

        }
    }

    /**
     * Wird aufgerufen um die Chunks zu initialisieren
     * 
     * @throws Exception
     * 
     * @throws IOException
     * 
     */
    abstract protected void setupChunks() throws Exception;

    /**
     * Startet den Download. Nach dem Aufruf dieser Funktion können keine
     * Downlaodparameter mehr gesetzt werden bzw bleiben wirkungslos.
     * 
     * @return
     * @throws Exception
     */
    public boolean startDownload() throws Exception {
        if (!connected) connect();
        DownloadLink block = JDUtilities.getController().getLinkThatBlocks(downloadLink);
        downloadLink.getLinkStatus().setStatusText(null);
        if (connection.getHeaderField("Location") != null) {

            error(LinkStatus.ERROR_PLUGIN_DEFEKT, "Sent a redirect to Downloadinterface");
            return false;
        }
        if (block != null) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            // linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);

            error(LinkStatus.ERROR_LINK_IN_PROGRESS, String.format(JDLocale.L("system.download.errors.linkisBlocked", "Mirror %s is loading"), block.getPlugin().getHost()));
            if (!handleErrors()) { return false; }
        }
        File fileOutput = new File(downloadLink.getFileOutput());
        if (fileOutput.getParentFile() == null) {
            error(LinkStatus.ERROR_FATAL, JDLocale.L("system.download.errors.invalidoutputfile", "Invalid Outputfile"));
            if (!handleErrors()) { return false; }
        }
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }

        loop: if (fileOutput.exists()) {

            logger.severe("File already exists. " + fileOutput);
            if (this.downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
                if (new File(downloadLink.getFileOutput()).delete()) {
                    logger.severe("--->Overwritten");
                    break loop;
                } else {

                    error(LinkStatus.ERROR_FATAL, JDLocale.L("system.download.errors.couldnotoverwritejdu", "Update Download Error"));
                    if (!handleErrors()) { return false; }
                }
            }
            if (JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_FILE_EXISTS) == 0) {

                if (new File(downloadLink.getFileOutput()).delete()) {
                    logger.severe("--->Overwritten");
                } else {

                    error(LinkStatus.ERROR_ALREADYEXISTS, JDLocale.L("system.download.errors.couldnotoverwrite", "Could not overwrite existing file"));
                    if (!handleErrors()) { return false; }
                }

            } else {

                error(LinkStatus.ERROR_ALREADYEXISTS, null);
                if (!handleErrors()) { return false; }
            }

        }

        // if (this.maxBytes > 0) {
        // logger.finer("Nibble feature active: " + maxBytes + " rest chunks to
        // 1");
        // chunkNum = 1;
        // }
        try {
            linkStatus.addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            setupChunks();
            waitForChunks();

            onChunksReady();
            linkStatus.removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            if (!handleErrors()) {

                return false;
            } else {

                return true;
            }
        }

        catch (Exception e) {
            handleErrors();
            // if (plugin.getCurrentStep().getStatus() !=
            // PluginStep.STATUS_ERROR) {
            // // e.printStackTrace();
            // linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
            //
            // plugin.getCurrentStep().setParameter(JDUtilities.
            // convertExceptionReadable(e));
            // plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            //
            // }
            linkStatus.removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            return false;
        }

    }

    /**
     * Bricht den Download komplett ab.
     * 
     * @param id
     */
    private void terminate(int id) {

        logger.severe("A critical Downloaderror occured. Terminate...");

        Iterator<Chunk> it = chunks.iterator();
        while (it.hasNext()) {
            it.next().interrupt();

        }

    }

    /**
     * Addiert alle chunkspeeds und giubt diese an den downloadlink weiter
     */
    public void updateSpeed() {
        int speed = 0;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext()) {
                speed += it.next().bytesPerSecond;
            }
        }

        downloadLink.addSpeedValue(speed);

    }

    // public void abort() {
    // this.aborted = true;
    //
    // }

    private void waitForChunks() {
        int i = 0;
        int interval = 150;
        while (chunksInProgress > 0) {
            synchronized (this) {

                if (waitFlag) {
                    try {
                        this.wait(interval);
                    } catch (Exception e) {
                        // e.printStackTrace();
                        Iterator<Chunk> it = chunks.iterator();
                        while (it.hasNext()) {
                            it.next().interrupt();

                        }
                        return;
                    }
                }
            }
            i++;
            waitFlag = true;
            // checkChunkParts();
            downloadLink.setDownloadCurrent(totaleLinkBytesLoaded);
            downloadLink.requestGuiUpdate();
            if (i == 1000 / interval) {

                assignChunkSpeeds();

                i = 0;
            }

        }

    }

    protected synchronized void writeBytes(Chunk chunk) {

        if (writeChunkBytes(chunk)) {

            // if (maxBytes > 0 && getChunkNum() == 1 &&
            // this.totaleLinkBytesLoaded >= maxBytes) {
            // error(ERROR_NIBBLE_LIMIT_REACHED);
            // }
        }
        // 152857135
        // logger.info("Bytes " + totalLoadedBytes);
    }

    /**
     * Schreibt den puffer eines chunks in die zugehörige Datei
     * 
     * @param buffer
     * @param currentBytePosition
     */

    abstract protected boolean writeChunkBytes(Chunk chunk);

    public void setFilesizeCheck(boolean b) {
        this.doFileSizeCheck = b;

    }

    public HTTPConnection getConnection() {
        // TODO Auto-generated method stub
        return this.connection;
    }

    public Request getRequest() {
        // TODO Auto-generated method stub
        return this.request;
    }

    /**
     * Setzt man diesen Wert auf true, so wird der erste Chunk nicht per ranges
     * geladen. d.h. es gibt keinen 0-...range
     * 
     * @param b
     */
    public void setFirstChunkRangeless(boolean b) {
        firstChunkRangeless = b;

    }

    public boolean isFirstChunkRangeless() {
        return firstChunkRangeless;
    }

}
