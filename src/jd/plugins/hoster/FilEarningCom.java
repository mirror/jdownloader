//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filearning.com" }, urls = { "http://(www\\.)?filearning\\.com/(files|get)/[A-Za-z0-9]+\\.html" }, flags = { 2 })
public class FilEarningCom extends PluginForHost {

    // DEV NOTES
    // Using FreakshareScript 1.2

    private static final String MAINPAGE = "http://www.filearning.com";

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(".com/get/", ".com/files/"));
    }

    public FilEarningCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        handleErrors();
        Regex fileInfo = br.getRegex("(?i)Download:\\&nbsp;<h1>(.+) \\(([\\d\\.]+ ?(MB|GB))\\)</h1>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because hoster tags files
        link.setFinalFileName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/get/"));
        Browser br2 = br.cloneBrowser();
        br2.getPage(br.getRegex("src=\"\\.(/[\\w/]+/core\\.js)").getMatch(0));
        String ttt = br.getRegex("var waitSecs = (\\d+);").getMatch(0);
        String fileid = br.getRegex("var file_id = '([^\\']+)\\';").getMatch(0);
        if (fileid == null) fileid = new Regex(downloadLink.getDownloadURL(), "/(.+)\\.html").getMatch(0);
        int tt = 60;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 240) {
            // 10 Minutes reconnect wait is not enough, let's wait 1 hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        String form = br2.getRegex("(<form.*</form>)").getMatch(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form = form.replace("' + base + '", "").replace("' + file_id + '", fileid).replace("' + timestamp + '", "" + System.currentTimeMillis());
        Form dlForm = new Form(form);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
            if (unknownError != null) logger.warning("Unknown error occured: " + unknownError);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void handleErrors() throws Exception {
        logger.info("Handling errors...");
        if (br.containsHTML(">The file you have requested (cannot be found|does not exist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Your download link is invalid or has expired, please try again\\.<")) throw new PluginException(LinkStatus.ERROR_RETRY, "Hoster issue?", 10 * 60 * 1000l);
        if (br.containsHTML("(>You can only download a max of \\d+ files per hour as a free user\\!<|>Los usuarios de Cuenta Gratis pueden descargar)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
    }

    private static final String PREMIUMTEXT = ">Account type: <strong>Premium Member</strong>";
    private static final Object LOCK        = new Object();

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.postPage(MAINPAGE + "/account/login", "task=dologin&return=http%3A%2F%2Fwww.filearning.com%2Fmembers%2Fmyfiles&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
            if (!br.containsHTML(PREMIUMTEXT)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        final String hostedFiles = br.getRegex(">Total Files: (\\d+)</a>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        final Regex expDate = br.getRegex(">Premium Expires:\\&nbsp;([^<>\"]*?) @ ([^<>\"]*?)</a>");
        if (expDate.getMatches().length != 1) {
            account.setValid(false);
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expDate.getMatch(0) + " " + expDate.getMatch(1), "dd-MM-yyyy hh:mm:ss", Locale.ENGLISH));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        final String getLink = link.getDownloadURL().replace(".com/files/", ".com/get/");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download&time=" + System.currentTimeMillis(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLink() {
        String getLink = br.getRegex("disabled=\"disabled\" onclick=\"document\\.location=\\'(.*?)\\';\"").getMatch(0);
        if (getLink == null) getLink = br.getRegex("(\\'|\")(" + "http://(www\\.)?([a-z0-9]+\\.)?" + MAINPAGE.replaceAll("(http://|www\\.)", "") + "/get/[A-Za-z0-9]+/\\d+/[^<>\"/]+)(\\'|\")").getMatch(1);
        return getLink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}