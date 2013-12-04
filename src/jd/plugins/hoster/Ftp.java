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

import java.io.IOException;
import java.net.URL;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.SimpleFTP;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.SimpleFTPDownloadInterface;

// DEV NOTES:
// - ftp filenames can contain & characters!

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?(?<!(hdmekani))\\.[a-zA-Z0-9]{2,}(:\\d+)?/[^\"\r\n ]+" }, flags = { 0 })
public class Ftp extends PluginForHost {

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getCustomFavIconURL(DownloadLink link) {
        if (link != null) {
            String domain = Browser.getHost(link.getDownloadURL(), true);
            if (domain != null) { return domain; }
        }
        return null;
    }

    public void download(String ftpurl, final DownloadLink downloadLink, boolean throwException) throws Exception {
        SimpleFTP ftp = new SimpleFTP();
        try {
            ftp.setLogger(logger);
            URL url = new URL(ftpurl);
            /* cut off all ?xyz at the end */
            String filePath = new Regex(ftpurl, "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            String name = null;
            ftp.connect(url);
            /* switch binary mode */
            ftp.bin();
            /*
             * some servers do not allow to list the folder, so this may fail but file still might be online
             */
            if (!filePath.startsWith("/")) filePath = "/" + filePath;
            long size = ftp.getSize(Encoding.urlDecode(filePath, false));
            if (size == -1) {
                if (ftp.wasLatestOperationNotPermitted()) {
                    String[] list = ftp.getFileInfo(Encoding.urlDecode(filePath, false));
                    if (list != null) {
                        if (list.length == 4) {
                            size = Long.parseLong(list[2]);
                        } else if (list.length == 7) size = Long.parseLong(list[4]);
                    }
                } else {
                    /* some server need / at the beginning */
                    filePath = "/" + filePath;
                    size = ftp.getSize(Encoding.urlDecode(filePath, false));
                }
            }
            if (size != -1) {
                downloadLink.setVerifiedFileSize(size);
                /* cut off all ?xyz at the end */
                name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                if (name == null) {
                    logger.severe("could not get filename from ftpurl");
                    name = downloadLink.getName();
                }
                name = Encoding.urlDecode(name, false);
                downloadLink.setFinalFileName(name);
            }
            if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            dl = new SimpleFTPDownloadInterface(ftp, downloadLink, Encoding.urlDecode(filePath, false));
            dl.startDownload();
        } catch (IOException e) {
            if (throwException && e.getMessage() != null && e.getMessage().contains("530")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Login incorrect");
            } else
                throw e;
        } finally {
            try {
                ftp.disconnect();
            } catch (final Throwable e) {
            }
        }
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        SimpleFTP ftp = new SimpleFTP();
        try {
            ftp.setLogger(logger);
            URL url = new URL(downloadLink.getDownloadURL());
            /* cut off all ?xyz at the end */
            String filePath = new Regex(downloadLink.getDownloadURL(), "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            ftp.connect(url);
            String name = null;
            /* switch binary mode */
            ftp.bin();
            /*
             * some servers do not allow to list the folder, so this may fail but file still might be online
             */
            if (!filePath.startsWith("/")) filePath = "/" + filePath;
            long size = ftp.getSize(Encoding.urlDecode(filePath, false));
            if (size == -1) {
                if (ftp.wasLatestOperationNotPermitted()) {
                    String[] list = ftp.getFileInfo(Encoding.urlDecode(filePath, false));
                    if (list != null) {
                        if (list.length == 4) {
                            size = Long.parseLong(list[2]);
                        } else if (list.length == 7) size = Long.parseLong(list[4]);
                    }
                } else {
                    /* some server need / at the beginning */
                    filePath = "/" + filePath;
                    size = ftp.getSize(Encoding.urlDecode(filePath, false));
                }
            }
            if (size != -1) {
                downloadLink.setVerifiedFileSize(size);
                /* cut off all ?xyz at the end */
                name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                if (name == null) {
                    logger.severe("could not get filename from ftpurl");
                    name = downloadLink.getName();
                }
                name = Encoding.urlDecode(name, false);
                downloadLink.setFinalFileName(name);
            }
            if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            if (e.getMessage().contains("530")) {
                downloadLink.getLinkStatus().setErrorMessage("Login incorrect");
                return AvailableStatus.UNCHECKABLE;
            } else
                throw e;
        } catch (Exception e) {
            logger.severe(e.getMessage());
            throw e;
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