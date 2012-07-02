//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDog.DISKSPACECHECK;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class RAFDownload extends DownloadInterface {

    public static final Object HASHCHECKLOCK = new Object();
    private RandomAccessFile   outputPartFile;
    private File               outputCompleteFile;

    public RAFDownload(PluginForHost plugin, DownloadLink downloadLink, Request request) throws IOException, PluginException {
        super(plugin, downloadLink, request);
    }

    @Override
    protected void onChunksReady() {
        logger.info("Close connections if they are not closed yet");
        try {
            for (Chunk c : this.getChunks()) {
                c.closeConnections();
            }
        } finally {
            logger.info("Close File. Let AV programs run");
            try {
                outputPartFile.close();
            } catch (Throwable e) {
            }
        }
        downloadLink.getLinkStatus().setStatusText(null);
        if (!handleErrors()) return;
        try {
            File part = new File(outputCompleteFile.getAbsolutePath() + ".part");
            /* lets check the hash/crc/sfv */
            if (JsonConfig.create(GeneralSettings.class).isHashCheckEnabled()) {
                synchronized (HASHCHECKLOCK) {
                    /*
                     * we only want one hashcheck running at the same time. many finished downloads can cause heavy diskusage here
                     */
                    String hash = null;
                    String type = null;
                    Boolean success = null;
                    if ((hash = downloadLink.getMD5Hash()) != null) {
                        /* MD5 Check */
                        type = "MD5";
                        downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("MD5"));
                        String hashFile = Hash.getMD5(part);
                        success = hash.equalsIgnoreCase(hashFile);
                    } else if ((hash = downloadLink.getSha1Hash()) != null) {
                        /* SHA1 Check */
                        type = "SHA1";
                        downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("SHA1"));
                        String hashFile = Hash.getSHA1(part);
                        success = hash.equalsIgnoreCase(hashFile);
                    } else if ((hash = new Regex(downloadLink.getName(), ".*?\\[([A-Fa-f0-9]{8})\\]").getMatch(0)) != null) {
                        type = "CRC32";
                        String hashFile = Long.toHexString(Hash.getCRC32(part));
                        success = hash.equalsIgnoreCase(hashFile);
                    } else {
                        DownloadLink sfv = null;
                        synchronized (downloadLink.getFilePackage()) {
                            for (DownloadLink dl : downloadLink.getFilePackage().getChildren()) {
                                if (dl.getFileOutput().toLowerCase().endsWith(".sfv")) {
                                    sfv = dl;
                                    break;
                                }
                            }
                        }
                        /* SFV File Available, lets use it */
                        if (sfv != null && sfv.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            String sfvText = IO.readFileToString(new File(sfv.getFileOutput()));
                            if (sfvText != null) {
                                /* Delete comments */
                                sfvText = sfvText.replaceAll(";(.*?)[\r\n]{1,2}", "");
                                if (sfvText != null && sfvText.contains(downloadLink.getName())) {
                                    downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("CRC32"));
                                    type = "CRC32";
                                    String crc = Long.toHexString(Hash.getCRC32(part));
                                    success = new Regex(sfvText, downloadLink.getName() + "\\s*" + crc).matches();
                                }
                            }
                        }
                    }
                    if (success != null) {
                        hashCheckFinished(type, success);
                    }
                }
            }
            boolean renameOkay = false;
            int retry = 5;
            /* rename part file to final filename */
            while (retry > 0) {
                /* first we try normal rename method */
                if ((renameOkay = part.renameTo(outputCompleteFile)) == true) {
                    break;
                }
                /* this may fail because something might lock the file */
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                retry--;
            }
            /* Fallback */
            if (renameOkay == false) {
                /* rename failed, lets try fallback */
                logger.severe("Could not rename file " + part + " to " + outputCompleteFile);
                logger.severe("Try copy workaround!");
                try {
                    DISKSPACECHECK freeSpace = DownloadWatchDog.getInstance().checkFreeDiskSpace(part.getParentFile(), part.length());
                    if (DISKSPACECHECK.FAILED.equals(freeSpace)) throw new Throwable("not enough diskspace free to copy part to complete file");
                    IO.copyFile(part, outputCompleteFile);
                    renameOkay = true;
                    part.deleteOnExit();
                    part.delete();
                } catch (Throwable e) {
                    LogSource.exception(logger, e);
                    /* error happened, lets delete complete file */
                    if (outputCompleteFile.exists() && outputCompleteFile.length() != part.length()) {
                        outputCompleteFile.delete();
                        outputCompleteFile.deleteOnExit();
                    }
                }
                if (!renameOkay) {
                    logger.severe("Copy workaround: :(");
                    error(LinkStatus.ERROR_LOCAL_IO, _JDT._.system_download_errors_couldnotrename());
                } else {
                    logger.severe("Copy workaround: :)");
                }
            }
            if (renameOkay) {
                /* save absolutepath as final location property */
                downloadLink.setProperty(DownloadLink.PROPERTY_FINALLOCATION, outputCompleteFile.getAbsolutePath());
                Date last = TimeFormatter.parseDateString(connection.getHeaderField("Last-Modified"));
                if (last != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    /* set original lastModified timestamp */
                    outputCompleteFile.setLastModified(last.getTime());
                } else {
                    /* set current timestamp as lastModified timestamp */
                    outputCompleteFile.setLastModified(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception", e);
            addException(e);
        }
    }

    private void hashCheckFinished(String hashType, boolean success) {
        logger.info(hashType + "-Check: " + (success ? "ok" : "failed"));
        if (success) {
            downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2_success(hashType));
        } else {
            String error = _JDT._.system_download_doCRC2_failed(hashType);
            downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
            downloadLink.getLinkStatus().setStatusText(error);
            downloadLink.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
            error(LinkStatus.ERROR_DOWNLOAD_FAILED, error);
        }
    }

    @Override
    protected void setupChunks() throws Exception {
        try {
            if (isRangeRequestSupported() && checkResumabled()) {
                logger.finer("Setup resume");
                this.setupResume();
            } else {
                logger.finer("Setup virgin download");
                this.setupVirginStart();
            }
        } catch (Exception e) {
            try {
                logger.info("CLOSE HD FILE");
                outputPartFile.close();
            } catch (Throwable e2) {
            }
            addException(e);
            throw e;
        }
    }

    private void setupVirginStart() throws FileNotFoundException {
        Chunk chunk;
        totalLinkBytesLoaded = 0;
        downloadLink.setDownloadCurrent(0);
        long partSize = getFileSize() / getChunkNum();
        if (connection.getRange() != null) {
            if ((connection.getRange()[1] == connection.getRange()[2] - 1) || (connection.getRange()[1] == connection.getRange()[2])) {
                logger.warning("Chunkload protection. this may cause traffic errors");
                partSize = getFileSize() / getChunkNum();
            } else {
                // Falls schon der 1. range angefordert wurde.... werden die
                // restlichen chunks angepasst
                partSize = (getFileSize() - connection.getLongContentLength()) / (getChunkNum() - 1);
            }
        }
        if (partSize <= 0) {
            logger.warning("Could not get Filesize.... reset chunks to 1");
            setChunkNum(1);
        }
        logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + partSize);

        createOutputChannel();
        downloadLink.setChunksProgress(new long[chunkNum]);

        addToChunksInProgress(getChunkNum());
        int start = 0;

        long rangePosition = 0;
        if (connection.getRange() != null && connection.getRange()[1] != connection.getRange()[2] - 1) {
            // Erster range schon angefordert

            chunk = new Chunk(0, rangePosition = connection.getRange()[1], connection, this);
            rangePosition++;
            logger.finer("Setup chunk 0: " + chunk);
            addChunk(chunk);
            start++;
        }

        for (int i = start; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new Chunk(rangePosition, -1, connection, this);
            } else {
                chunk = new Chunk(rangePosition, rangePosition + partSize - 1, connection, this);
                rangePosition = rangePosition + partSize;
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }

    }

    private void createOutputChannel() throws FileNotFoundException {
        outputCompleteFile = new File(downloadLink.getFileOutput());
        if (!outputCompleteFile.getParentFile().exists()) {
            outputCompleteFile.getParentFile().mkdirs();
        }
        outputPartFile = new RandomAccessFile(outputCompleteFile.getAbsolutePath() + ".part", "rw");
    }

    private void setupResume() throws FileNotFoundException {

        long parts = getFileSize() / getChunkNum();
        logger.info("Resume: " + getFileSize() + " partsize: " + parts);
        Chunk chunk;
        this.createOutputChannel();
        addToChunksInProgress(getChunkNum());

        for (int i = 0; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new Chunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, -1, connection, this);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
            } else {
                chunk = new Chunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection, this);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }

    }

    @Override
    protected boolean writeChunkBytes(Chunk chunk) {
        try {
            synchronized (outputPartFile) {
                outputPartFile.seek(chunk.getWritePosition());
                outputPartFile.write(chunk.buffer.getInternalBuffer(), 0, chunk.buffer.size());
                if (chunk.getID() >= 0) {
                    downloadLink.getChunksProgress()[chunk.getID()] = chunk.getCurrentBytesPosition() - 1;
                }
                return true;
            }
        } catch (Exception e) {
            LogSource.exception(logger, e);
            error(LinkStatus.ERROR_LOCAL_IO, Exceptions.getStackTrace(e));
            addException(e);
            return false;
        }
    }

    /**
     * 
     * @param downloadLink
     *            downloadlink der geladne werden soll (wird zur darstellung verwendet)
     * @param request
     *            Verbindung die geladen werden soll
     * @param b
     *            Resumefaehige verbindung
     * @param i
     *            max chunks. fuer negative werte wirden die chunks aus der config verwendet. Bsp: -3 : Min(3,Configwert);
     * @return
     * @throws IOException
     * @throws PluginException
     */
    public static DownloadInterface download(DownloadLink downloadLink, Request request, boolean b, int i) throws IOException, PluginException {
        /* disable gzip, because current downloadsystem cannot handle it correct */
        request.getHeaders().put("Accept-Encoding", null);
        DownloadInterface dl = new RAFDownload(downloadLink.getLivePlugin(), downloadLink, request);
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        if (i == 0) {
            dl.setChunkNum(JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile());
        } else {
            dl.setChunkNum(i < 0 ? Math.min(i * -1, JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile()) : i);
        }
        dl.setResume(b);
        return dl;

    }

    public static DownloadInterface download(DownloadLink downloadLink, Request request) throws Exception {
        return download(downloadLink, request, false, 1);
    }

    @Override
    public void cleanupDownladInterface() {
        try {
            this.outputPartFile.close();
        } catch (Throwable e) {
        }
    }

}