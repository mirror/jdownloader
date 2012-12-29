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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unibytes.com" }, urls = { "http://(www\\.)?unibytes\\.com/[a-zA-Z0-9\\-\\.\\_ ]+" }, flags = { 2 })
public class UniBytesCom extends PluginForHost {

    // DEV NOTES
    // other: they blocked our default User Agent.

    private static final String CAPTCHATEXT      = "captcha\\.jpg";
    private static final String FATALSERVERERROR = "<u>The requested resource \\(\\) is not available\\.</u>";
    private static final String MAINPAGE         = "http://www.unibytes.com/";
    private static String       agent            = null;
    private static final String freeDlLink       = "(https?://st\\d+\\.unibytes\\.com/fdload/file[^\"]+)";

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
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<p>File not found or removed</p>|>\\s+Page Not Found\\s+<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(FATALSERVERERROR)) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("id=\"fileName\" style=\"[^\"\\']+\">(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("\\((\\d+\\.\\d+ [A-Za-z]+)\\)</h3><script>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("</span>[\t\n\r ]+\\((.*?)\\)</h3><script>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("\\(([\\d\\.]+ ?(KB|MB|GB))\\)").getMatch(0);
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
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
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
        String uid = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/([a-zA-Z0-9\\-\\.\\_/]+)").getMatch(0);
        requestFileInformation(downloadLink);
        if (br.containsHTML(FATALSERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fatal server error");
        final String addedLinkCoded = Encoding.urlEncode(downloadLink.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null || !dllink.contains("fdload/")) {
            dllink = dllink == null ? br.getRegex("<div id=\"exeLink\"><a href=\"(http:[^\"]+)").getMatch(0) : dllink;
            dllink = dllink == null ? br.getRegex(freeDlLink).getMatch(0) : dllink;
            if (dllink == null) {
                if (br.containsHTML("(showNotUniqueIP\\(\\);|>Somebody else is already downloading using your IP-address<)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
                int iwait = 60;
                String regexedTime = br.getRegex("id=\"slowRest\">(\\d+)</").getMatch(0);
                if (regexedTime == null) regexedTime = br.getRegex("var timerRest = (\\d+);").getMatch(0);
                if (regexedTime != null) iwait = Integer.parseInt(regexedTime);
                String ipBlockedTime = br.getRegex("guestDownloadDelayValue\">(\\d+)</span>").getMatch(0);
                if (ipBlockedTime == null) ipBlockedTime = br.getRegex("guestDownloadDelay\\((\\d+)\\);").getMatch(0);
                if (ipBlockedTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(ipBlockedTime) * 60 * 1001l);
                String step = br.getRegex("/free\\?step=([^\"&]+)").getMatch(0);
                if (step == null) {
                    logger.warning("Couldn't find 'step'");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                sleep(iwait * 1001l, downloadLink);
                br.getPage(downloadLink.getDownloadURL() + "/free?&step=" + step + "&referer=");
                if (br.getRedirectLocation() != null && br.getRedirectLocation().matches(freeDlLink)) {
                    dllink = br.getRedirectLocation();
                }
                if (dllink == null) {
                    dllink = br.getRegex(freeDlLink).getMatch(0);
                }
                if ((dllink == null) && br.containsHTML("(id=\"noThanxDiv\".+\">no, thanks</a></div>|<a href=(\"|\\')(/" + uid + "/[^\"]+step=[^\"]+))")) {
                    // for times with addition page of no thanks && steps
                    String step2 = br.getRegex("<div style=\".+\" id=\"noThanxDiv\"><a href=\"([^\"]+)\".+\">no, thanks</a></div>").getMatch(0);
                    if (step2 == null) {
                        step2 = br.getRegex("<a href=\"([^\"]+)\"[^>]+\">no, thanks</a>").getMatch(0);
                        if (step2 == null) step2 = br.getRegex("<a href=(\"|\\')(/" + uid + "/[^\"]+step=[^\"]+)").getMatch(1);
                        if (step2 != null) {
                            br.getPage(step2);
                            if (br.getRedirectLocation() != null && br.getRedirectLocation().matches(freeDlLink)) {
                                dllink = br.getRedirectLocation();
                            } else {
                                dllink = br.getRegex(freeDlLink).getMatch(0);
                            }
                        }
                    }
                }
                String step3 = br.getRegex("name=\"s\" value=\"(.*?)\"").getMatch(0);
                if (dllink == null && step3 == null) {
                    logger.warning("'step3' equals null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // not sure if this is captcha component required or even current...
                if (dllink == null) {
                    br.postPage(downloadLink.getDownloadURL(), "step=captcha&s=" + step3 + "&referer=" + addedLinkCoded);
                    if (br.containsHTML(CAPTCHATEXT)) {
                        logger.info("Captcha found");
                        for (int i = 0; i <= 5; i++) {
                            String code = getCaptchaCode("http://www.unibytes.com/captcha.jpg", downloadLink);
                            String post = "s=" + step3 + "&referer=" + addedLinkCoded + "&step=last&captcha=" + code;
                            br.postPage(downloadLink.getDownloadURL(), post);
                            if (!br.containsHTML(CAPTCHATEXT)) break;
                        }
                        if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        logger.info("Captcha not found");
                    }
                    dllink = br.getRegex(freeDlLink).getMatch(0);
                    if (dllink == null) dllink = br.getRegex("style=\"width: 650px; margin: 40px auto; text-align: center; font-size: 2em;\"><a href=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            logger.warning("dllink equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        Account aa = AccountController.getInstance().getValidAccount(this);
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
        String dllink = br.getRegex("style=\"text-align:center; padding:50px 0;\"><a href=\"(http.*?)\"").getMatch(0);
        dllink = null;
        if (dllink == null) dllink = br.getRegex("\"(http://st\\d+\\.unibytes\\.com/download/file.*?\\?referer=.*?)\"").getMatch(0);
        if (dllink == null) {
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
        br.postPage(MAINPAGE, "lb_login=" + Encoding.urlEncode(account.getUser()) + "&lb_password=" + Encoding.urlEncode(account.getPass()) + "&lb_remember=true");
        if (br.getCookie(MAINPAGE, "hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}