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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.mail.ru" }, urls = { "http://[\\w\\.]*?wge4zu4rjfsdehehztiuxw/[A-Z0-9]{6}(/[a-z0-9]+)?" }, flags = { 2 })
public class FilesMailRu extends PluginForHost {
    private static final String UA          = RandomUserAgent.generate();
    private boolean             keepCookies = false;

    public FilesMailRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.reg.mail.ru/cgi-bin/signup");
    }

    @Override
    public String getAGBLink() {
        return "http://files.mail.ru/cgi-bin/files/fagreement";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Rename the decrypted links to make them work
        link.setUrlDownload(link.getDownloadURL().replaceAll("wge4zu4rjfsdehehztiuxw", "files.mail.ru"));
    }

    private static final String DLLINKREGEX  = "\"(http://[a-z0-9-]+\\.files\\.mail\\.ru/.*?/.*?)\"";
    private static final String UNAVAILABLE1 = ">В обработке<";
    private static final String UNAVAILABLE2 = ">In process<";
    private static final String INFOREGEX    = "<td class=\"name\">(.*?<td class=\"do\">.*?)</td>";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        if (!keepCookies) this.setBrowserExclusive();

        br.getHeaders().put("User-Agent", UA);
        br.setFollowRedirects(true);
        if (downloadLink.getName() == null && downloadLink.getStringProperty("folderID", null) == null) {
            logger.warning("final filename and folderID are bot null for unknown reasons!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (downloadLink.getName() != null) downloadLink.setProperty("finalFilename", downloadLink.getName());
        String folderIDregexed = new Regex(downloadLink.getDownloadURL(), "mail\\.ru/([A-Z0-9]+)").getMatch(0);
        downloadLink.setProperty("folderID", "http://files.mail.ru/" + folderIDregexed);
        br.getPage(downloadLink.getStringProperty("folderID", null));
        // At the moment jd gets the russian version of the site. Errorhandling
        // also works for English but filesize handling doesn't so if this
        // plugin get's broken that's on of the first things to check
        if (br.containsHTML("(was not found|were deleted by sender|Не найдено файлов, отправленных с кодом|<b>Ошибка</b>)")) {
            logger.info("Link is 100% offline, found errormessage on the page!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // This part is to find the filename in case the downloadurl
        // redirects to the folder it comes from
        String finalfilename = downloadLink.getStringProperty("finalFilename", null);
        if (finalfilename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] linkinformation = br.getRegex(INFOREGEX).getColumn(0);
        if (linkinformation == null || linkinformation.length == 0) {
            logger.warning("Critical error : Couldn't get the linkinformation");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String info : linkinformation) {
            if (info.contains(finalfilename)) {
                // regex the new downloadlink and save it!
                String directlink = new Regex(info, DLLINKREGEX).getMatch(0);
                if ((info.contains(UNAVAILABLE1) || info.contains(UNAVAILABLE2)) && directlink == null) {
                    logger.info("File " + downloadLink.getStringProperty("folderID", null) + " is still uploading (temporary unavailable)!");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.FilesMailRu.InProcess", "Datei steht noch im Upload"));
                }
                if (directlink == null) {
                    logger.warning("Critical error occured: The final downloadlink couldn't be found in the available check!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                directlink = fixLink(directlink);
                downloadLink.setUrlDownload(directlink);
                logger.info("Set new UrlDownload, link = " + downloadLink.getDownloadURL());
                String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
                String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
                if (filename == null) filename = new Regex(info, "class=\"str\">(.*?)</div>").getMatch(0);
                if (filesize == null || filename == null) {
                    logger.warning("filename and filesize regex of linkinformation seem to be broken!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filesize = filesize.replace("Г", "G");
                filesize = filesize.replace("М", "M");
                filesize = filesize.replaceAll("(к|К)", "k");
                filesize = filesize.replaceAll("(Б|б)", "");
                filesize = filesize + "b";
                downloadLink.setFinalFileName(filename);
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                return AvailableStatus.TRUE;
            }
        }
        logger.warning("File couldn't be found on the page...");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        doFree(downloadLink, false);
    }

    private void doFree(DownloadLink downloadLink, boolean premium) throws Exception, PluginException {
        String finallink = null;
        keepCookies = premium;
        requestFileInformation(downloadLink);
        finallink = br.getRegex(DLLINKREGEX).getMatch(0);
        if (finallink == null) {
            logger.warning("Critical error occured: The final downloadlink couldn't be found in handleFree!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!premium) goToSleep(downloadLink);
        // Errorhandling, sometimes the link which is usually renewed by the
        // linkgrabber doesn't work and needs to be refreshed again!
        int chunks = 1;
        if (premium) chunks = 0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, chunks);
        if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads!"); }
        dl.startDownload();
    }

    public void goToSleep(DownloadLink downloadLink) throws PluginException {
        String ttt = br.getRegex("файлы через.*?(\\d+).*?сек").getMatch(0);
        if (ttt == null) ttt = br.getRegex("download files in.*?(\\d+).*?sec").getMatch(0);
        int tt = 10;
        if (ttt != null) tt = Integer.parseInt(ttt);
        logger.info("Waiting " + tt + " seconds...");
        sleep((tt + 1) * 1001, downloadLink);
    }

    private String fixLink(String dllink) {
        logger.info("Correcting link...");
        String replaceThis = new Regex(dllink, "http://(content\\d+-n)\\.files\\.mail\\.ru.*?").getMatch(0);
        if (replaceThis != null) dllink = dllink.replace(replaceThis, replaceThis.replace("-n", ""));
        return dllink;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://swa.mail.ru/cgi-bin/auth", "Page=http%3A%2F%2Ffiles.mail.ru%2F&Login=" + Encoding.urlEncode(account.getUser()) + "&Domain=mail.ru&Password=" + Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(true);
        br.getPage("http://files.mail.ru/eng?back=%2Fsms-services");
        if (!br.containsHTML(">You have a VIP status<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<b>Your VIP status is valid until (.*?)</b><br><br>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "MMMM dd, yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        br.getHeaders().put("User-Agent", UA);
        login(account);
        br.setFollowRedirects(false);
        doFree(link, true);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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