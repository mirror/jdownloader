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

package jd.plugins.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
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
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface.DownloadFailedException;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RAFDownload extends DownloadInterface {
    public RAFDownload(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        super(plugin, downloadLink, urlConnection);

    }

    protected long           writeTimer = System.currentTimeMillis();

    protected long           writeCount = 0;

    protected long           hdWritesPerSecond;

    protected FileChannel[]  channels;

    protected File[]         partFiles;

    private FileChannel      outputChannel;

    private RandomAccessFile outputFile;

    protected void addBytes(Chunk chunk) {
        try {
            int limit = chunk.buffer.limit()-chunk.buffer.position();
            synchronized(outputChannel){           
                outputFile.seek( chunk.currentBytePosition);
                outputChannel.write(chunk.buffer);
            }   
            if (maxBytes > 0 && getChunkNum() == 1 && this.bytesLoaded >= maxBytes) {
                error(ERROR_NIBBLE_LIMIT_REACHED);
            }    
            if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.currentBytePosition + limit - 1;

        }
        catch (Exception e) {

            e.printStackTrace();
            error(ERROR_LOCAL_IO);
        }

    }

    @Override
    protected void setupChunks() throws DownloadFailedException {
        try {
  
            boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", true);
            long fileSize = getFileSize();
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
            if (checkResumabled() && plugin.getFreeConnections() >= getChunkNum()) {
                logger.info("Resume: " + fileSize);
                long parts = fileSize / getChunkNum();
                // if (parts == -1) {
                // logger.warning("Could not get Filesize.... reset chunks to
                // 1");
                // setChunkNum(1);
                // }
                // logger.finer("Start Download in " + getChunkNum() + " chunks.
                // Chunksize: " + parts);

                // downloadLink.setChunksProgress(new int[chunkNum]);
                Chunk chunk;

                outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                outputChannel = outputFile.getChannel();
                for (int i = 0; i < getChunkNum(); i++) {
                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i], -1, connection);
                    }
                    else {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i], (i + 1) * parts, connection);
                    }

                    addChunk(chunk);
                }

            }
            else {
                this.setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
                this.bytesLoaded = 0;
                downloadLink.setDownloadCurrent(0);
                long parts = fileSize > 0 ? fileSize / getChunkNum() : -1;
                if (parts == -1) {
                    logger.warning("Could not get Filesize.... reset chunks to 1");
                    setChunkNum(1);
                }
                logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + parts);

                // downloadLink.setChunksProgress(new int[chunkNum]);
                Chunk chunk;
                outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                outputChannel = outputFile.getChannel();
                downloadLink.setChunksProgress(new int[chunkNum]);
                for (int i = 0; i < getChunkNum(); i++) {

                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(i * parts, -1, connection);
                    }
                    else {
                        chunk = new Chunk(i * parts, (i + 1) * parts, connection);
                    }

                    addChunk(chunk);
                }
            }

        }
        catch (Exception e) {
            try {
                outputChannel.force(false);

                outputFile.close();
                outputChannel.close();
                e.printStackTrace();
            }
            catch (Exception e2) {
            }
            throw new DownloadFailedException("Chunksetup failed: " + JDUtilities.convertExceptionReadable(e));
        }

    }

    private boolean checkResumabled() {
        logger.info("prog " + downloadLink.getChunksProgress());
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
            this.bytesLoaded = loaded;
            downloadLink.setDownloadCurrent(bytesLoaded);
            return true;
        }
        return false;

    }

    @Override
    protected void onChunksReady() throws DownloadFailedException {

       
        //
        try {

            this.outputChannel.force(false);
            outputFile.close();
            outputChannel.close();
            if (!handleErrors()) {

                throw new DownloadFailedException("Download failed after chunks were ready");
            }
          if(!  new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))){
              
              logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
              error(ERROR_COULD_NOT_RENAME);

              throw new DownloadFailedException("Rename error");
          }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new DownloadFailedException("Chunkfile not found");
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new DownloadFailedException("IO Merge Error");
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DownloadFailedException(JDUtilities.convertExceptionReadable(e));
        }

    }

}
