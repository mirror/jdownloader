//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.FtpEvent;
import jd.nutils.FtpListener;
import jd.nutils.JDHash;
import jd.nutils.SimpleFTP;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

// DEV NOTES:
// - ftp filenames can contain & characters!

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?(?<!(hdmekani))\\.[a-zA-Z0-9]{2,}(:\\d+)?/[^\"\r\n ]+" }, flags = { 0 })
public class Ftp extends PluginForHost {

    Long speed = 0L;

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void download(String ftpurl, final DownloadLink downloadLink, boolean throwException) throws IOException, PluginException {
        SimpleFTP ftp = new SimpleFTP();
        try {
            ftp.setLogger(logger);
        } catch (final Throwable e) {
            /* does not exist in 09581 stable */
        }
        try {
            if (new File(downloadLink.getFileOutput()).exists()) throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
            URL url = new URL(ftpurl);
            /* cut off all ?xyz at the end */
            String filePath = new Regex(ftpurl, "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            String name = null;
            ftp.connect(url);
            if (oldStyle()) {
                /*
                 * old style, list folder content and then change into folder to retrieve the file
                 */
                if (!filePath.contains("/")) filePath = "/" + filePath;
                String[] list = ftp.getFileInfo(Encoding.urlDecode(filePath, false));
                if (list != null) {
                    /* list command worked */
                    /* cut off all ?xyz at the end */
                    name = new Regex(ftpurl, ".*/(.+?)(\\?|$)").getMatch(0);
                    if (name == null) {
                        logger.severe("could not get filename from ftpurl");
                        name = downloadLink.getName();
                    }
                    name = Encoding.urlDecode(name, false);
                    downloadLink.setFinalFileName(name);
                    if (list.length == 4) downloadLink.setDownloadSize(Long.parseLong(list[2]));
                    if (list.length == 7) downloadLink.setDownloadSize(Long.parseLong(list[4]));
                    String path = Encoding.urlDecode(filePath.substring(0, filePath.lastIndexOf("/")), false);
                    if (path.length() > 0) ftp.cwd(path);
                    /* switch binary mode */
                    ftp.bin();
                }
            } else {
                /*
                 * new style, do a getSize request and then switch to binary and retrieve file by complete path
                 */
                /* switch binary mode */
                ftp.bin();
                /*
                 * some servers do not allow to list the folder, so this may fail but file still might be online
                 */
                long size = ftp.getSize(Encoding.urlDecode(filePath, false));
                if (size != -1) {
                    downloadLink.setDownloadSize(size);
                    /* cut off all ?xyz at the end */
                    name = new Regex(ftpurl, ".*/(.+?)(\\?|$)").getMatch(0);
                    if (name == null) {
                        logger.severe("could not get filename from ftpurl");
                        name = downloadLink.getName();
                    }
                    name = Encoding.urlDecode(name, false);
                    downloadLink.setFinalFileName(name);
                } else {
                    /* some server need / at the beginning */
                    filePath = "/" + filePath;
                    size = ftp.getSize(Encoding.urlDecode(filePath, false));
                    if (size != -1) {
                        downloadLink.setDownloadSize(size);
                        /* cut off all ?xyz at the end */
                        name = new Regex(ftpurl, ".*/(.+?)(\\?|$)").getMatch(0);
                        if (name == null) {
                            logger.severe("could not get filename from ftpurl");
                            name = downloadLink.getName();
                        }
                        name = Encoding.urlDecode(name, false);
                        downloadLink.setFinalFileName(name);
                    }
                }
            }
            if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            try {
                ftp.getBroadcaster().addListener(new FtpListener() {
                    private long before   = 0;
                    private long last     = 0;
                    private long lastTime = System.currentTimeMillis();

                    public void onDownloadProgress(FtpEvent event) {
                        downloadLink.setDownloadCurrent(event.getProgress());
                        if (System.currentTimeMillis() - lastTime > 1000) {
                            last = event.getProgress();
                            speed = ((last - before) / (System.currentTimeMillis() - lastTime)) * 1000l;
                            lastTime = System.currentTimeMillis();
                            before = last;
                            downloadLink.setChunksProgress(new long[] { last });
                        }
                    }

                });
            } catch (Throwable e) {
                /* stable does not have appwork utils */
                downloadLink.getLinkStatus().setStatusText("ProgressBar not supported");
            }

            File tmp = null;
            /*
             * we need dummy browser for RAFDownload, else nullpointer will happen
             */
            br = new Browser();
            RAFDownload raf = new RAFDownload(this, downloadLink, null);
            raf.setResume(false);
            raf.addChunksDownloading(1);
            dl = raf;
            downloadLink.setDownloadInstance(dl);
            try {
                ftp.setCmanager(dl.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            try {
                ftp.setDownloadInterface(dl);
            } catch (final Throwable e) {
            }
            downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            try {
                try {
                    downloadLink.getDownloadLinkController().getConnectionHandler().addConnectionHandler(dl.getManagedConnetionHandler());
                } catch (final Throwable e) {
                }
                if (oldStyle()) {
                    /*
                     * in old style we moved into the folder and only need to retrieve the file by name
                     */
                    ftp.download(name, tmp = new File(downloadLink.getFileOutput() + ".part"));
                } else {
                    /*
                     * in new style we need to retrieve the file by complete path
                     */
                    try {
                        ftp.download(Encoding.urlDecode(filePath, false), tmp = new File(downloadLink.getFileOutput() + ".part"), downloadLink.getBooleanProperty("RESUME", true));
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().contains("Resume not supported")) {
                            /* resume not supported, retry without resume */
                            downloadLink.setProperty("RESUME", false);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else {
                            throw e;
                        }
                    }
                }
            } finally {
                try {
                    downloadLink.setDownloadCurrent(tmp.length());
                    ftp.setDownloadInterface(null);
                } catch (final Throwable e) {
                }
                downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
                downloadLink.setDownloadInstance(null);
                downloadLink.getLinkStatus().setStatusText(null);
            }
            if (tmp.length() != downloadLink.getDownloadSize()) {
                if (oldStyle() || downloadLink.getBooleanProperty("RESUME", true) == false) {
                    tmp.delete();
                }
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
            }

            if (!isEmpty(downloadLink.getMD5Hash()) && !downloadLink.getMD5Hash().equalsIgnoreCase(JDHash.getMD5(tmp))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " CRC error"); }

            if (!isEmpty(downloadLink.getSha1Hash()) && !downloadLink.getSha1Hash().equalsIgnoreCase(JDHash.getSHA1(tmp))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " CRC error"); }

            if (!tmp.renameTo(new File(downloadLink.getFileOutput()))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " Rename failed. file exists?"); }
            downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
        } catch (IOException e) {
            if (throwException && e.getMessage() != null && e.getMessage().contains("530")) {
                downloadLink.getLinkStatus().setErrorMessage("Login incorrect");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else
                throw e;
        } finally {
            try {
                ftp.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private boolean isEmpty(String s) {
        if (s == null || s.trim().length() == 0) return true;
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://jdownloader.org";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        download(downloadLink.getDownloadURL(), downloadLink, false);
    }

    /* old simpleftp does not have size command support */
    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        SimpleFTP ftp = new SimpleFTP();
        try {
            ftp.setLogger(logger);
        } catch (final Throwable e) {
            /* does not exist in 09581 stable */
        }
        try {
            URL url = new URL(downloadLink.getDownloadURL());
            /* cut off all ?xyz at the end */
            String filePath = new Regex(downloadLink.getDownloadURL(), "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            ftp.connect(url);
            String name = null;
            if (oldStyle() || true) {
                if (!filePath.contains("/")) filePath = "/" + filePath;
                String[] list = ftp.getFileInfo(Encoding.urlDecode(filePath, false));
                if (list != null) {
                    /* list command worked */
                    /* cut off all ?xyz at the end */
                    name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                    if (name == null) {
                        logger.severe("could not get filename from ftpurl");
                        name = downloadLink.getName();
                    }
                    name = Encoding.urlDecode(name, false);
                    downloadLink.setFinalFileName(name);
                    if (list.length == 4) downloadLink.setDownloadSize(Long.parseLong(list[2]));
                    if (list.length == 7) downloadLink.setDownloadSize(Long.parseLong(list[4]));
                }
            } else {
                /* switch binary mode */
                ftp.bin();
                /*
                 * some servers do not allow to list the folder, so this may fail but file still might be online
                 */
                long size = ftp.getSize(Encoding.urlDecode(filePath, false));
                if (size != -1) {
                    downloadLink.setDownloadSize(size);
                    /* cut off all ?xyz at the end */
                    name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                    if (name == null) {
                        logger.severe("could not get filename from ftpurl");
                        name = downloadLink.getName();
                    }
                    name = Encoding.urlDecode(name, false);
                    downloadLink.setFinalFileName(name);
                } else {
                    /* some server need / at the beginning */
                    filePath = "/" + filePath;
                    size = ftp.getSize(Encoding.urlDecode(filePath, false));
                    if (size != -1) {
                        downloadLink.setDownloadSize(size);
                        /* cut off all ?xyz at the end */
                        name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                        if (name == null) {
                            logger.severe("could not get filename from ftpurl");
                            name = downloadLink.getName();
                        }
                        name = Encoding.urlDecode(name, false);
                        downloadLink.setFinalFileName(name);
                    }
                }
            }
            if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            if (e.getMessage().contains("530")) {
                downloadLink.getLinkStatus().setErrorMessage("Login incorrect");
                return AvailableStatus.UNCHECKABLE;
            } else
                throw e;
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            logger.severe(e.getMessage());
        } finally {
            try {
                ftp.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("RESUME", true);
    }

    @Override
    public void resetPluginGlobals() {
    }
}