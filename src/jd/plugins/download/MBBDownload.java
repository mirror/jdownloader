package jd.plugins.download;

//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program  is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://wnu.org/licenses/>.

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

public class MBBDownload extends DownloadInterface {

    public MBBDownload(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        super(plugin, downloadLink, urlConnection);
        // TODO Auto-generated constructor stub
    }

    private MappedByteBuffer wrBuf;

    /**
     * Schreibt den puffer eines chunks in die zugehörige Datei
     * 
     * @param buffer
     * @param currentBytePosition
     */
    // private synchronized void addBytes(Chunk chunk, int s) {
    // try {
    // if ((System.currentTimeMillis() - writeTimer) >= 1000) {
    // this.hdWritesPerSecond = writeCount / 1;
    // writeTimer = System.currentTimeMillis();
    // writeCount = 0;
    // logger.info("HD ZUgriffe: " + hdWritesPerSecond);
    // }
    // this.writeCount++;
    // // currentBytePosition=-1;
    // this.outputFile.seek(chunk.currentBytePosition);
    //
    // int length = chunk.buffer.limit() - chunk.buffer.position();
    // // byte[] tmp = new byte[length];
    // // logger.info("wrote " + length + " at " + currentBytePosition);
    // // buffer.get(tmp, buffer.position(), length);
    // outputChannel.write(chunk.buffer);
    // // this.outputFile.write(tmp);
    // }
    // catch (Exception e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // error(ERROR_LOCAL_IO);
    // }
    //
    // }
    protected void addBytes(Chunk chunk) {
        try {
            synchronized (wrBuf) {
                wrBuf.position((int) chunk.currentBytePosition);
                wrBuf.put(chunk.buffer);
            }
            if (maxBytes > 0 && (chunk.currentBytePosition + chunk.buffer.capacity()) > maxBytes) {
                error(ERROR_NIBBLE_LIMIT_REACHED);
            }
            if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.currentBytePosition + chunk.buffer.capacity();

        }
        catch (Exception e) {

            e.printStackTrace();
            error(ERROR_LOCAL_IO);
        }

    }

