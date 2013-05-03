//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bytesbox.com" }, urls = { "https?://(www\\.)?bytesbox\\.com/\\!/[A-Za-z0-9]+" }, flags = { 2 })
public class BytesBoxCom extends PluginForHost {

    private static AtomicBoolean isWaittimeDetected = new AtomicBoolean(false);

    public BytesBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.bytesbox.com/professional.php");
    }

    @Override
    public String getAGBLink() {
        return "http://bytesbox.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>BytesBox\\.com \\- Download ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("Download: ([^<>\"]*?)</div>").getMatch(0);
        if (filename != null) link.setName(Encoding.htmlDecode(filename.trim()));
        String filesize = br.getRegex(">Download<span style=\" font\\-weight:normal;\"><br>([^<>\"]*?)</span></a>").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        }
        isWaittimeDetected.set(false);
        if (br.containsHTML("class=\"inactiveButton\">Wait<span")) isWaittimeDetected.set(true);
        if (isWaittimeDetected.get() == true) return AvailableStatus.TRUE;
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("File too big for free users </span>")) {
            logger.warning("Not possible to download in free mode");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Not possible to download in free mode");
        }
        /* waittime */
        if (br.containsHTML("class=\"inactiveButton\">Wait<span")) {
            isWaittimeDetected.set(true);
            long wait = 40 * 60 * 1000l;
            String waittime = br.getRegex("(\\d+:\\d+) Mins").getMatch(0);
            if (waittime != null) wait = (Integer.parseInt(waittime.split(":")[0]) * 60 + Integer.parseInt(waittime.split(":")[1])) * 1000l;
            if (wait > 30000) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
            } else {
                sleep(wait, downloadLink);
            }
        }
        /* captcha */
        final String downsess = br.getRegex("var downsess = \"([a-z0-9]+)\";").getMatch(0);
        if (downsess == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 0; i <= 3; i++) {
            final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
            jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
            final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
            final String code = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage("/ajax.solvcaptcha.php", "downsess=" + downsess + "&adcopy_challenge=" + sm.getChallenge(code) + "&adcopy_response=" + Encoding.urlEncode(code));
            if (ajaxBR.containsHTML("\"status\":\"ERROR\"")) continue;
            break;
        }
        if (ajaxBR.containsHTML("\"status\":\"ERROR\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.postPage("/getdownlink.php", "down_sess=" + downsess + "&file=" + getFileId(downloadLink));
        String dllink = br.getRegex("link\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFileId(DownloadLink downloadLink) {
        String dllink = downloadLink.getDownloadURL();
        return new Regex(dllink, "\\!/([A-Za-z0-9]+)/?$").getMatch(0);
    }

    private static final String MAINPAGE = "http://bytesbox.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
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
                            this.br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("https://www.bytesbox.com/login.php", "remember=on&loginAct=Login+to+your+account&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(this.getHost(), "bytesbox_pass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("/settings.php");
        if (!br.containsHTML("<p>Plan Type: <span>Professional Storage</span>")) {
            account.setValid(false);
            ai.setStatus("Unsupported accounttype");
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String usedSpace = br.getRegex("<span>(\\d+)</span>MB<span>/\\d+</span>GB").getMatch(0);
        if (usedSpace != null) ai.setUsedSpace(SizeFormatter.getSize(usedSpace + " MB"));
        final String expire = br.getRegex("<p>Next Due Date: <span>([^<>\"]*?)</span>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MMMM-dd hh:mm:ss", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
        return 1;
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