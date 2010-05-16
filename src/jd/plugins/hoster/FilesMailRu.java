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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.mail.ru" }, urls = { "http://[\\w\\.]*?wge4zu4rjfsdehehztiuxw/.*?/[a-z0-9]+" }, flags = { 0 })
public class FilesMailRu extends PluginForHost {

    public FilesMailRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://files.mail.ru/cgi-bin/files/fagreement";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Rename the decrypted links to make them work
        link.setUrlDownload(link.getDownloadURL().replace("wge4zu4rjfsdehehztiuxw", "files.mail.ru"));
    }

    public boolean iHaveToWait = false;
    public String finalLinkRegex = "\"(http://[a-z0-9]+\\.files\\.mail\\.ru/.*?/.*?)\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getName() == null && downloadLink.getStringProperty("folderID", null) == null) {
            logger.warning("final filename and folderID are bot null for unknown reasons!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (downloadLink.getName() != null) downloadLink.setProperty("finalFilename", downloadLink.getName());
        String folderIDregexed = new Regex(downloadLink.getDownloadURL(), "mail\\.ru/([A-Z0-9]+)").getMatch(0);
        downloadLink.setProperty("folderID", "http://files.mail.ru/" + folderIDregexed);
        // At the moment jd gets the russian version of the site. Errorhandling
        // also works for English but filesize handling doesn't so if this
        // plugin get's broken that's on of the first things to check
        try {
            if (!br.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
                long size = br.getHttpConnection().getLongContentLength();
                if (size == 0) {
                    logger.warning("Filesize observed by the direct URL is 0...");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setDownloadSize(Long.valueOf(size));
                iHaveToWait = false;
                return AvailableStatus.TRUE;
            } else {
                logger.info("Directlink is outdated, accessing link " + downloadLink.getStringProperty("folderID", null) + " to renew the ticket!");
                br.getPage(downloadLink.getStringProperty("folderID", null));
                if (br.containsHTML("(was not found|were deleted by sender|Не найдено файлов, отправленных с кодом|<b>Ошибка</b>)")) {
                    logger.info("Link is 100% offline, found errormessage on the page!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                // This part is to find the filename in case the downloadurl
                // redirects to the folder it comes from
                String finalfilename = downloadLink.getStringProperty("finalFilename", null);
                if (finalfilename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String[] linkinformation = br.getRegex("<td class=\"name\">(.*?)<td class=\"do\">").getColumn(0);
                if (linkinformation.length == 0) {
                    logger.warning("Critical error : Couldn't get the linkinformation");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (String info : linkinformation) {
                    if (info.contains(finalfilename)) {
                        // regex the new downloadlink and save it!
                        String directlink = new Regex(info, finalLinkRegex).getMatch(0);
                        if (directlink == null) {
                            logger.warning("Critical error occured: The final downloadlink couldn't be found in the available check!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        downloadLink.setUrlDownload(directlink);
                        // If the previous direct link was wrong we have to set
                        // a new one (code above) and wait about 10 secs
                        iHaveToWait = true;
                        logger.info("Set new UrlDownload, link = " + downloadLink.getDownloadURL() + " waittime is enabled!");
                        String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
                        String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
                        if (filesize == null || filename == null) {
                            logger.warning("filename and filesize regex of linkinformation seem to be broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        filesize = filesize.replace("Г", "G");
                        filesize = filesize.replace("М", "M");
                        filesize = filesize.replace("к", "k");
                        filesize = filesize.replaceAll("(Б|б)", "");
                        filesize = filesize + "b";
                        downloadLink.setFinalFileName(filename);
                        downloadLink.setDownloadSize(Regex.getSize(filesize));
                        return AvailableStatus.TRUE;
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (iHaveToWait) goToSleep(downloadLink);
        // Errorhandling, sometimes the link which is usually renewed by the
        // linkgrabber doesn't work and needs to be refreshed again!
        URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.getContentType().contains("html")) {
            logger.info("Renewing dllink, seems like we had a broken link here!");
            br.followConnection();
            if (br.containsHTML("\\?trycount=")) {
                logger.info("files.mail.ru page showed the \"trycount\" link. Trying to open the mainpage of the link to find a new dllink...");
                br.getPage(downloadLink.getStringProperty("folderID", null));
                // Because we are renewing the link we have to sleep here
                goToSleep(downloadLink);
            }
            String finallink = br.getRegex(finalLinkRegex).getMatch(0);
            if (finallink == null) {
                logger.warning("Critical error occured: The final downloadlink couldn't be found in handleFree!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setUrlDownload(finallink);
            con.disconnect();
        } else {
            logger.info("dllink seems to be okay (checked in handleFree)");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        if ((dl.getConnection().getContentType().contains("html"))) {
            logger.warning("The finallink doesn't seem to be a file, following connection...");
            logger.warning("finallink = " + downloadLink.getDownloadURL());
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void goToSleep(DownloadLink downloadLink) throws PluginException {
        String ttt = br.getRegex("файлы через.*?(\\d+).*?сек").getMatch(0);
        if (ttt == null) ttt = br.getRegex("download files in.*?(\\d+).*?sec").getMatch(0);
        int tt = 10;
        if (ttt != null) tt = Integer.parseInt(ttt);
        logger.info("Waiting" + tt + " seconds...");
        sleep(tt * 1001, downloadLink);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}