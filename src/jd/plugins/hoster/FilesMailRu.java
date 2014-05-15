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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.mail.ru" }, urls = { "filesmailrudecrypted://.+" }, flags = { 2 })
public class FilesMailRu extends PluginForHost {
    private static String       UA            = RandomUserAgent.generate();
    private boolean             keepCookies   = false;
    
    public static final String  DLLINKREGEX   = "<div id=\"dlinklinkOff\\d+\".*?<a href=\"(http[^<>\"]*?)\"";
    public static final String  UNAVAILABLE1  = ">В обработке<";
    public static final String  UNAVAILABLE2  = ">In process<";
    private static final String INFOREGEX     = "<td class=\"name\">(.*?<td class=\"do\">.*?)</td>";
    public static final String  LINKOFFLINE   = "(was not found|were deleted by sender|Не найдено файлов, отправленных с кодом|<b>Ошибка</b>|>Page cannot be displayed<)";
    public static final String  DLMANAGERPAGE = "class=\"download_type_choose_l\"";
    
    private static final String TYPE_VIDEO    = "http://my\\.mail\\.ru/video/top#?video=/[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+";
    private String              DLLINK        = null;
    
    public FilesMailRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.reg.mail.ru/cgi-bin/signup");
    }
    
    public void correctDownloadLink(DownloadLink link) {
        // Rename the decrypted links to make them work
        link.setUrlDownload(link.getDownloadURL().replaceAll("filesmailrudecrypted://", ""));
    }
    
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (!keepCookies) this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", UA);
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO)) throw new PluginException(LinkStatus.ERROR_FATAL, "Please re-add this link!");
        if (downloadLink.getName() == null && downloadLink.getStringProperty("folderID", null) == null) {
            logger.warning("final filename and folderID are bot null for unknown reasons!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(downloadLink.getStringProperty("folderID"));
        if (br.containsHTML(DLMANAGERPAGE)) {
            if (br.containsHTML(LINKOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filesize = br.getRegex("</div>[\t\n\r ]+</div>[\t\n\r ]+</td>[\t\n\r ]+<td title=\"(\\d+(\\.\\d+)? [^<>\"]*?)\">").getMatch(0);
            final String filename = br.getRegex("<title>([^<>\"]*?)  скачать [^<>\"]*?@Mail\\.Ru</title>").getMatch(0);
            if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            return AvailableStatus.TRUE;
        } else {
            // At the moment jd gets the russian version of the site.
            // Errorhandling
            // also works for English but filesize handling doesn't so if this
            // plugin get's broken that's on of the first things to check
            if (br.containsHTML(LINKOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // This part is to find the filename in case the downloadurl
            // redirects to the folder it comes from
            String finalfilename = downloadLink.getStringProperty("realfilename");
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
                    directlink = fixLink(directlink, br);
                    downloadLink.setUrlDownload(directlink);
                    logger.info("Set new UrlDownload, link = " + downloadLink.getDownloadURL());
                    String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
                    String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
                    if (filename == null) filename = new Regex(info, "class=\"str\">(.*?)</div>").getMatch(0);
                    if (filesize == null || filename == null) {
                        logger.warning("filename and filesize regex of linkinformation seem to be broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    filesize = fixFilesize(filesize, br);
                    downloadLink.setFinalFileName(filename);
                    downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                    return AvailableStatus.TRUE;
                }
            }
            logger.warning("File couldn't be found on the page...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }
    
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO)) requestFileInformation(downloadLink);
        doFree(downloadLink, false);
    }
    
    private void doFree(DownloadLink downloadLink, boolean premium) throws Exception, PluginException {
        keepCookies = premium;
        requestFileInformation(downloadLink);
        // Downloadmanager
        boolean dlManagerReady = false;
        if (downloadLink.getBooleanProperty("MRDWNLD", false)) {
            String id = br.getRegex("\"http://dlm\\.mail\\.ru/downloader_fmr_([0-9a-f]+)\\.exe\"").getMatch(0);
            Browser MRDWNLD = br.cloneBrowser();
            prepareBrowserForDlManager(MRDWNLD);
            MRDWNLD.getPage("/cgi-bin/files/fdownload?dlink=" + id);
            id = MRDWNLD.getRegex("<url>(http://.*?)</url>").getMatch(0);
            if (id != null) {
                downloadLink.setUrlDownload(id);
                dlManagerReady = true;
            }
        }
        DLLINK = br.getRegex(DLLINKREGEX).getMatch(0);
        if (DLLINK == null && !dlManagerReady) {
            logger.warning("Critical error occured: The final downloadlink couldn't be found in handleFree!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!(premium || dlManagerReady)) goToSleep(downloadLink);
        // Errorhandling, sometimes the link which is usually renewed by the
        // linkgrabber doesn't work and needs to be refreshed again!
        int chunks = 1;
        if (premium) chunks = 0;
        if (dlManagerReady) prepareBrowserForDlManager(br);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, chunks);
        if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads!");
        dl.startDownload();
    }
    
    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
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
    public String getAGBLink() {
        return "http://files.mail.ru/cgi-bin/files/fagreement";
    }
    
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }
    
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }
    
    public void goToSleep(DownloadLink downloadLink) throws PluginException {
        String ttt = br.getRegex("файлы через.*?(\\d+).*?сек").getMatch(0);
        if (ttt == null) ttt = br.getRegex("download files in.*?(\\d+).*?sec").getMatch(0);
        int tt = 10;
        if (ttt != null) tt = Integer.parseInt(ttt);
        logger.info("Waiting " + tt + " seconds...");
        sleep((tt + 1) * 1001, downloadLink);
    }
    
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        br.getHeaders().put("User-Agent", UA);
        login(account);
        br.setFollowRedirects(false);
        doFree(link, true);
    }
    
    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        prepareBrowser();
        br.setFollowRedirects(true);
        br.postPage("http://swa.mail.ru/cgi-bin/auth", "Page=http%3A%2F%2Ffiles.mail.ru%2F&Login=" + Encoding.urlEncode(account.getUser()) + "&Domain=mail.ru&Password=" + Encoding.urlEncode(account.getPass()));
        br.getPage("http://files.mail.ru/eng?back=%2Fsms-services");
        if (!br.containsHTML(">You have a VIP status<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
    }
    
    private void prepareBrowser() {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.18) Gecko/20110614 Firefox/3.6.18");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", "identity");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
    }
    
    private void prepareBrowserForDlManager(Browser browser) {
        browser.getHeaders().put("Pragma", null);
        browser.getHeaders().put("Cache-Control", null);
        browser.getHeaders().put("Accept-Charset", null);
        browser.getHeaders().put("Accept-Encoding", null);
        browser.getHeaders().put("Accept", "*/*");
        browser.getHeaders().put("Accept-Language", null);
        browser.getHeaders().put("User-Agent", "MRDWNLD");
        browser.getHeaders().put("Referer", null);
        browser.getHeaders().put("Content-Type", null);
    }
    
    public String fixFilesize(String filesize, final Browser br) {
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        return filesize;
    }
    
    public String fixLink(String dllink, final Browser br) {
        logger.info("Correcting link...");
        String replaceThis = new Regex(dllink, "http://(content\\d+-n)\\.files\\.mail\\.ru.*?").getMatch(0);
        if (replaceThis != null) dllink = dllink.replace(replaceThis, replaceThis.replace("-n", ""));
        return dllink;
    }
    
    /* Avoid multi-hosters from downloading links from this host because links come from a decrypter */
    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }
    
    @Override
    public void reset() {
    }
    
    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
    
}
