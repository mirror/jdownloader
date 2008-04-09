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
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ChunkFileDownload extends DownloadInterface {
    public ChunkFileDownload(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        super(plugin, downloadLink, urlConnection);

    }

    protected long          writeTimer = System.currentTimeMillis();

    protected long          writeCount = 0;

    protected long          hdWritesPerSecond;

    protected FileChannel[] channels;

    protected File[]        partFiles;

  

    protected void writeChunkBytes(Chunk chunk) {
        try {
            if (speedDebug) {
                if ((System.currentTimeMillis() - writeTimer) >= 1000) {
                    this.hdWritesPerSecond = writeCount / 1;
                    writeTimer = System.currentTimeMillis();
                    writeCount = 0;
                    logger.info("HD ZUgriffe: " + hdWritesPerSecond);
                }
                this.writeCount++;
            }

            channels[chunk.getID()].write(chunk.buffer);
           // logger.info(chunk.currentBytePosition + " <<size " + chunk.buffer.limit() + " of " + chunk.getID() + " - " + partFiles[chunk.getID()].length());
            if (maxBytes > 0 && getChunkNum() == 1 && this.bytesLoaded >= maxBytes) {
                error(ERROR_NIBBLE_LIMIT_REACHED);
            }
            // if (chunk.getID() >= 0)
            // downloadLink.getChunksProgress()[chunk.getID()] = (int)
            // chunk.currentBytePosition + chunk.buffer.capacity();

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

            String fileName = downloadLink.getName();

            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
            downloadLink.setDownloadMax((int) fileSize);
            setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
            if(checkResumabled()&&plugin.getFreeConnections() >= getChunkNum()){
                // logger.info("Filsize: " + fileSize);
               long parts = fileSize/getChunkNum();
//                if (parts == -1) {
//                    logger.warning("Could not get Filesize.... reset chunks to 1");
//                    setChunkNum(1);
//                }
//                logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + parts);
                
                // downloadLink.setChunksProgress(new int[chunkNum]);
                Chunk chunk;

                channels = new FileChannel[getChunkNum()];
                partFiles = new File[getChunkNum()];
                for (int i = 0; i < getChunkNum(); i++) {
                    partFiles[i] = new File(getPartFileName(i, (int) fileSize));
                    //partFiles[i].delete();
                    logger.info("resume partfile " + i + " - " + partFiles[i].getAbsolutePath()+" at "+partFiles[i].length()+"/"+parts);
                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(i * parts+partFiles[i].length(), -1, connection);
                    }
                    else {
                        chunk = new Chunk(i * parts+partFiles[i].length(), (i + 1) * parts, connection);
                    }
                   

                    channels[i] = new FileOutputStream(partFiles[i], true).getChannel();

                    addChunk(chunk);
                }  
                
                
            }else{
                this.setChunkNum(Math.min(getChunkNum(),plugin.getFreeConnections()));
                this.bytesLoaded=0;
                downloadLink.setDownloadCurrent(0);
            long parts = fileSize > 0 ? fileSize / getChunkNum() : -1;
            if (parts == -1) {
                logger.warning("Could not get Filesize.... reset chunks to 1");
                setChunkNum(1);
            }
            logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + parts);
           
            // downloadLink.setChunksProgress(new int[chunkNum]);
            Chunk chunk;

            channels = new FileChannel[getChunkNum()];
            partFiles = new File[getChunkNum()];
            for (int i = 0; i < getChunkNum(); i++) {
                partFiles[i] = new File(getPartFileName(i, (int) fileSize));
                partFiles[i].delete();
                logger.info("create partfile " + i + " - " + partFiles[i].getAbsolutePath());
                if (i == (getChunkNum() - 1)) {
                    chunk = new Chunk(i * parts, -1, connection);
                }
                else {
                    chunk = new Chunk(i * parts, (i + 1) * parts, connection);
                }

                if (!partFiles[i].exists()) partFiles[i].createNewFile();

                channels[i] = new FileOutputStream(partFiles[i]).getChannel();

                addChunk(chunk);
            }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DownloadFailedException("Chunksetup failed: " + JDUtilities.convertExceptionReadable(e));
        }

    }

    private String getPartFileName(int num, int fileSize) {
        return downloadLink.getFileOutput() + ".part_" + num + "_" + fileSize;
    }

    private boolean checkResumabled() {
        if(!isResume())return false;
        int chunkCount = 0;
        File part;
        int loaded = 0;
        int fileSize = (int) getFileSize();
        while ((part = new File(getPartFileName(chunkCount, (int) fileSize))).exists()) {
            loaded += part.length();
            chunkCount++;
        }
        if (chunkCount > 0) {

            this.setChunkNum(chunkCount);
            logger.info("Resume with " + chunkCount + " chunks");
            this.bytesLoaded=loaded;
            downloadLink.setDownloadCurrent(bytesLoaded);
            return true;
        }
        return false;

    }

    @Override
    protected void onChunksReady() throws DownloadFailedException {
      
        if (!handleErrors()) {
            for (int i = 0; i < getChunkNum(); i++) {
                try {
                    channels[i].force(true);
          
                channels[i].close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            throw new DownloadFailedException("Download failed after chunks were ready");
        }

        FileChannel in;
        FileChannel out;
        try {
            out = new FileOutputStream(downloadLink.getFileOutput(), true).getChannel();

            int coppied = 0;
            downloadLink.setStatusText(JDLocale.L("download.status.merge","Zusammenfügen"));
            long length=0;
            for (int i = 0; i < getChunkNum(); i++) {
                channels[i].force(true);
                channels[i].close();
               in = new FileInputStream(partFiles[i].getAbsolutePath()).getChannel();
                coppied = 0;
           length=partFiles[i].length();

                while (coppied != length) {
                    coppied += in.transferTo(coppied, length, out);
                 logger.info("Partmerge: "+coppied);
                }
                ;

                logger.info("Merged " + partFiles[i].getAbsolutePath() + " to " + downloadLink.getFileOutput());
               
                //in.force(true);
               
                in.close();
                logger.info("Deleted "+partFiles[i].delete());
                partFiles[i] = null;
            }
            System.gc();
            out.force(true);
            out.close();
            return;
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
