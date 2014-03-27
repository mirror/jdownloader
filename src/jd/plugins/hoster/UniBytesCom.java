//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.AccountController;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unibytes.com" }, urls = { "http://(www\\.)?unibytes\\.com/([a-zA-Z0-9\\-\\.\\_]{11}B|[a-zA-Z0-9\\-\\.\\_]{11}Lqw-Us4P3UgBB)" }, flags = { 2 })
public class UniBytesCom extends PluginForHost {

    // DEV NOTES
    // UID are case sensitive.
    // [a-zA-Z0-9\\-\\.\\_]{11}Lqw-Us4P3UgBB = recently uploaded links + folder links?
    // [a-zA-Z0-9\\-\\.\\_]{11}B = short link??
    // it doesn't seem possible exchanging 11char uid between the different url structures.

    // other: they blocked our default User Agent.

    private final String CAPTCHATEXT      = "captcha\\.jpg";
    private final String FATALSERVERERROR = "<u>The requested resource \\(\\) is not available\\.</u>";
    private final String MAINPAGE         = "http://www.unibytes.com/";
    private String       agent            = null;
    private final String freeDlLink       = "(https?://st\\d+\\.unibytes\\.com/fdload/file[^\"]+)";
    private final String SECURITYCAPTCHA  = "text from the image and click \"Continue\" to access the website";

    public UniBytesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.unibytes.com/vippay");
    }

    public void prepBrowser() {
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.setCookie(MAINPAGE, "lang", "en");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<p>File not found or removed</p>|>\\s+Page Not Found\\s+<|File not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(FATALSERVERERROR)) return AvailableStatus.UNCHECKABLE;
        if (br.containsHTML(SECURITYCAPTCHA)) {
            link.getLinkStatus().setStatusText("Can't check status, security captcha...");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("Download file:</small><br/>([^<>\"]*?)<small>").getMatch(0);
        String filesize = br.getRegex("\\((\\d+\\.\\d+ [A-Za-z]+)\\)</h3><script>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("</span>[\t\n\r ]+\\((.*?)\\)</h3><script>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("\\(([\\d\\.,]+ ?(KB|MB|GB|bytes))\\)").getMatch(0);
            }
        }
        if (filename == null) {
            // Leave this in
            logger.warning("Fatal error happened in the availableCheck...");
            logger.warning("Filename = " + filename);
            logger.warning("Filesize = " + filesize);
            logger.warning(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set final name here because server sometimes sends bad filenames
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            filesize = filesize.replace(",", "").replace("bytes", "b");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
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
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.unibytes.com/page/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String offer = br.getRegex("<a href=\"/([^\"]+&referer=)").getMatch(0); // Russian only
        if (offer != null) br.postPage(MAINPAGE + offer, "");
        if (br.containsHTML(SECURITYCAPTCHA)) {
            final Form captchaForm = br.getForm(0);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final String code = getCaptchaCode("http://www." + this.getHost() + "/captcha/?rnd=", downloadLink);
            captchaForm.put("captcha", code);
            br.submitForm(captchaForm);
            if (br.containsHTML(SECURITYCAPTCHA)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String uid = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/([a-zA-Z0-9\\-\\.\\_]+)").getMatch(0);
        if (br.containsHTML(FATALSERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fatal server error");
        String dllink = br.getRedirectLocation();
        if (dllink == null || !dllink.contains("fdload/")) {
            dllink = dllink == null ? br.getRegex("<div id=\"exeLink\"><a href=\"(http:[^\"]+)").getMatch(0) : dllink;
            dllink = dllink == null ? br.getRegex(freeDlLink).getMatch(0) : dllink;
            if (dllink != null) {
                /* Waittime is skippable but maybe forced for russians */
                int wait = 60;
                final String waittime = br.getRegex("var nn = (\\d+);").getMatch(0);
                if (waittime != null) wait = Integer.parseInt(waittime);
                this.sleep(wait * 1001l, downloadLink);
            } else {
                // maybe outdated
                if (br.containsHTML("(showNotUniqueIP\\(\\);|>Somebody else is already downloading using your IP-address<)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
                String ipBlockedTime = br.getRegex("guestDownloadDelayValue\">(\\d+)</span>").getMatch(0);
                if (ipBlockedTime == null) ipBlockedTime = br.getRegex("guestDownloadDelay\\((\\d+)\\);").getMatch(0);
                if (ipBlockedTime == null) ipBlockedTime = br.getRegex("Wait for\\s+(\\d+)\\s+min").getMatch(0);
                if (ipBlockedTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(ipBlockedTime) * 60 * 1001l);
                // step1
                String stepForward = br.getRegex("\"(/" + uid + "/free\\?step=[^\"]+)").getMatch(0);
                if (stepForward == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.getPage(stepForward);
                // links can be found after the first step in some regions!
                dllink = br.getRegex(freeDlLink).getMatch(0);
                if (dllink == null) {
                    // step2 - euroland require!
                    stepForward = br.getRegex("\"(/" + uid + "/link\\?step=[^\"]+)").getMatch(0);
                    if (stepForward == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    br.getPage(stepForward);
                    if (br.containsHTML("\">Somebody else is already downloading using")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 2 * 60 * 1001l);
                    if (ipBlockedTime == null) ipBlockedTime = br.getRegex("Wait for\\s+(\\d+)\\s+min").getMatch(0);
                    if (ipBlockedTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(ipBlockedTime) * 60 * 1001l);
                    dllink = br.getRegex(freeDlLink).getMatch(0);
                    if (dllink == null) {
                        logger.warning("dllink equals null!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(FATALSERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fatal server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("style=\"text\\-align:center; clear: both; padding\\-top: 3em;\"><a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://st\\d+\\.unibytes\\.com/download/file.*?\\?referer=.*?)\"").getMatch(0);
        if (dllink == null) {
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa != null) {
                AccountInfo ai = new AccountInfo();
                String expireDate = br.getRegex("(Ваш VIP-аккаунт действителен до|Your VIP account valid till) ([0-9\\.]+)\\.<").getMatch(1);
                if (expireDate != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "dd.MM.yyyy", null));
                } else {
                    ai.setExpired(true);
                }
                account.setAccountInfo(ai);
                if (ai.isExpired()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        prepBrowser();
        br.getPage(MAINPAGE);
        br.postPage(MAINPAGE, "lb_login=" + Encoding.urlEncode(account.getUser()) + "&lb_password=" + Encoding.urlEncode(account.getPass()) + "&lb_remember=true");
        if (br.getCookie(MAINPAGE, "hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
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