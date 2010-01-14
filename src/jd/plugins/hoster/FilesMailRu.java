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

    public String folderID = this.getPluginConfig().getStringProperty("folderID", null);

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getFinalFileName() == null && this.getPluginConfig().getStringProperty("folderID", null) == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.getPluginConfig().setProperty("finalName", downloadLink.getFinalFileName());
        this.getPluginConfig().save();
        String folderIDregexed = new Regex(downloadLink.getDownloadURL(), "mail\\.ru/([A-Z0-9]+)").getMatch(0);
        this.getPluginConfig().setProperty("folderID", "http://files.mail.ru/" + folderIDregexed);
        this.getPluginConfig().save();
        // At the moment jd gets the russian version of the site. Errorhandling
        // also works for English but filesize handling doesn't so if this
        // plugin get's broken that's on of the first things to check
        try {
            if (!br.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
                long size = br.getHttpConnection().getLongContentLength();
                downloadLink.setDownloadSize(Long.valueOf(size));
                return AvailableStatus.TRUE;
            } else {
                logger.info("folderID does exist, accessing page " + folderID + " to renew the ticket");
                br.getPage(folderID);
                if (br.containsHTML("(was not found|were deleted by sender|Не найдено файлов, отправленных с кодом|<b>Ошибка</b>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                // This part is to find the filename in case the downloadurl
                // redirects to the folder it comes from
                String finalfilename = this.getPluginConfig().getStringProperty("finalName", null);
                if (finalfilename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String[] linkinformation = br.getRegex("<td class=\"name\">(.*?)<td class=\"do\">").getColumn(0);
                if (linkinformation.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                for (String info : linkinformation) {
                    if (info.contains(finalfilename)) {
                        String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
                        String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
                        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        filesize = filesize.replace("Г", "G");
                        filesize = filesize.replace("М", "M");
                        filesize = filesize.replace("к", "k");
                        filesize = filesize.replace("Б", "");
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
        br.setFollowRedirects(true);
        String dllink = null;
        if (br.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
            logger.info("folderID does exist, accessing page " + folderID + " to renew the ticket");
            br.getPage(folderID);
            // Find the downloadlink if the actual one redirects to the previous
            // downloadpage (folder)
            String finalfilename = this.getPluginConfig().getStringProperty("finalName", null);
            if (finalfilename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String[] linkinformation = br.getRegex("<td class=\"name\">(.*?)<td class=\"do\">").getColumn(0);
            if (linkinformation.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (String info : linkinformation) {
                if (info.contains(finalfilename)) {
                    String directlink = new Regex(info, "\"(http://.*?\\.files\\.mail\\.ru/.*?/.*?)\"").getMatch(0);
                    if (directlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    dllink = directlink;
                    // Set the new downloadlink so when the user stops the
                    // download and tries again he doesn't have to wait the 10
                    // Seconds to start!
                    downloadLink.setUrlDownload(dllink);
                    // Ticket Time
                    // If we get the dllink this way we have to wait about 10
                    // seconds else the server doesn't allow starting the
                    // download
                    String ttt = br.getRegex("файлы через.*?(\\d+).*?сек").getMatch(0);
                    int tt = 10;
                    if (ttt == null) ttt = br.getRegex("download files in.*?(\\d+).*?sec").getMatch(0);
                    if (ttt != null) tt = Integer.parseInt(ttt);
                    sleep(tt * 1001, downloadLink);
                }
            }
        } else {
            dllink = downloadLink.getDownloadURL();
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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