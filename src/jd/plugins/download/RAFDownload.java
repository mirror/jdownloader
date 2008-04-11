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

    protected void writeChunkBytes(Chunk chunk) {
        try {
            int limit = chunk.buffer.limit()-chunk.buffer.position();
            if(maxBytes<0){
            synchronized(outputChannel){           
                outputFile.seek( chunk.currentBytePosition);
                outputChannel.write(chunk.buffer);
            }  } else{
                chunk.buffer.clear();
            }
            if (maxBytes > 0 && getChunkNum() == 1 && this.bytesLoaded >= maxBytes) {
                error(ERROR_NIBBLE_LIMIT_REACHED);
            }    
            if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.currentBytePosition + limit;

        }
        catch (Exception e) {

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
            if (checkResumabled() && plugin.getFreeConnections() >= getChunkNum()&&maxBytes<0) {
                logger.info("Resume: " + fileSize);
                long parts = fileSize / getChunkNum();
           
                Chunk chunk;

               outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");
               
                outputChannel = outputFile.getChannel();
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {
                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i]+1, -1, connection);
                        chunk.setLoaded((int)(downloadLink.getChunksProgress()[i]-i * parts+1));
                     }
                    else {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i]+1, (i + 1) * parts-1, connection);
                        chunk.setLoaded((int)(downloadLink.getChunksProgress()[i]-i * parts+1));
                    }

                    addChunk(chunk);
                }

            }
            else {
                if(maxBytes>0)this.setChunkNum(1);
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
                if(maxBytes<0)outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                if(maxBytes<0)outputChannel = outputFile.getChannel();
                downloadLink.setChunksProgress(new int[chunkNum]);
                logger.info("Filesize = "+fileSize);
                logger.info("Partsize = "+parts);
               // int total=0;
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {

                    if (i == (getChunkNum() - 1)) {
                        
                        if(maxBytes>0){
                            chunk = new Chunk(0, maxBytes, connection); 
                            logger.info("NIBBELING: Just load the first "+(maxBytes+1)+"Bytes");
                        }else{
                            chunk = new Chunk(i * parts, -1, connection);  
                           // total+=(fileSize-i * parts);
                            logger.info("+part "+(fileSize-i * parts));
                        }
                      
                    }
                    else {
                        chunk = new Chunk(i * parts, (i + 1) * parts-1, connection);
                        //total+=((i + 1) * parts-i * parts);
                        logger.info("+part "+((i + 1) * parts-i * parts));
                    }

                    addChunk(chunk);
                }
               // logger.info("Total splitted size: "+total);
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

            if(maxBytes<0)   this.outputChannel.force(false);
         if(maxBytes<0)  outputFile.close();
         if(maxBytes<0)  outputChannel.close();
            if (!handleErrors()) {

                return;
            }
          if(!  new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))){
              
              logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
              error(ERROR_COULD_NOT_RENAME);
             
          }
       

        }
        catch (Exception e) {
            e.printStackTrace();
            addException(e);
        }
      
      

    }

}
