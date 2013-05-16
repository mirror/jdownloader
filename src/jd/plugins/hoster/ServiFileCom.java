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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "servifile.com" }, urls = { "http://(www\\.)?servifile\\.com/files/[^/]+" }, flags = { 2 })
public class ServiFileCom extends PluginForHost {

    public ServiFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    private static final String MAINPAGE      = "http://www.servifile.com";
    private static final String GETLINKREGEX  = "disabled=\"disabled\" onclick=\"document\\.location=\\'(.*?)\\';\"";
    private static final String GETLINKREGEX2 = "\\'(" + "http://(www\\.)" + MAINPAGE.replaceAll("(http://|www\\.)", "") + "/get/[A-Za-z0-9]+/\\d+/.*?)\\'";
    private static final String PREMIUMLIMIT  = "out of 200\\.00 GB</td>";
    private static final String PREMIUMTEXT   = ">Account type:</td>[\t\n\r ]+<td><b>Premium</b>";
    private static Object       LOCK          = new Object();

    // Using FreakshareScript 1.2
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The file you have requested does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex dlInfo = br.getRegex("<h1 style=\"text\\-align:left;\">([^<>\"]*?) \\((\\d+(\\.\\d+)? [A-Za-z]{1,5}), \\d+ descargas\\)</h1>");
        final String filename = dlInfo.getMatch(0);
        final String filesize = dlInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        // final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        // final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        // final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        // if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // rc.setId(id);
        // rc.load();
        // final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        // final String c = getCaptchaCode(cf, downloadLink);
        // br.postPage(br.getURL(), "entrar=no&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" +
        // Encoding.urlEncode(c));
        // if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML(">The file you have requested does not exists")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String getLink = br.getRegex(GETLINKREGEX).getMatch(0);
        if (getLink == null) getLink = br.getRegex(GETLINKREGEX2).getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 60;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 240) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, "task=download", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(files per hour for free users\\.</div>|>Los usuarios de Cuenta Gratis pueden descargar)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
            if (unknownError != null) logger.warning("Unknown error occured: " + unknownError);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(downloadLink);
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink dl) {
        // Remove unwanted filename tag
        dl.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(this.dl.getConnection())).trim().replace("[Servifile.com]", ""));
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                br.postPage(MAINPAGE + "/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login&task=dologin&return=%2F");
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
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
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
        final String hostedFiles = br.getRegex("<td>Files Hosted:</td>[\t\r\n ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        final String space = br.getRegex("<td>Spaced Used:</td>[\t\n\r ]+<td>(.*?) " + PREMIUMLIMIT).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        final String expire = br.getRegex(">Premium end date:</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        account.setValid(true);
        if (expire != null) ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd-MM-yyyy", Locale.ENGLISH));
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        String getLink = br.getRegex(GETLINKREGEX).getMatch(0);
        if (getLink == null) getLink = br.getRegex(GETLINKREGEX2).getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(getLink);
        // No multiple connections possible for premiumusers
        Form form = new Form();
        form.setMethod(MethodType.POST);
        form.setAction(getLink);
        form.put("task", "download");
        br.setFollowRedirects(true);
        boolean resume = true;
        int maxChunks = -2;
        if (oldStyle()) {
            /*
             * in old stable we have a bug with post data and range request headers
             */
            resume = false;
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(link);
        dl.startDownload();
    }

    /* old simpleftp does not have size command support */
    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        return true;
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
        /**
         * 4 possible but errorhandling is good, we can just set it to unlimited
         */
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}