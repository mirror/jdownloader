//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zettaupload.com" }, urls = { "http://(www\\.)?zettaupload\\.com/(?!file_category|latest_file|wklej|terms|report_file|partners|contact|faq|login|register)[a-z0-9]+" }, flags = { 2 })
public class ZettaUploadCom extends PluginForHost {

    public ZettaUploadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    // DTemplate Version 0.1.7-psp
    // mods:
    // non account: 1 * 20
    // premium account: chunks * maxdls
    // protocol: no https
    // captchatype: recaptcha

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms." + TYPE;
    }

    private final String  MAINPAGE  = "http://zettaupload.com";
    private final String  TYPE      = "php";
    private final boolean RESUME    = false;
    private final int     MAXCHUNKS = 1;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(this.getHost() + "decrypted", this.getHost()));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/error." + TYPE) || br.getURL().contains("/index." + TYPE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("<th class=\"descr\"([^<>]*?)?>[\t\n\r ]+<strong>([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)<br/>");
        final String filename = fInfo.getMatch(1);
        final String filesize = fInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        boolean captcha = false;
        requestFileInformation(downloadLink);
        final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (waittime != null) sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
        final String getLink = br.getRegex("\"(http://zettaupload\\.com/[A-Za-z0-9]+\\?download\\&t=[a-z0-9]+)\"").getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, RESUME, MAXCHUNKS);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(http://[^<>\"]*?)\"").getMatch(0);
            final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null || captchaAction == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaAction, "submit=kontynuuj&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, RESUME, MAXCHUNKS);
                if (!dl.getConnection().isContentDisposition()) {
                    br.followConnection();
                    rc.reload();
                    continue;
                }
                break;
            }
            captcha = true;
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("User-Agent", RandomUserAgent.generate());
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://zettaupload.com/login.html", "submit=Logowanie&submitme=1&loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "spf") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www." + this.getHost() + "/upgrade.html");
        if (!br.containsHTML("Typ Konta:[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+Free User[\t\n\r ]+</td>")) {
            account.setValid(false);
            ai.setStatus("Unknown accounttype");
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        final String dllink = br.getRegex("\"(http://zettaupload\\.com/[A-Za-z0-9]+/download\\.html\\?t=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
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