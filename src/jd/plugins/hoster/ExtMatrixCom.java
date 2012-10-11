//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extmatrix.com" }, urls = { "http://(www\\.)?extmatrix\\.com/files/[A-Za-z0-9]+/[^<>\"/]+\\.html" }, flags = { 2 })
public class ExtMatrixCom extends PluginForHost {

    public ExtMatrixCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    private static final String MAINPAGE            = "http://www.extmatrix.com";
    private static final String PREMIUMONLYTEXT     = "(>Premium Only\\!|you have requested require a premium account for download\\.<)";
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.extmatrixcom.errors.premiumonly", "Only downloadable via premium account");

    // private static final String APIKEY = "4LYl9hFH1Xapzycg4fuFtUSPVvWsr";

    // Using FlexShareScript 1.2.1, heavily modified
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The file you have requested does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("style=\"text\\-align:(center|left|right);\">(Premium Only\\!)?([^\"<>]+) \\(([0-9\\.]+ [A-Za-z]+)(\\))?(,[^<>\"/]+)?</h1>");
        String filename = fileInfo.getMatch(2);
        String filesize = fileInfo.getMatch(3);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML(PREMIUMONLYTEXT)) link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
        // Set final filename here because hoster taggs files
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(PREMIUMONLYTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        String getLink = getLink();
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 20;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 240) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        // Short waittime can be skipped
        // sleep(tt * 1001l, downloadLink);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(getLink);
            if (con.getContentType().contains("html")) {
                br.followConnection();
                final String action = br.getRegex("\"(http://s\\d+\\.extmatrix\\.com/get/[A-Za-z0-9]+/\\d+/[^<>\"]*?)\"").getMatch(0);
                final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
                if (rcID == null || action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(rcID);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, action, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&task=download", true, 1);
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, true, 1);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Server is too busy for free users")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots", 10 * 60 * 1000l);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("(files per hour for free users\\.</div>|>Los usuarios de Cuenta Gratis pueden descargar|hours for free users\\.|var time =)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
            if (unknownError != null) logger.warning("Unknown error occured: " + unknownError);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static final String PREMIUMLIMIT = "out of 1024\\.00 TB</td>";
    private static final String PREMIUMTEXT  = "Account type:</td>[\n ]+<td><b>Premium</b>";
    private static Object       LOCK         = new Object();

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
            br.postPage(MAINPAGE + "/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&captcha=&submit=Login&task=dologin&return=.%2Fmembers%2Fmyfiles.php");
            if (!br.containsHTML(PREMIUMTEXT)) {
                br.getPage(MAINPAGE + "/members/myfiles.php");
                if (!br.containsHTML(PREMIUMLIMIT)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
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
        String hostedFiles = br.getRegex("<td>Files Hosted:</td>[\t\r\n ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        String space = br.getRegex("<td>Spaced Used:</td>[\t\n\r ]+<td>(.*?) " + PREMIUMLIMIT).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
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
        br.getPage(link.getDownloadURL());
        // Little help from their admin^^
        String getLink = br.getRegex("<a id=\\'jd_support\\' href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (getLink == null) getLink = getLink();
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int maxChunks = -2;
        if (oldStyle()) {
            /*
             * stable has bug with openDownload and postData, cookies are not
             * forwared to chunks
             */
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download", true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

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