    public void setupChunks() throws DownloadFailedException {
        try {

            RandomAccessFile outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");
         
            FileChannel outputChannel = outputFile.getChannel();
            long fileSize = getFileSize();
            wrBuf = outputChannel.map(FileChannel.MapMode.READ_WRITE, 0, (int) fileSize);
            outputChannel.close();
            outputFile.close();
            outputChannel = null;
            outputFile = null;
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);

            boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", true);
            if (correctChunks) {

                // logger.finer("Linkspeed: " + allowedLinkSpeed);
                int tmp = Math.min(Math.max(1, (int) (fileSize / Chunk.MIN_CHUNKSIZE)), getChunkNum());
                if (tmp != getChunkNum()) {

                    logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
                    setChunkNum(tmp);
                }
            }

            int[] chunksProgress = downloadLink.getChunksProgress();
            boolean isProgressStatusValid = false;
            int[][] loadedRanges = null;

            if (chunksProgress != null && this.isResume() && plugin.getFreeConnections() >= getChunkNum()) {
                try {
                    logger.info("Try to resume download");
               
                    logger.info("Old Chunknum: " + chunksProgress.length);

                    int bc;
                    boolean ch;
                    int bytesLoaded = 0;
                    isProgressStatusValid = true;
                    int supposedParts = (int) (fileSize / chunksProgress.length);
                    int[] startBytes = new int[chunksProgress.length];
                    loadedRanges = new int[chunksProgress.length][2];
                    for (int i = 0; i < chunksProgress.length; i++) {
                        if (i > 0 && chunksProgress[i] <= chunksProgress[i - 1]) {
                            isProgressStatusValid = false;
                            if (speedDebug) logger.info("Resumeerror: chunksorder error at chunk " + i);
                            break;
                        }
                        bc = 0;
                        ch = false;
                        startBytes[i] = i * supposedParts;
                        if (speedDebug) logger.info("Check Startposition " + i + " :" + i * supposedParts);
                        wrBuf.position(i * supposedParts + bc);
                        while (true) {

                            // logger.info(wrBuf.position()+"->"+wrBuf.limit()+"("+wrBuf.capacity()+")");
                            byte b = wrBuf.get();
                            if (b != 0) {
                                ch = true;
                                loadedRanges[i][0] = i * supposedParts + bc;
                                if (speedDebug) logger.info("ok at " + (i * supposedParts + bc));
                                break;
                            }
                            bc++;
                            if (i * supposedParts + bc >= chunksProgress[i]) break;
                        }
                        if (!ch) {
                            isProgressStatusValid = false;
                            if (speedDebug) logger.info("Chunk check failed (forwardsearch)" + i);
                            break;
                        }

                    }

                    if (isProgressStatusValid) {
                        for (int i = 0; i < chunksProgress.length; i++) {
                            bc = 1;

                            if (i > 0 && chunksProgress[i] <= chunksProgress[i - 1]) {
                                isProgressStatusValid = false;
                                break;
                            }
                            if (chunksProgress[i] == 0) continue;
                            ch = false;

                            while (true) {
                                wrBuf.position(chunksProgress[i] - bc);

                                bc++;
                                byte b = wrBuf.get();
                                if (b != 0) {
                                    ch = true;
                                    if (speedDebug) logger.info("Chunk " + i + " OK. " + "Found data at " + (chunksProgress[i] - bc) + " : " + b);
                                    loadedRanges[i][1] = chunksProgress[i] - bc;
                                    if (speedDebug) logger.info("Verified Range " + loadedRanges[i][0] + "- " + loadedRanges[i][1] + " as correctly loaded");

                                    bytesLoaded += Math.max(0, loadedRanges[i][1] - loadedRanges[i][0]);

                                    break;
                                }
                                if ((chunksProgress[i] - bc) < 0 || (i > 0 && (chunksProgress[i] - bc) <= chunksProgress[i - 1])) break;
                            }

                            if (!ch) {
                                isProgressStatusValid = false;
                                if (speedDebug) logger.info("Chunk check failed " + i);
                                break;
                            }

                        }

                    }
                    if (isProgressStatusValid) {
                        this.bytesLoaded = bytesLoaded;
                        logger.info("Cached Chunkprogress is valid. Resuming...");
                        setChunkNum(chunksProgress.length);
                    }
                    else {
                        logger.info("Cached Chunkprogress is invalid. Overwriting...");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (isProgressStatusValid) {

                logger.finer("Start Download in " + getChunkNum() + " chunks. Resume Ranges");
                downloadLink.setDownloadMax((int) fileSize);

                for (int i = 0; i < getChunkNum(); i++) {
                    if (i == (getChunkNum() - 1)) {
                        addChunk(new Chunk(loadedRanges[i][1], fileSize - 1, connection));
                    }
                    else {
                        addChunk(new Chunk(loadedRanges[i][1], loadedRanges[i + 1][0], connection));
                    }

                }
            }
            else {
                setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
                // logger.info("Filsize: " + fileSize);
                long parts = fileSize > 0 ? fileSize / getChunkNum() : -1;
                if (parts == -1) {
                    logger.warning("Could not get Filesize.... reset chunks to 1");
                    chunkNum = 1;
                }
                logger.finer("Start Download in " + chunkNum + " chunks. Chunksize: " + parts);
                downloadLink.setDownloadMax((int) fileSize);
                downloadLink.setChunksProgress(new int[chunkNum]);
                for (int i = 0; i < chunkNum; i++) {
                    if (i == (chunkNum - 1)) {
                        addChunk(new Chunk(i * parts, fileSize - 1, connection));
                    }
                    else {
                        addChunk(new Chunk(i * parts, (i + 1) * parts, connection));
                    }

                }
            }

        }

        catch (Exception e) {

            e.printStackTrace();
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            plugin.getCurrentStep().setParameter(e.getLocalizedMessage());
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            wrBuf = null;
            // gc muss hier aufgerufe werden weil jd sonst die Datei hinter
            // wrBuf nicht freigibt.
            System.gc();
            System.runFinalization();
            throw new DownloadFailedException("Download Fehlgeschlagen. Chunkinitialisierung: " + e.getLocalizedMessage());
        }

    }

    @Override
    protected void onChunksReady() throws DownloadFailedException {

        int bugCount = 0;
        // workaround für einen bug in der map implementation von java.
        while (true) {
            try {
                this.wrBuf.force();
                wrBuf = null;

                System.gc();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                bugCount++;
                if (bugCount > 100) {

                    // error(ERROR_LOCAL_IO);

                }
                try {
                    Thread.sleep(100);
                }
                catch (Exception e2) {

                }

            }

        }

        // gc muss hier aufgerufe werden weil jd sonst die Datei hinter
        // wrBuf nicht freigibt.

        // System.runFinalization();
        if (!handleErrors()) {

            throw new DownloadFailedException("Downloadfehler after Chunks were finished");
        }
        if (!new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))) {
            logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
            error(ERROR_COULD_NOT_RENAME);

            throw new DownloadFailedException("Downloadfehler: Could not rename Part file");
        }
    }

}
