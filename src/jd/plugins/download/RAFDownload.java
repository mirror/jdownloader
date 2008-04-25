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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class RAFDownload extends DownloadInterface {
    public RAFDownload(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        super(plugin, downloadLink, urlConnection);

    }

    protected long writeTimer = System.currentTimeMillis();

    protected long writeCount = 0;

    protected long hdWritesPerSecond;

    protected FileChannel[] channels;

    protected File[] partFiles;

    private FileChannel outputChannel;

    private RandomAccessFile outputFile;

    protected void writeChunkBytes(Chunk chunk) {
        try {
            // int limit = chunk.buffer.limit()-chunk.buffer.position();
            if (maxBytes < 0) {
                synchronized (outputChannel) {
                    outputFile.seek(chunk.getWritePosition());
                    outputChannel.write(chunk.buffer);
                }
            } else {
                chunk.buffer.clear();
            }
           
        } catch (Exception e) {

            e.printStackTrace();
            error(ERROR_LOCAL_IO);
            addException(e);
        }

    }

    @Override
    protected void setupChunks() {
        try {

            boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", true);
            fileSize = getFileSize();
            if (correctChunks) {

                int tmp = Math.min(Math.max(1, (int) (fileSize / Chunk.MIN_CHUNKSIZE)), getChunkNum());
                if (tmp != getChunkNum()) {
                    logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
                    this.setChunkNum(tmp);
                }
            }

            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
            downloadLink.setDownloadMax((int) fileSize);
            setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
            if (checkResumabled() && plugin.getFreeConnections() >= getChunkNum() && maxBytes < 0) {
                logger.info("Resume: " + fileSize);
                long parts = fileSize / getChunkNum();

                Chunk chunk;

                outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                outputChannel = outputFile.getChannel();
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {
                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i] + 1, -1, connection);
                        chunk.setLoaded((int) (downloadLink.getChunksProgress()[i] - i * parts + 1));
                    } else {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection);
                        chunk.setLoaded((int) (downloadLink.getChunksProgress()[i] - i * parts + 1));
                    }

                    addChunk(chunk);
                }

            } else {
                if (maxBytes > 0) this.setChunkNum(1);
                this.setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
                this.totaleLinkBytesLoaded = 0;
                downloadLink.setDownloadCurrent(0);
                long parts = fileSize > 0 ? fileSize / getChunkNum() : -1;
                if (parts == -1) {
                    logger.warning("Could not get Filesize.... reset chunks to 1");
                    setChunkNum(1);
                }
                logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + parts);

                // downloadLink.setChunksProgress(new int[chunkNum]);
                Chunk chunk;
                if (maxBytes < 0) outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                if (maxBytes < 0) outputChannel = outputFile.getChannel();
                downloadLink.setChunksProgress(new int[chunkNum]);
                logger.info("Filesize = " + fileSize);
                logger.info("Partsize = " + parts);
                // int total=0;
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {

                    if (i == (getChunkNum() - 1)) {

                        if (maxBytes > 0) {
                            chunk = new Chunk(0, maxBytes, connection);
                            logger.info("NIBBELING: Just load the first " + (maxBytes + 1) + "Bytes");
                        } else {
                            chunk = new Chunk(i * parts, -1, connection);
                            // total+=(fileSize-i * parts);
                            logger.info("+part " + (fileSize - i * parts));
                        }

                    } else {
                        chunk = new Chunk(i * parts, (i + 1) * parts - 1, connection);
                        // total+=((i + 1) * parts-i * parts);
                        logger.info("+part " + ((i + 1) * parts - i * parts));
                    }

                    addChunk(chunk);
                }
                // logger.info("Total splitted size: "+total);
            }

        } catch (Exception e) {
            try {
                outputChannel.force(false);

                outputFile.close();
                outputChannel.close();
                e.printStackTrace();
            } catch (Exception e2) {
            }
            addException(e);
        }

    }

    private boolean checkResumabled() {

        if (!isResume() || downloadLink.getChunksProgress() == null) return false;

        int loaded = 0;
        int fileSize = (int) getFileSize();
        int chunks = downloadLink.getChunksProgress().length;
        int part = fileSize / chunks;

        for (int i = 0; i < chunks; i++) {
            loaded += downloadLink.getChunksProgress()[i] - i * part;
        }
        if (chunks > 0) {

            this.setChunkNum(chunks);
            logger.info("Resume with " + chunks + " chunks");

            return true;
        }
        return false;

    }

    @Override
    protected void onChunksReady() {

        System.gc();
        System.runFinalization();
        //
        try {

            if (maxBytes < 0) this.outputChannel.force(false);
            if (maxBytes < 0) outputFile.close();
            if (maxBytes < 0) outputChannel.close();
            if (!handleErrors()) {
            
            return; }
            if (!new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))) {

                logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
                error(ERROR_COULD_NOT_RENAME);

            }
            DownloadLink sfv;
            if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_DO_CRC, false) && (sfv = downloadLink.getFilePackage().getSFV()) != null) {
                if (sfv.getStatus() == DownloadLink.STATUS_DONE) {
                    long crc = JDUtilities.getCRC(new File(downloadLink.getFileOutput()));
                    
                    String sfvText = JDUtilities.getLocalFile(new File(sfv.getFileOutput()));
                    if (sfvText != null&&sfvText.toLowerCase().contains(new File(downloadLink.getFileOutput()).getName().toLowerCase())) {
                        String[] l = JDUtilities.splitByNewline(sfvText);
                        boolean c = false;
                        for (String line : l) {
                            logger.info(line+" - "+Long.toHexString(crc).toUpperCase());
                            if (line.trim().endsWith(Long.toHexString(crc).toUpperCase()) || line.trim().endsWith(Long.toHexString(crc).toLowerCase())) {
                                c = true;
                                logger.info("CRC CHECK SUCCESS");
                                break;
                            }
                        }
                        if (c) {
                            downloadLink.setCrcStatus(DownloadLink.CRC_STATUS_OK);
                        } else {
                            downloadLink.setCrcStatus(DownloadLink.CRC_STATUS_BAD);
                            error(ERROR_CRC);
                            
                        }

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            addException(e);
        }

    }

}
